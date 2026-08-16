package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class StatementRelevanceScorer {

    private StatementRelevanceScorer() {}

    static List<Statement> rank(List<Statement> statements, String query, int maxResults) {
        if (statements.isEmpty()) {
            return List.of();
        }
        Set<String> terms = QueryTerms.from(query);
        if (terms.isEmpty()) {
            return recentStatements(statements, maxResults);
        }

        List<ScoredStatement> scored = statements.stream()
                .map(statement -> new ScoredStatement(statement, score(statement, terms, query)))
                .sorted(Comparator
                        .comparingInt(ScoredStatement::score).reversed()
                        .thenComparing(s -> s.statement().createdAt(), Comparator.reverseOrder()))
                .toList();

        List<Statement> relevant = scored.stream()
                .filter(s -> s.score() > 0)
                .limit(maxResults)
                .map(ScoredStatement::statement)
                .toList();

        return relevant.isEmpty()
                ? recentStatements(statements, maxResults)
                : relevant;
    }

    private static int score(Statement statement, Set<String> terms, String query) {
        String subject = normalize(statement.subject());
        String predicate = normalize(statement.predicate());
        String object = normalize(statement.object());
        int total = 0;
        for (String term : terms) {
            if (subject.contains(term)) {
                total += 3;
            }
            if (predicate.contains(term)) {
                total += 2;
            }
            if (object.contains(term)) {
                total += 2;
            }
        }
        total -= countryMismatchPenalty(query, subject, predicate, object);
        return Math.max(0, total);
    }

    private static int countryMismatchPenalty(String query, String subject, String predicate, String object) {
        String q = query.toLowerCase(Locale.ROOT);
        String combined = subject + " " + predicate + " " + object;
        if (mentionsUsa(q) && mentionsGermany(combined) && !mentionsUsa(combined)) {
            return 12;
        }
        if (mentionsAustria(q) && mentionsGermany(combined) && !mentionsAustria(combined)) {
            return 12;
        }
        if (mentionsGermany(q) && mentionsUsa(combined) && !mentionsGermany(combined)) {
            return 12;
        }
        return 0;
    }

    private static boolean mentionsUsa(String text) {
        return text.contains("usa")
                || text.contains("vereinigten staaten")
                || text.contains("vereinigte staaten")
                || text.contains("amerika");
    }

    private static boolean mentionsAustria(String text) {
        return text.contains("österreich") || text.contains("oesterreich");
    }

    private static boolean mentionsGermany(String text) {
        return text.contains("deutschland")
                || text.contains("bundesrepublik deutschland")
                || text.contains(" merz");
    }

    private static List<Statement> recentStatements(List<Statement> statements, int maxResults) {
        return statements.stream()
                .sorted(Comparator.comparing(Statement::createdAt).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private record ScoredStatement(Statement statement, int score) {}
}
