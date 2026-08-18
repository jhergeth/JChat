package name.hergeth.jchat.debug;

import name.hergeth.jchat.ai.KnowledgeStore;
import name.hergeth.jchat.ai.context.AmbientContext;
import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.ai.search.SearchTrace;
import name.hergeth.jchat.openai.dto.ChatMessage;
import name.hergeth.jchat.tools.ToolExecutionRecord;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class DebugTraceService {

    private final DebugTraceStore traceStore;
    private final KnowledgeStore knowledgeStore;

    public DebugTraceService(DebugTraceStore traceStore, KnowledgeStore knowledgeStore) {
        this.traceStore = traceStore;
        this.knowledgeStore = knowledgeStore;
    }

    public String record(
            String conversationId,
            String requestType,
            String userInput,
            List<String> retrievedContext,
            AmbientContext ambientContext,
            List<ChatMessage> prompt,
            String llmResponse,
            String chatProvider,
            SearchTrace searchTrace) {
        return record(conversationId, requestType, userInput, retrievedContext, ambientContext,
                prompt, llmResponse, chatProvider, searchTrace, List.of());
    }

    public String record(
            String conversationId,
            String requestType,
            String userInput,
            List<String> retrievedContext,
            AmbientContext ambientContext,
            List<ChatMessage> prompt,
            String llmResponse,
            String chatProvider,
            SearchTrace searchTrace,
            List<ToolExecutionRecord> toolCalls) {
        String id = UUID.randomUUID().toString();
        TurnDebugSnapshot snapshot = new TurnDebugSnapshot(
                id,
                Instant.now(),
                conversationId,
                requestType,
                userInput,
                retrievedContext,
                AmbientContextViews.from(ambientContext),
                prompt.stream().map(m -> new PromptLine(m.role(), m.content())).toList(),
                llmResponse,
                chatProvider,
                SearchTraceView.from(searchTrace),
                toViews(knowledgeStore.all(conversationId)),
                toToolViews(toolCalls));
        traceStore.add(snapshot);
        return id;
    }

    public void updateSearchTrace(String traceId, SearchTrace searchTrace) {
        if (traceId == null || traceId.isBlank() || searchTrace == null) {
            return;
        }
        Optional<TurnDebugSnapshot> existing = traceStore.findById(traceId);
        if (existing.isEmpty()) {
            return;
        }
        TurnDebugSnapshot prior = existing.get();
        TurnDebugSnapshot updated = new TurnDebugSnapshot(
                prior.id(),
                prior.timestamp(),
                prior.conversationId(),
                prior.requestType(),
                prior.userInput(),
                prior.retrievedContext(),
                prior.ambientContext(),
                prior.prompt(),
                prior.llmResponse(),
                prior.chatProvider(),
                SearchTraceView.from(searchTrace),
                prior.knowledgeStore(),
                prior.toolCalls());
        traceStore.replace(updated);
    }

    public List<StatementView> knowledgeStore(String conversationId) {
        return toViews(knowledgeStore.all(conversationId));
    }

    private static List<ToolCallView> toToolViews(List<ToolExecutionRecord> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }
        return toolCalls.stream()
                .map(t -> new ToolCallView(
                        t.step(),
                        t.toolName(),
                        t.argumentsJson(),
                        t.result(),
                        t.error(),
                        t.durationMs(),
                        t.dataSource(),
                        t.searchQuery()))
                .toList();
    }

    private static List<StatementView> toViews(List<Statement> statements) {
        return statements.stream()
                .map(s -> new StatementView(
                        s.subject(), s.predicate(), s.object(),
                        s.turnId(), s.source().name(), s.createdAt()))
                .toList();
    }
}
