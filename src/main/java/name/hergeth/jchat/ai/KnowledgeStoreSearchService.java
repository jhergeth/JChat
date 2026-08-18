package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@jakarta.inject.Singleton
public class KnowledgeStoreSearchService {

    private static final int STRONG_MATCH_SCORE = 4;
    private static final int LIST_MATCH_SCORE = 3;
    private static final int LIST_MIN_FACTS = 3;

    private final KnowledgeStore knowledgeStore;
    private final KnowledgeLimits limits;

    public KnowledgeStoreSearchService(KnowledgeStore knowledgeStore, KnowledgeLimits limits) {
        this.knowledgeStore = knowledgeStore;
        this.limits = limits;
    }

    public Optional<KnowledgeStoreMatch> tryMatch(
            String conversationId,
            String userMessage,
            String searchQuery) {
        if (conversationId == null || conversationId.isBlank()) {
            return Optional.empty();
        }
        String lookupQuery = combinedQuery(userMessage, searchQuery);
        List<Statement> all = knowledgeStore.all(conversationId);
        if (all.isEmpty()) {
            return Optional.empty();
        }

        List<StatementRelevanceScorer.RankedStatement> ranked =
                StatementRelevanceScorer.rankScoredForStoreLookup(all, lookupQuery, limits.maxStoreStatements());

        List<StatementRelevanceScorer.RankedStatement> relevant;
        if (isBroadListQuestion(userMessage, searchQuery)) {
            relevant = all.stream()
                    .filter(KnowledgeStoreSearchService::isCabinetOrGovernmentFact)
                    .filter(statement -> !isGeographicallyIncompatible(statement, lookupQuery))
                    .map(statement -> new StatementRelevanceScorer.RankedStatement(statement, 1))
                    .toList();
            if (relevant.size() < LIST_MIN_FACTS) {
                return Optional.empty();
            }
        } else {
            relevant = ranked.stream()
                    .filter(entry -> entry.score() > 0)
                    .toList();
            if (relevant.isEmpty()) {
                return Optional.empty();
            }
            int topScore = relevant.get(0).score();
            if (topScore < STRONG_MATCH_SCORE) {
                return Optional.empty();
            }
        }

        List<Statement> statements = relevant.stream()
                .map(StatementRelevanceScorer.RankedStatement::statement)
                .limit(limits.maxKnowledgeInContext())
                .toList();
        String promptContext = KnowledgeStorePromptFormatter.format(statements);
        if (promptContext.isBlank()) {
            return Optional.empty();
        }

        String query = searchQuery == null || searchQuery.isBlank() ? userMessage : searchQuery;
        int topScore = relevant.get(0).score();
        return Optional.of(new KnowledgeStoreMatch(query.trim(), statements, promptContext, topScore));
    }

    private static boolean isCabinetOrGovernmentFact(Statement statement) {
        String predicate = normalize(statement.predicate());
        String object = normalize(statement.object());
        return predicate.contains("minister")
                || predicate.contains("kabinett")
                || predicate.contains("secretary")
                || predicate.contains("amtsinhaber")
                || predicate.contains("president")
                || predicate.contains("praesident")
                || predicate.contains("präsident")
                || predicate.contains("pressesprecher")
                || predicate.contains("regierung")
                || object.contains("minister")
                || object.contains("ministerium");
    }

    private static boolean isGeographicallyIncompatible(Statement statement, String query) {
        String q = query.toLowerCase(Locale.ROOT);
        String fact = normalize(statement.subject()) + " "
                + normalize(statement.predicate()) + " "
                + normalize(statement.object());
        if (mentionsUsa(q) && mentionsAustralia(fact) && !mentionsUsa(fact)) {
            return true;
        }
        if (mentionsGermany(q) && mentionsUsa(fact) && !mentionsGermany(fact)) {
            return true;
        }
        return false;
    }

    private static boolean mentionsUsa(String text) {
        return text.contains("usa")
                || text.contains("vereinigten staaten")
                || text.contains("vereinigte staaten")
                || text.contains("amerika")
                || text.contains("white house")
                || text.contains("weiße haus")
                || text.contains("weisse haus");
    }

    private static boolean mentionsGermany(String text) {
        return text.contains("deutschland")
                || text.contains("bundesrepublik")
                || text.contains(" merz");
    }

    private static boolean mentionsAustralia(String text) {
        return text.contains("australien")
                || text.contains("canberra")
                || text.contains("sydney")
                || text.contains("melbourne");
    }

    private static boolean isBroadListQuestion(String userMessage, String searchQuery) {
        String combined = combinedQuery(userMessage, searchQuery).toLowerCase(Locale.ROOT);
        return combined.contains("mitglieder")
                || combined.contains("kabinett")
                || combined.contains("cabinet")
                || combined.contains("ministerien")
                || combined.contains("ministerium")
                || combined.contains("members")
                || (combined.contains("regierung") && (combined.contains("welche") || combined.contains("wer")))
                || (combined.contains("government") && combined.contains("members"));
    }

    private static String combinedQuery(String userMessage, String searchQuery) {
        String user = userMessage == null ? "" : userMessage.trim();
        String search = searchQuery == null ? "" : searchQuery.trim();
        if (user.isBlank()) {
            return search;
        }
        if (search.isBlank() || user.equalsIgnoreCase(search)) {
            return user;
        }
        return user + " " + search;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
