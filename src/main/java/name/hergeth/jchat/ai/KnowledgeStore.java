package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.List;

public interface KnowledgeStore {
    void add(Statement statement);
    void replaceAll(String conversationId, List<Statement> statements);
    List<Statement> all(String conversationId);
}
