package name.hergeth.jchat.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import name.hergeth.jchat.openai.dto.ChatMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Optional;

public class ScenarioRunner {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final JChatHttpClient client;
    private final Path scenariosDir;
    private final Path outputDir;
    private final boolean validate;
    private final boolean semanticValidate;
    private final SemanticTripleValidator semanticValidator;

    public ScenarioRunner(
            JChatHttpClient client,
            Path scenariosDir,
            Path outputDir,
            boolean validate,
            boolean semanticValidate) {
        this.client = client;
        this.scenariosDir = scenariosDir;
        this.outputDir = outputDir;
        this.validate = validate;
        this.semanticValidate = semanticValidate;
        this.semanticValidator = semanticValidate ? new SemanticTripleValidator(client) : null;
    }

    public List<ScenarioRunResult> runAll() throws IOException, InterruptedException {
        return runAll(Optional.empty());
    }

    public List<ScenarioRunResult> runAll(Optional<String> onlyScenario) throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        List<ScenarioDefinition> scenarios = onlyScenario.isPresent()
                ? List.of(ScenarioLoader.loadByName(scenariosDir, onlyScenario.get()))
                : ScenarioLoader.loadAll(scenariosDir);
        List<ScenarioRunResult> results = new ArrayList<>();
        for (ScenarioDefinition scenario : scenarios) {
            System.out.printf("Running scenario %s (%s)...%n", scenario.name(), scenario.conversationId());
            results.add(run(scenario));
        }
        Path summaryFile = outputDir.resolve("summary.json");
        MAPPER.writeValue(summaryFile.toFile(), results);
        return results;
    }

    public ScenarioRunResult run(ScenarioDefinition scenario) throws IOException, InterruptedException {
        List<ChatMessage> history = new ArrayList<>();
        List<ScenarioTurnResult> turnResults = new ArrayList<>();

        for (int i = 0; i < scenario.turns().size(); i++) {
            String userMessage = scenario.turns().get(i);
            history.add(new ChatMessage("user", userMessage));

            System.out.printf("  turn %d/%d: %s%n", i + 1, scenario.turns().size(), truncate(userMessage, 80));
            String assistantResponse = client.chat(
                    scenario.conversationId(), List.copyOf(history));
            history.add(new ChatMessage("assistant", assistantResponse));

            client.waitForBackgroundLlmTasks(Duration.ofMinutes(3));

            List<StatementSnapshot> store = client.knowledgeStore(scenario.conversationId());
            turnResults.add(new ScenarioTurnResult(i, userMessage, assistantResponse, store));
        }

        List<StatementSnapshot> finalStore = client.knowledgeStore(scenario.conversationId());
        ScenarioValidationResult validation = validate
                ? validate(scenario, finalStore)
                : new ScenarioValidationResult(true, List.of());

        ScenarioRunResult result = new ScenarioRunResult(
                scenario.name(),
                scenario.conversationId(),
                Instant.now(),
                turnResults,
                finalStore,
                validation);

        Path resultFile = outputDir.resolve(scenario.name() + ".result.json");
        MAPPER.writeValue(resultFile.toFile(), result);

        Path storeFile = outputDir.resolve(scenario.name() + ".store.json");
        MAPPER.writeValue(storeFile.toFile(), finalStore);

        return result;
    }

    private ScenarioValidationResult validate(ScenarioDefinition scenario, List<StatementSnapshot> store)
            throws IOException, InterruptedException {
        Path scenarioFile = scenariosDir.resolve(scenario.name() + ".yaml");
        Optional<ScenarioExpected> expectedOpt = ScenarioLoader.loadExpected(scenarioFile);
        if (expectedOpt.isEmpty()) {
            return new ScenarioValidationResult(true, List.of());
        }

        ScenarioExpected expected = expectedOpt.get();
        List<String> failures = new ArrayList<>();

        if (expected.minStatements() != null && store.size() < expected.minStatements()) {
            failures.add("Expected at least " + expected.minStatements() + " statements, got " + store.size());
        }

        for (TripleExpectation triple : expected.mustContain()) {
            if (containsTriple(store, triple)) {
                continue;
            }
            if (semanticValidate && semanticValidator.matches(store, triple)) {
                System.out.printf("  semantic match: %s%n", formatTriple(triple));
                continue;
            }
            failures.add("Missing triple: " + formatTriple(triple));
        }

        return new ScenarioValidationResult(failures.isEmpty(), failures);
    }

    private boolean containsTriple(List<StatementSnapshot> store, TripleExpectation expected) {
        return store.stream().anyMatch(actual -> TripleMatcher.matches(actual, expected));
    }

    private static String formatTriple(TripleExpectation triple) {
        return triple.subject() + " | " + triple.predicate() + " | " + triple.object();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').trim();
        return oneLine.length() <= maxLength ? oneLine : oneLine.substring(0, maxLength) + "...";
    }
}
