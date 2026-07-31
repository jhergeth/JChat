package name.hergeth.jchat.ai;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import name.hergeth.jchat.ai.llm.AiServiceFactory;
import name.hergeth.jchat.ai.llm.TaskRouter;
import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.ai.model.Turn;
import name.hergeth.jchat.openai.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Singleton
@Replaces(NoopStatementExtractor.class)
public class LlmStatementExtractor implements StatementExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(LlmStatementExtractor.class);

    private final AiServiceFactory aiServiceFactory;
    private final TaskRouter taskRouter;
    private final StatementParser statementParser;
    private final String extractionPrompt;

    public LlmStatementExtractor(
            AiServiceFactory aiServiceFactory,
            TaskRouter taskRouter,
            StatementParser statementParser,
            @Value("${app.extraction-prompt-path}") String promptPath) throws IOException {
        this.aiServiceFactory = aiServiceFactory;
        this.taskRouter = taskRouter;
        this.statementParser = statementParser;
        this.extractionPrompt = loadPrompt(promptPath);
    }

    @Override
    public List<Statement> extract(Turn turn) {
        String provider = taskRouter.providerFor("extraction");
        List<ChatMessage> messages = List.of(
                new ChatMessage("system", extractionPrompt),
                new ChatMessage("user", buildExtractionInput(turn)));

        LOG.debug("Extracting statements for turn {} via {}", turn.turnId(), provider);
        String response = aiServiceFactory.chat(provider, messages);
        List<Statement> statements = statementParser.parse(response, turn);
        if (statements.isEmpty()) {
            LOG.info("Extraction returned no parseable triples for turn {} — LLM response: {}",
                    turn.turnId(), truncate(response));
        } else {
            LOG.debug("Extracted {} raw statements from turn {}", statements.size(), turn.turnId());
        }
        return statements;
    }

    private static String buildExtractionInput(Turn turn) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== GESAMTES GESPRAECH ===\n");
        sb.append(turn.conversationForExtraction()).append("\n\n");
        sb.append("=== LETZTER AUSTAUSCH (neue Fakten besonders beachten) ===\n");
        sb.append("User: ").append(turn.userMessage()).append('\n');
        sb.append("Assistant: ").append(turn.assistantMessage());
        return sb.toString().trim();
    }

    private static String truncate(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').trim();
        return oneLine.length() <= 200 ? oneLine : oneLine.substring(0, 200) + "...";
    }

    private static String loadPrompt(String path) throws IOException {
        try (InputStream resource = LlmStatementExtractor.class.getClassLoader().getResourceAsStream(path)) {
            if (resource != null) {
                return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        throw new IOException("Extraction prompt not found: " + path);
    }
}
