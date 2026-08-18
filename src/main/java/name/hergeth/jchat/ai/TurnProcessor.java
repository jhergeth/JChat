package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.ai.model.Turn;
import name.hergeth.jchat.ai.llm.BackgroundLlmExecutor;
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
    private final KnowledgeStoreWriter knowledgeStoreWriter;
    private final BackgroundLlmExecutor backgroundLlmExecutor;

    public TurnProcessor(
            StatementExtractor statementExtractor,
            StatementNormalizer statementNormalizer,
            KnowledgeStoreWriter knowledgeStoreWriter,
            BackgroundLlmExecutor backgroundLlmExecutor) {
        this.statementExtractor = statementExtractor;
        this.statementNormalizer = statementNormalizer;
        this.knowledgeStoreWriter = knowledgeStoreWriter;
        this.backgroundLlmExecutor = backgroundLlmExecutor;
    }

    public void scheduleProcess(Turn turn) {
        backgroundLlmExecutor.run(
                "extraction:" + turn.conversationId(),
                () -> process(turn));
    }

    public void process(Turn turn) {
        List<Statement> raw = statementExtractor.extract(turn);
        List<Statement> normalized = statementNormalizer.normalize(raw);

        if (normalized.isEmpty()) {
            if (raw.isEmpty()) {
                LOG.debug("Knowledge store unchanged for {}: no triples extracted (turn {})",
                        turn.conversationId(), turn.turnId());
            } else {
                LOG.warn("Knowledge store unchanged for {}: raw={}, all filtered (turn {})",
                        turn.conversationId(), raw.size(), turn.turnId());
            }
            return;
        }

        knowledgeStoreWriter.merge(turn.conversationId(), normalized, MAX_STATEMENTS);

        LOG.debug("Knowledge store updated for {}: raw={}, stored={} (turn {})",
                turn.conversationId(), raw.size(),
                Math.min(normalized.size(), MAX_STATEMENTS), turn.turnId());
    }
}
