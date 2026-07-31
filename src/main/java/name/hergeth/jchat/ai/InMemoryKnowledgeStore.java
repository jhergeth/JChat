package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class InMemoryKnowledgeStore implements KnowledgeStore {

    private final java.util.Map<String, List<Statement>> statementsByConversation =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void add(Statement statement) {
        List<Statement> statements = statementsByConversation
                .computeIfAbsent(statement.conversationId(), id -> new ArrayList<>());
        String factKey = StatementTextNormalizer.factKey(statement.subject(), statement.predicate());
        statements.removeIf(existing ->
                StatementTextNormalizer.factKey(existing.subject(), existing.predicate()).equals(factKey));
        statements.add(statement);
    }

    @Override
    public void replaceAll(String conversationId, List<Statement> statements) {
        statementsByConversation.put(conversationId, new ArrayList<>(statements));
    }

    @Override
    public List<Statement> all(String conversationId) {
        List<Statement> statements = statementsByConversation.get(conversationId);
        return statements == null ? List.of() : List.copyOf(statements);
    }
}
