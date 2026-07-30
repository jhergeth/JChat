package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class IdentityStatementNormalizer implements StatementNormalizer {

    @Override
    public List<Statement> normalize(List<Statement> statements) {
        return statements.stream()
                .filter(this::isComplete)
                .toList();
    }

    private boolean isComplete(Statement statement) {
        return statement.subject() != null && !statement.subject().isBlank()
                && statement.predicate() != null && !statement.predicate().isBlank()
                && statement.object() != null && !statement.object().isBlank();
    }
}
