package name.hergeth.jchat.ai.search;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import name.hergeth.jchat.ai.KnowledgeStoreMatch;
import name.hergeth.jchat.ai.KnowledgeStoreSearchService;
import name.hergeth.jchat.ai.context.AmbientContext;
import name.hergeth.jchat.ai.model.Statement;
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
    private final KnowledgeStoreSearchService knowledgeStoreSearch;
    private final boolean knowledgeStoreFirst;

    public SearchOrchestrator(
            @Value("${app.search.enabled:false}") boolean enabled,
            @Value("${app.search.max-results:5}") int maxResults,
            @Value("${app.search.knowledge-store-first:true}") boolean knowledgeStoreFirst,
            WebSearchProviderFactory providerFactory,
            SearchQueryPlanner queryPlanner,
            KnowledgeStoreSearchService knowledgeStoreSearch) {
        this.enabled = enabled;
        this.maxResults = maxResults;
        this.knowledgeStoreFirst = knowledgeStoreFirst;
        this.providerFactory = providerFactory;
        this.queryPlanner = queryPlanner;
        this.knowledgeStoreSearch = knowledgeStoreSearch;
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

            if (knowledgeStoreFirst) {
                var storeHit = knowledgeStoreSearch.tryMatch(conversationId, userMessage, decision.query());
                if (storeHit.isPresent()) {
                    return traceFromKnowledgeStore(storeHit.get());
                }
            }

            return executeExternalSearch(decision.query(), "Triple-Extraktion folgt nach der Antwort");
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

    /** LLM tool path — always external web search (ignores knowledge store). */
    public SearchTrace searchWithQueryForTool(String conversationId, String query) {
        if (!enabled) {
            return SearchTrace.disabled("app.search.enabled=false");
        }
        if (providerFactory.activeProvider().isEmpty()) {
            return SearchTrace.disabled("Websuche nicht verfügbar");
        }
        String sanitized = SearchQueryNormalizer.sanitize(query == null ? "" : query.trim());
        if (sanitized.isBlank()) {
            return SearchTrace.disabled("Leere Suchanfrage");
        }
        return executeExternalSearch(sanitized, "Tool-Suche");
    }

    /** @deprecated use {@link #searchWithQueryForTool} for tool calls */
    public SearchTrace searchWithQuery(String query) {
        return searchWithQueryForTool(null, query);
    }

    /** @deprecated use {@link #searchWithQueryForTool} for tool calls */
    public SearchTrace searchWithQuery(String conversationId, String userMessage, String query) {
        return searchWithQueryForTool(conversationId, query);
    }

    private SearchTrace executeExternalSearch(String sanitized, String detailPrefix) {
        try {
            WebSearchProvider provider = providerFactory.activeProvider().orElseThrow();
            List<SearchSnippet> snippets = provider.search(sanitized, maxResults);
            snippets = SnippetRelevanceRanker.rank(snippets, sanitized);

            if (snippets.isEmpty()) {
                LOG.info("Web search for '{}' returned no snippets", sanitized);
                return new SearchTrace(true, "no_snippets", "Suche lieferte 0 Treffer",
                        sanitized, 0, List.of(), List.of(), List.of(), "");
            }

            SearchTrace trace = new SearchTrace(true, "success",
                    "OK — " + detailPrefix,
                    sanitized, snippets.size(), List.of(), List.of(), snippets, "");
            String promptContext = "Tool-Suche".equals(detailPrefix)
                    ? ToolSearchResultFormatter.format(trace)
                    : SearchPromptFormatter.format(trace);
            trace = new SearchTrace(true, "success",
                    "OK — " + detailPrefix,
                    sanitized, snippets.size(), List.of(), List.of(), snippets, promptContext);

            LOG.info("Web search for '{}': {} snippets", sanitized, snippets.size());
            return trace;
        } catch (Exception e) {
            LOG.warn("Web search failed for query '{}'", sanitized, e);
            return SearchTrace.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
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

    private SearchTrace traceFromKnowledgeStore(KnowledgeStoreMatch match) {
        List<String> tripleLines = match.statements().stream()
                .map(Statement::formatForPrompt)
                .toList();
        LOG.info("Search satisfied from knowledge store for '{}': {} fact(s), top score {}",
                match.query(), match.statements().size(), match.topScore());
        return new SearchTrace(
                true,
                "knowledge_store",
                "OK — aus Wissensspeicher (keine Websuche, nicht re-gespeichert)",
                match.query(),
                0,
                List.of(),
                tripleLines,
                List.of(),
                match.promptContext());
    }
}
