package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class IdentityStatementNormalizer implements StatementNormalizer {

    private static final Set<String> PLACEHOLDER_TOKENS = Set.of(
            "subject", "subjekt", "predicate", "predikat", "praedikat",
            "object", "objekt", "example", "beispiel");

    @Override
    public List<Statement> normalize(List<Statement> statements) {
        return statements.stream()
                .map(this::clean)
                .map(StatementSemanticFixer::fix)
                .filter(this::isComplete)
                .filter(s -> !isPlaceholder(s))
                .filter(s -> !StatementSemanticFixer.isInvalid(s))
                .filter(s -> !StatementTextNormalizer.isInvalidSubject(s.subject()))
                .filter(s -> !StatementTextNormalizer.isVagueObject(s.object()))
                .filter(s -> !StatementTextNormalizer.isPredicateTooLong(s.predicate()))
                .collect(Collectors.toMap(
                        s -> StatementTextNormalizer.factKey(s.subject(), s.predicate()),
                        s -> s,
                        (a, b) -> b))
                .values().stream()
                .toList();
    }

    private Statement clean(Statement statement) {
        return new Statement(
                StatementTextNormalizer.normalizeSubject(statement.subject()),
                StatementTextNormalizer.normalizePredicate(statement.predicate()),
                StatementTextNormalizer.normalizeObject(statement.object()),
                statement.conversationId(),
                statement.turnId(),
                statement.createdAt());
    }

    private boolean isComplete(Statement statement) {
        return statement.subject().length() >= 2
                && statement.predicate().length() >= 2
                && statement.object().length() >= 2;
    }

    private boolean isPlaceholder(Statement statement) {
        if (isPlaceholderToken(statement.subject())
                || isPlaceholderToken(statement.predicate())
                || isPlaceholderToken(statement.object())) {
            return true;
        }
        String combined = (statement.subject() + " " + statement.predicate() + " " + statement.object())
                .toLowerCase(Locale.ROOT);
        return combined.contains("subject | predicate")
                || combined.contains("subjekt | predikat");
    }

    private boolean isPlaceholderToken(String value) {
        return PLACEHOLDER_TOKENS.contains(value.toLowerCase(Locale.ROOT).trim());
    }
}
