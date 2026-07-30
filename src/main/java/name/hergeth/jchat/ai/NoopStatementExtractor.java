package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.ai.model.Turn;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class NoopStatementExtractor implements StatementExtractor {

    @Override
    public List<Statement> extract(Turn turn) {
        return List.of();
    }
}
