package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EntityFactExpander {

    private EntityFactExpander() {}

    public static List<Statement> expand(EntityIndex index, List<String> focusEntityKeys, int maxFacts) {
        if (index.isEmpty() || focusEntityKeys == null || focusEntityKeys.isEmpty() || maxFacts <= 0) {
            return List.of();
        }
        Map<String, Statement> dedup = new LinkedHashMap<>();
        for (String entityKey : focusEntityKeys) {
            addFacts(dedup, index.factsFor(entityKey));
            for (Statement statement : index.factsFor(entityKey)) {
                String objectKey = StatementTextNormalizer.entityKey(statement.object());
                if (index.hasEntity(objectKey)) {
                    addFacts(dedup, index.factsFor(objectKey));
                }
            }
            if (dedup.size() >= maxFacts) {
                break;
            }
        }
        return dedup.values().stream().limit(maxFacts).toList();
    }

    private static void addFacts(Map<String, Statement> dedup, List<Statement> statements) {
        for (Statement statement : statements) {
            dedup.putIfAbsent(StatementTextNormalizer.statementDedupKey(statement), statement);
        }
    }
}
