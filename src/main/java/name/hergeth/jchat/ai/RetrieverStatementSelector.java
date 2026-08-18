package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the retriever candidate pool: recent turns first, then min/max statement bounds.
 */
final class RetrieverStatementSelector {

    private RetrieverStatementSelector() {}

    static List<Statement> select(
            List<Statement> all,
            int minTurns,
            int minStatements,
            int maxStatements) {
        if (all.isEmpty()) {
            return List.of();
        }

        List<Statement> orderedByRecency = all.stream()
                .sorted(Comparator.comparing(Statement::createdAt).reversed())
                .toList();

        Set<String> selectedKeys = new LinkedHashSet<>();
        List<Statement> selected = new ArrayList<>();

        if (minTurns > 0) {
            for (String turnId : recentTurnIds(all, minTurns)) {
                for (Statement statement : statementsForTurn(all, turnId)) {
                    addIfNew(selected, selectedKeys, statement);
                }
            }
        }

        selected = new ArrayList<>(trimToMax(selected, maxStatements));

        if (selected.size() < minStatements) {
            for (Statement statement : orderedByRecency) {
                if (selected.size() >= minStatements || selected.size() >= maxStatements) {
                    break;
                }
                addIfNew(selected, selectedKeys, statement);
            }
        }

        return trimToMax(selected, maxStatements);
    }

    private static List<String> recentTurnIds(List<Statement> all, int minTurns) {
        Map<String, InstantHolder> turnRecency = new LinkedHashMap<>();
        for (Statement statement : all) {
            String turnId = normalizeTurnId(statement.turnId());
            InstantHolder existing = turnRecency.get(turnId);
            if (existing == null || statement.createdAt().isAfter(existing.latest())) {
                turnRecency.put(turnId, new InstantHolder(statement.createdAt()));
            }
        }
        return turnRecency.entrySet().stream()
                .sorted(Map.Entry.<String, InstantHolder>comparingByValue(
                        Comparator.comparing(InstantHolder::latest)).reversed())
                .limit(minTurns)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static List<Statement> statementsForTurn(List<Statement> all, String turnId) {
        return all.stream()
                .filter(statement -> normalizeTurnId(statement.turnId()).equals(turnId))
                .sorted(Comparator.comparing(Statement::createdAt).reversed())
                .toList();
    }

    private static void addIfNew(List<Statement> selected, Set<String> selectedKeys, Statement statement) {
        String key = StatementTextNormalizer.factKey(statement.subject(), statement.predicate());
        if (selectedKeys.add(key)) {
            selected.add(statement);
        }
    }

    private static List<Statement> trimToMax(List<Statement> statements, int maxStatements) {
        if (statements.size() <= maxStatements) {
            return List.copyOf(statements);
        }
        return statements.stream()
                .sorted(Comparator.comparing(Statement::createdAt).reversed())
                .limit(maxStatements)
                .toList();
    }

    private static String normalizeTurnId(String turnId) {
        return turnId == null || turnId.isBlank() ? "unknown" : turnId.trim();
    }

    private record InstantHolder(java.time.Instant latest) {}
}
