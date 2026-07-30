package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class InMemoryKnowledgeStore implements KnowledgeStore {

    private final Map<String, List<Statement>> statementsByConversation = new ConcurrentHashMap<>();

    @Override
    public void add(Statement statement) {
        statementsByConversation
                .computeIfAbsent(statement.conversationId(), id -> new ArrayList<>())
                .add(statement);
    }

    @Override
    public List<Statement> all(String conversationId) {
        List<Statement> statements = statementsByConversation.get(conversationId);
        return statements == null ? List.of() : List.copyOf(statements);
    }
}
