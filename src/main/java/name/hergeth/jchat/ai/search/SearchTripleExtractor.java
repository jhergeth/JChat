package name.hergeth.jchat.ai.search;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import name.hergeth.jchat.ai.IdentityStatementNormalizer;
import name.hergeth.jchat.ai.PromptLoader;
import name.hergeth.jchat.ai.StatementParser;
import name.hergeth.jchat.ai.llm.AiServiceFactory;
import name.hergeth.jchat.ai.llm.TaskRouter;
import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.openai.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class SearchTripleExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(SearchTripleExtractor.class);

    private final AiServiceFactory aiServiceFactory;
    private final TaskRouter taskRouter;
    private final StatementParser statementParser;
    private final IdentityStatementNormalizer statementNormalizer;
    private final String extractionPrompt;
    private final int maxTriples;

    public SearchTripleExtractor(
            AiServiceFactory aiServiceFactory,
            TaskRouter taskRouter,
            StatementParser statementParser,
            IdentityStatementNormalizer statementNormalizer,
            @Value("${app.search-extraction-prompt-path:search-extraction-prompt.txt}") String promptPath,
            @Value("${app.search.max-triples:6}") int maxTriples) throws IOException {
        this.aiServiceFactory = aiServiceFactory;
        this.taskRouter = taskRouter;
        this.statementParser = statementParser;
        this.statementNormalizer = statementNormalizer;
        this.extractionPrompt = PromptLoader.load(promptPath);
        this.maxTriples = maxTriples;
    }

    public List<Statement> extract(String userMessage, List<SearchSnippet> snippets, String conversationId, String turnId) {
        return extractAfterAnswer(userMessage, "", snippets, conversationId, turnId,
                taskRouter.providerFor("search-extract"));
    }

    public List<Statement> extractAfterAnswer(
            String userMessage,
            String assistantAnswer,
            List<SearchSnippet> snippets,
            String conversationId,
            String turnId,
            String provider) {
        if (snippets.isEmpty()) {
            return List.of();
        }

        String userContent = buildInput(userMessage, assistantAnswer, snippets);
        String response = aiServiceFactory.chat(provider, List.of(
                new ChatMessage("system", extractionPrompt),
                new ChatMessage("user", userContent)));

        Instant now = Instant.now();
        List<Statement> raw = statementParser.parse(response, conversationId, turnId, now);
        List<Statement> normalized = statementNormalizer.normalize(raw).stream()
                .filter(s -> !SearchTripleQualityFilter.isLowQuality(s))
                .toList();
        if (normalized.isEmpty() && !raw.isEmpty()) {
            LOG.warn("Search extraction: raw={}, normalized/filtered=0", raw.size());
        }
        return normalized.stream().limit(maxTriples).toList();
    }

    private static String buildInput(String userMessage, String assistantAnswer, List<SearchSnippet> snippets) {
        String snippetBlock = snippets.stream()
                .map(SearchSnippet::formatForExtraction)
                .collect(Collectors.joining("\n\n---\n\n"));
        StringBuilder input = new StringBuilder();
        input.append("Nutzerfrage: ").append(userMessage).append("\n\n");
        if (assistantAnswer != null && !assistantAnswer.isBlank()) {
            input.append("Assistenten-Antwort:\n").append(assistantAnswer).append("\n\n");
        }
        input.append("Websuche-Snippets:\n").append(snippetBlock);
        return input.toString();
    }
}
