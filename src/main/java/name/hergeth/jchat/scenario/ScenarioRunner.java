package name.hergeth.jchat.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import name.hergeth.jchat.openai.dto.ChatMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ScenarioRunner {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final JChatHttpClient client;
    private final Path scenariosDir;
    private final Path outputDir;
    private final boolean validate;

    public ScenarioRunner(JChatHttpClient client, Path scenariosDir, Path outputDir, boolean validate) {
        this.client = client;
        this.scenariosDir = scenariosDir;
        this.outputDir = outputDir;
        this.validate = validate;
    }

    public List<ScenarioRunResult> runAll() throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        List<ScenarioDefinition> scenarios = ScenarioLoader.loadAll(scenariosDir);
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
                    scenario.model(), scenario.conversationId(), List.copyOf(history));
            history.add(new ChatMessage("assistant", assistantResponse));

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
            throws IOException {
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
            if (!containsTriple(store, triple)) {
                failures.add("Missing triple: " + formatTriple(triple));
            }
        }

        return new ScenarioValidationResult(failures.isEmpty(), failures);
    }

    private static boolean containsTriple(List<StatementSnapshot> store, TripleExpectation expected) {
        return store.stream().anyMatch(actual -> matches(actual, expected));
    }

    private static boolean matches(StatementSnapshot actual, TripleExpectation expected) {
        if (!normalize(actual.subject()).equals(normalize(expected.subject()))) {
            return false;
        }
        if (!normalize(actual.predicate()).equals(normalize(expected.predicate()))) {
            return false;
        }
        String actualObject = normalize(actual.object());
        String expectedObject = normalize(expected.object());
        return actualObject.contains(expectedObject) || expectedObject.contains(actualObject);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
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
