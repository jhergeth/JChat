package name.hergeth.jchat.ai.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import name.hergeth.jchat.ai.PromptLoader;
import name.hergeth.jchat.ai.context.AmbientContext;
import name.hergeth.jchat.ai.context.AmbientContextFormatter;
import name.hergeth.jchat.ai.llm.AiServiceFactory;
import name.hergeth.jchat.ai.llm.TaskRouter;
import name.hergeth.jchat.openai.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Singleton
public class SearchQueryPlanner {

    private static final Logger LOG = LoggerFactory.getLogger(SearchQueryPlanner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiServiceFactory aiServiceFactory;
    private final TaskRouter taskRouter;
    private final String planPrompt;

    public SearchQueryPlanner(
            AiServiceFactory aiServiceFactory,
            TaskRouter taskRouter,
            @Value("${app.search-plan-prompt-path:search-plan-prompt.txt}") String promptPath) throws IOException {
        this.aiServiceFactory = aiServiceFactory;
        this.taskRouter = taskRouter;
        this.planPrompt = PromptLoader.load(promptPath);
    }

    public SearchDecision plan(String userMessage) {
        return plan(userMessage, List.of());
    }

    public SearchDecision plan(String userMessage, List<String> recentUserMessages) {
        return plan(userMessage, recentUserMessages, null);
    }

    public SearchDecision plan(String userMessage, List<String> recentUserMessages, AmbientContext ambientContext) {
        String provider = taskRouter.providerFor("search-plan");
        String plannerInput = buildPlannerInput(userMessage, recentUserMessages, ambientContext);
        String response = aiServiceFactory.chat(provider, List.of(
                new ChatMessage("system", planPrompt),
                new ChatMessage("user", plannerInput)));

        return parseDecision(response);
    }

    private static String buildPlannerInput(
            String userMessage,
            List<String> recentUserMessages,
            AmbientContext ambientContext) {
        StringBuilder sb = new StringBuilder();
        if (ambientContext != null) {
            sb.append(AmbientContextFormatter.formatForPlanner(ambientContext)).append("\n\n");
        }
        if (recentUserMessages == null || recentUserMessages.size() <= 1) {
            sb.append("Aktuelle Nachricht: ").append(userMessage);
            return sb.toString();
        }
        sb.append("Vorherige Nutzer-Nachrichten:\n");
        int start = Math.max(0, recentUserMessages.size() - 4);
        for (int i = start; i < recentUserMessages.size() - 1; i++) {
            sb.append("- ").append(recentUserMessages.get(i)).append('\n');
        }
        sb.append("\nAktuelle Nachricht: ").append(userMessage);
        return sb.toString();
    }

    private SearchDecision parseDecision(String response) {
        try {
            String json = extractJson(response);
            JsonNode node = MAPPER.readTree(json);
            boolean search = node.path("search").asBoolean(false);
            String query = node.path("query").asText("").trim();
            if (search && !query.isBlank()) {
                return SearchDecision.go(query);
            }
            return SearchDecision.skip();
        } catch (Exception e) {
            LOG.warn("Failed to parse search plan JSON: {}", truncate(response));
            return SearchDecision.skip();
        }
    }

    private static String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private static String truncate(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').trim();
        return oneLine.length() <= 120 ? oneLine : oneLine.substring(0, 120) + "...";
    }
}
