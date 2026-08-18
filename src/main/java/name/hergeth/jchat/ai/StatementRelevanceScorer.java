package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class StatementRelevanceScorer {

    private StatementRelevanceScorer() {}

    static List<Statement> mostRecent(List<Statement> statements, int maxResults) {
        return recentStatements(statements, maxResults);
    }

    static List<Statement> rank(List<Statement> statements, String query, int maxResults) {
        List<RankedStatement> scored = rankScored(statements, query, maxResults);
        List<Statement> relevant = scored.stream()
                .filter(ranked -> ranked.score() > 0)
                .map(RankedStatement::statement)
                .toList();
        return relevant.isEmpty()
                ? recentStatements(statements, maxResults)
                : relevant;
    }

    static List<RankedStatement> rankScored(List<Statement> statements, String query, int maxResults) {
        return rankScoredInternal(statements, query, maxResults, false);
    }

    /** Store lookup: match query terms against subject only (simple entity facts). */
    static List<RankedStatement> rankScoredForStoreLookup(
            List<Statement> statements,
            String query,
            int maxResults) {
        return rankScoredInternal(statements, query, maxResults, true);
    }

    private static List<RankedStatement> rankScoredInternal(
            List<Statement> statements,
            String query,
            int maxResults,
            boolean subjectOnly) {
        if (statements.isEmpty()) {
            return List.of();
        }
        Set<String> terms = QueryTerms.from(query);
        if (terms.isEmpty()) {
            return List.of();
        }

        List<RankedStatement> scored = statements.stream()
                .map(statement -> new RankedStatement(
                        statement,
                        subjectOnly
                                ? scoreStoreLookup(statement, terms, query)
                                : score(statement, terms, query)))
                .filter(entry -> entry.score() > 0)
                .sorted(Comparator
                        .comparingInt(RankedStatement::score).reversed()
                        .thenComparing(s -> s.statement().createdAt(), Comparator.reverseOrder()))
                .limit(maxResults)
                .toList();

        return scored;
    }

    private static int score(Statement statement, Set<String> terms, String query) {
        String subject = normalize(statement.subject());
        String predicate = normalize(statement.predicate());
        String object = normalize(statement.object());
        int total = 0;
        for (String term : terms) {
            if (TermMatcher.matches(subject, term)) {
                total += 3;
            }
            if (TermMatcher.matches(predicate, term)) {
                total += 2;
            }
            if (TermMatcher.matches(object, term)) {
                total += 2;
            }
        }
        total -= countryMismatchPenalty(query, subject, predicate, object);
        return Math.max(0, total);
    }

    private static int scoreStoreLookup(Statement statement, Set<String> terms, String query) {
        String subject = normalize(statement.subject());
        String predicate = normalize(statement.predicate());
        String object = normalize(statement.object());
        int total = 0;
        for (String term : terms) {
            if (TermMatcher.matches(subject, term)) {
                total += 4;
            } else if (TermMatcher.matches(object, term)) {
                total += 3;
            } else if (TermMatcher.matches(predicate, term)) {
                total += 1;
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

    record RankedStatement(Statement statement, int score) {}
}
