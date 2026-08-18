package name.hergeth.jchat.ai;

import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import name.hergeth.jchat.ai.context.ResolvedContext;
import name.hergeth.jchat.ai.model.Statement;

import java.util.List;

@Singleton
@Replaces(KnowledgeStoreRetriever.class)
public class RelevanceRetriever implements Retriever {

    private final KnowledgeStore knowledgeStore;
    private final KnowledgeLimits limits;

    public RelevanceRetriever(KnowledgeStore knowledgeStore, KnowledgeLimits limits) {
        this.knowledgeStore = knowledgeStore;
        this.limits = limits;
    }

    @Override
    public List<Statement> retrieve(String conversationId, ResolvedContext context) {
        ResolvedContext safeContext = context == null ? ResolvedContext.plain("") : context;
        List<Statement> all = knowledgeStore.all(conversationId);

        if (safeContext.hasFocusEntities()) {
            EntityIndex index = EntityIndex.from(all);
            List<Statement> bundle = EntityFactExpander.expand(
                    index, safeContext.focusEntityKeys(), limits.maxKnowledgeInContext());
            if (!bundle.isEmpty()) {
                return bundle;
            }
        }

        List<Statement> pool = RetrieverStatementSelector.select(
                all,
                limits.minPromptTurns(),
                limits.minPromptStatements(),
                limits.maxPromptStatements());
        return StatementRelevanceScorer.rank(
                pool, safeContext.queryForScoring(), limits.maxKnowledgeInContext());
    }
}
