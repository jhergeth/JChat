package name.hergeth.jchat.debug;

import name.hergeth.jchat.ai.KnowledgeStore;
import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.openai.dto.ChatMessage;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Singleton
public class DebugTraceService {

    private final DebugTraceStore traceStore;
    private final KnowledgeStore knowledgeStore;

    public DebugTraceService(DebugTraceStore traceStore, KnowledgeStore knowledgeStore) {
        this.traceStore = traceStore;
        this.knowledgeStore = knowledgeStore;
    }

    public void record(
            String conversationId,
            String requestType,
            String userInput,
            List<String> retrievedContext,
            List<ChatMessage> prompt,
            String llmResponse,
            String chatProvider) {
        TurnDebugSnapshot snapshot = new TurnDebugSnapshot(
                UUID.randomUUID().toString(),
                Instant.now(),
                conversationId,
                requestType,
                userInput,
                retrievedContext,
                prompt.stream().map(m -> new PromptLine(m.role(), m.content())).toList(),
                llmResponse,
                chatProvider,
                toViews(knowledgeStore.all(conversationId)));
        traceStore.add(snapshot);
    }

    public List<StatementView> knowledgeStore(String conversationId) {
        return toViews(knowledgeStore.all(conversationId));
    }

    private static List<StatementView> toViews(List<Statement> statements) {
        return statements.stream()
                .map(s -> new StatementView(
                        s.subject(), s.predicate(), s.object(),
                        s.turnId(), s.createdAt()))
                .toList();
    }
}
