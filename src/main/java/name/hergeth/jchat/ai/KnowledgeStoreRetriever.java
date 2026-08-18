package name.hergeth.jchat.ai;

import io.micronaut.context.annotation.Replaces;
import name.hergeth.jchat.ai.context.ResolvedContext;
import name.hergeth.jchat.ai.model.Statement;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
@Replaces(NoopRetriever.class)
public class KnowledgeStoreRetriever implements Retriever {

    private final KnowledgeStore knowledgeStore;

    public KnowledgeStoreRetriever(KnowledgeStore knowledgeStore) {
        this.knowledgeStore = knowledgeStore;
    }

    @Override
    public List<Statement> retrieve(String conversationId, ResolvedContext context) {
        return knowledgeStore.all(conversationId);
    }
}
