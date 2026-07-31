package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.ai.model.Turn;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Singleton
public class TurnProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TurnProcessor.class);
    private static final int MAX_STATEMENTS = 12;

    private final StatementExtractor statementExtractor;
    private final StatementNormalizer statementNormalizer;
    private final KnowledgeStore knowledgeStore;

    public TurnProcessor(
            StatementExtractor statementExtractor,
            StatementNormalizer statementNormalizer,
            KnowledgeStore knowledgeStore) {
        this.statementExtractor = statementExtractor;
        this.statementNormalizer = statementNormalizer;
        this.knowledgeStore = knowledgeStore;
    }

    public void process(Turn turn) {
        List<Statement> raw = statementExtractor.extract(turn);
        List<Statement> normalized = statementNormalizer.normalize(raw);

        if (normalized.isEmpty()) {
            if (raw.isEmpty()) {
                LOG.info("Knowledge store unchanged for {}: no triples extracted (turn {})",
                        turn.conversationId(), turn.turnId());
            } else {
                LOG.warn("Knowledge store unchanged for {}: raw={}, all filtered (turn {})",
                        turn.conversationId(), raw.size(), turn.turnId());
            }
            return;
        }

        List<Statement> merged = merge(knowledgeStore.all(turn.conversationId()), normalized);
        List<Statement> toStore = merged.stream().limit(MAX_STATEMENTS).toList();
        knowledgeStore.replaceAll(turn.conversationId(), toStore);

        LOG.info("Knowledge store updated for {}: raw={}, merged={}, stored={} (turn {})",
                turn.conversationId(), raw.size(), merged.size(), toStore.size(), turn.turnId());
    }

    private static List<Statement> merge(List<Statement> existing, List<Statement> extracted) {
        java.util.Map<String, Statement> byKey = new java.util.LinkedHashMap<>();
        for (Statement statement : existing) {
            byKey.put(StatementTextNormalizer.factKey(statement.subject(), statement.predicate()), statement);
        }
        for (Statement statement : extracted) {
            byKey.put(StatementTextNormalizer.factKey(statement.subject(), statement.predicate()), statement);
        }
        return byKey.values().stream()
                .sorted(java.util.Comparator.comparing(Statement::createdAt).reversed())
                .toList();
    }
}
