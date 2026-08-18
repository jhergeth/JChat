package name.hergeth.jchat.ai.search;

import jakarta.inject.Singleton;
import name.hergeth.jchat.ai.KnowledgeLimits;
import name.hergeth.jchat.ai.KnowledgeStoreWriter;
import name.hergeth.jchat.ai.llm.BackgroundLlmExecutor;
import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.debug.DebugTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import name.hergeth.jchat.tools.ToolExecutionRecord;

@Singleton
public class SearchPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SearchPostProcessor.class);

    private final SearchTripleExtractor tripleExtractor;
    private final KnowledgeStoreWriter knowledgeStoreWriter;
    private final DebugTraceService debugTraceService;
    private final BackgroundLlmExecutor backgroundLlmExecutor;
    private final KnowledgeLimits limits;

    public SearchPostProcessor(
            SearchTripleExtractor tripleExtractor,
            KnowledgeStoreWriter knowledgeStoreWriter,
            DebugTraceService debugTraceService,
            BackgroundLlmExecutor backgroundLlmExecutor,
            KnowledgeLimits limits) {
        this.tripleExtractor = tripleExtractor;
        this.knowledgeStoreWriter = knowledgeStoreWriter;
        this.debugTraceService = debugTraceService;
        this.backgroundLlmExecutor = backgroundLlmExecutor;
        this.limits = limits;
    }

    public void scheduleAfterAnswer(
            String conversationId,
            String userMessage,
            String answer,
            String chatProvider,
            SearchTrace searchTrace,
            String debugTraceId,
            String turnId) {
        if (searchTrace == null || !searchTrace.searched() || !"success".equals(searchTrace.status())) {
            return;
        }
        if (searchTrace.snippets().isEmpty()) {
            return;
        }

        backgroundLlmExecutor.run(
                "search-extract:" + conversationId,
                () -> extractAndStore(
                        conversationId, userMessage, answer, chatProvider, searchTrace, debugTraceId, turnId));
    }

    public void scheduleAfterToolCalls(
            String conversationId,
            String userMessage,
            String answer,
            String chatProvider,
            List<ToolExecutionRecord> toolCalls,
            String turnId) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return;
        }
        for (ToolExecutionRecord record : toolCalls) {
            if (!record.hasWebSearchTrace()) {
                continue;
            }
            SearchTrace trace = record.searchTrace();
            backgroundLlmExecutor.run(
                    "tool-search-extract:" + conversationId + ":" + record.step(),
                    () -> extractAndStore(
                            conversationId, userMessage, answer, chatProvider, trace, null, turnId));
        }
    }

    private void extractAndStore(
            String conversationId,
            String userMessage,
            String answer,
            String chatProvider,
            SearchTrace searchTrace,
            String debugTraceId,
            String turnId) {
        try {
            String storeTurnId = turnId == null || turnId.isBlank()
                    ? "web-" + UUID.randomUUID().toString().substring(0, 8)
                    : turnId;
            List<SearchSnippet> snippets = searchTrace.snippets();

            List<Statement> wikiFacts = WikiOfficeHolderExtractor.extractOfficeFacts(
                    snippets, conversationId, storeTurnId);
            List<Statement> llmFacts = tripleExtractor.extractAfterAnswer(
                    userMessage, answer, snippets, conversationId, storeTurnId, chatProvider);
            List<Statement> statements = mergeStatements(wikiFacts, llmFacts);

            if (!statements.isEmpty()) {
                knowledgeStoreWriter.mergeSearchResults(
                        conversationId, statements, limits.maxStoreStatements());
            }

            List<String> tripleLines = statements.stream()
                    .map(Statement::formatForPrompt)
                    .toList();

            SearchTrace updated = new SearchTrace(
                    true,
                    "success",
                    statements.isEmpty() ? "Keine Triples extrahiert" : "OK (nach Antwort)",
                    searchTrace.query(),
                    searchTrace.snippetCount(),
                    tripleLines,
                    searchTrace.storeFacts(),
                    snippets,
                    searchTrace.promptContext());

            if (debugTraceId != null && !debugTraceId.isBlank()) {
                debugTraceService.updateSearchTrace(debugTraceId, updated);
            }

            LOG.info("Async search extraction for '{}': {} triples stored (conversation {})",
                    searchTrace.query(), statements.size(), conversationId);
        } catch (Exception e) {
            LOG.warn("Async search extraction failed for conversation {}", conversationId, e);
        }
    }

    private static List<Statement> mergeStatements(List<Statement> primary, List<Statement> secondary) {
        List<Statement> merged = new ArrayList<>(primary);
        for (Statement statement : secondary) {
            boolean duplicate = merged.stream().anyMatch(existing ->
                    existing.subject().equalsIgnoreCase(statement.subject())
                            && existing.predicate().equalsIgnoreCase(statement.predicate()));
            if (!duplicate) {
                merged.add(statement);
            }
        }
        return merged;
    }
}
