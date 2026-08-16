package name.hergeth.jchat.ai.search;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import name.hergeth.jchat.ai.context.AmbientContext;
import name.hergeth.jchat.openai.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class SearchOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(SearchOrchestrator.class);

    private final boolean enabled;
    private final int maxResults;
    private final WebSearchProviderFactory providerFactory;
    private final SearchQueryPlanner queryPlanner;

    public SearchOrchestrator(
            @Value("${app.search.enabled:false}") boolean enabled,
            @Value("${app.search.max-results:5}") int maxResults,
            WebSearchProviderFactory providerFactory,
            SearchQueryPlanner queryPlanner) {
        this.enabled = enabled;
        this.maxResults = maxResults;
        this.providerFactory = providerFactory;
        this.queryPlanner = queryPlanner;
    }

    public boolean isEnabled() {
        return enabled && providerFactory.activeProvider().isPresent();
    }

    public SearchTrace maybeSearch(String conversationId, String userMessage) {
        return maybeSearch(conversationId, userMessage, List.of());
    }

    public SearchTrace maybeSearch(String conversationId, String userMessage, List<ChatMessage> messages) {
        return maybeSearch(conversationId, userMessage, messages, null);
    }

    public SearchTrace maybeSearch(
            String conversationId,
            String userMessage,
            List<ChatMessage> messages,
            AmbientContext ambientContext) {
        if (!enabled) {
            return SearchTrace.disabled("app.search.enabled=false");
        }
        if (providerFactory.activeProvider().isEmpty()) {
            String msg = "Websuche nicht verfügbar (Wikipedia deaktiviert, XNSEARCH_URL nicht gesetzt)";
            LOG.info("Web search disabled for {}: {}", conversationId, msg);
            return SearchTrace.disabled(msg);
        }

        List<String> recentUserMessages = recentUserMessages(messages);

        try {
            SearchDecision decision = resolveDecision(userMessage, recentUserMessages, ambientContext);
            if (!decision.search() || decision.query().isBlank()) {
                LOG.debug("Search skipped by planner/heuristic for conversation {}", conversationId);
                return SearchTrace.plannerSkip();
            }

            WebSearchProvider provider = providerFactory.activeProvider().orElseThrow();
            List<SearchSnippet> snippets = provider.search(decision.query(), maxResults);
            snippets = SnippetRelevanceRanker.rank(snippets, decision.query());

            if (snippets.isEmpty()) {
                LOG.info("Search for '{}' returned no snippets", decision.query());
                return new SearchTrace(true, "no_snippets", "Suche lieferte 0 Treffer",
                        decision.query(), 0, List.of(), List.of());
            }

            SearchTrace trace = new SearchTrace(true, "success",
                    "OK — Triple-Extraktion folgt nach der Antwort",
                    decision.query(), snippets.size(), List.of(), snippets, "");
            String promptContext = SearchPromptFormatter.format(trace);
            trace = new SearchTrace(true, "success",
                    "OK — Triple-Extraktion folgt nach der Antwort",
                    decision.query(), snippets.size(), List.of(), snippets, promptContext);

            LOG.info("Search for '{}': {} snippets (prompt ready, triples deferred)",
                    decision.query(), snippets.size());

            return trace;
        } catch (Exception e) {
            LOG.warn("Web search failed for conversation {} — continuing without search", conversationId, e);
            return SearchTrace.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private SearchDecision resolveDecision(
            String userMessage,
            List<String> recentUserMessages,
            AmbientContext ambientContext) {
        if (!SearchIntentHeuristic.shouldSearch(userMessage, recentUserMessages)) {
            LOG.debug("Search skipped by heuristic — no LLM planner call");
            return SearchDecision.skip();
        }

        SearchDecision plannerDecision = queryPlanner.plan(userMessage, recentUserMessages, ambientContext);
        if (plannerDecision.search() && !plannerDecision.query().isBlank()) {
            String query = SearchQueryNormalizer.sanitize(plannerDecision.query());
            LOG.info("Search planner query: {}", query);
            return SearchDecision.go(query);
        }

        String fallback = SearchIntentHeuristic.fallbackQuery(userMessage, recentUserMessages);
        if (!fallback.isBlank()) {
            String query = SearchQueryNormalizer.sanitize(fallback);
            LOG.info("Search heuristic fallback query: {}", query);
            return SearchDecision.go(query);
        }

        LOG.debug("Search skipped — planner declined and heuristic produced no query");
        return SearchDecision.skip();
    }

    private static List<String> recentUserMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<String> userMessages = new ArrayList<>();
        for (ChatMessage message : messages) {
            if ("user".equals(message.role()) && message.content() != null && !message.content().isBlank()) {
                userMessages.add(message.content().trim());
            }
        }
        return userMessages;
    }
}
