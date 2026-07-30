package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.List;

public interface Retriever {
    List<Statement> retrieve(String conversationId, String query);
}
