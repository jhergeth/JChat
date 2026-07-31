package name.hergeth.jchat.ai;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Value;
import name.hergeth.jchat.ai.model.Statement;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
@Replaces(KnowledgeStoreRetriever.class)
public class RelevanceRetriever implements Retriever {

    private final KnowledgeStore knowledgeStore;
    private final int maxStatements;

    public RelevanceRetriever(
            KnowledgeStore knowledgeStore,
            @Value("${app.retriever.max-statements:12}") int maxStatements) {
        this.knowledgeStore = knowledgeStore;
        this.maxStatements = maxStatements;
    }

    @Override
    public List<Statement> retrieve(String conversationId, String query) {
        List<Statement> all = knowledgeStore.all(conversationId);
        return StatementRelevanceScorer.rank(all, query, maxStatements);
    }
}
