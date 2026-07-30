package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class NoopRetriever implements Retriever {

    @Override
    public List<Statement> retrieve(String conversationId, String query) {
        return List.of();
    }
}
