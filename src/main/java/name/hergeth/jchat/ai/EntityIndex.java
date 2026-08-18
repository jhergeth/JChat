package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EntityIndex {

    private final Map<String, String> keyToLabel;
    private final Map<String, List<Statement>> factsByKey;
    private final Map<String, String> aliasToKey;
    private final List<String> labelsByLength;

    private EntityIndex(
            Map<String, String> keyToLabel,
            Map<String, List<Statement>> factsByKey,
            Map<String, String> aliasToKey) {
        this.keyToLabel = keyToLabel;
        this.factsByKey = factsByKey;
        this.aliasToKey = aliasToKey;
        this.labelsByLength = keyToLabel.values().stream()
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    public static EntityIndex from(List<Statement> statements) {
        if (statements == null || statements.isEmpty()) {
            return empty();
        }
        Map<String, String> keyToLabel = new HashMap<>();
        Map<String, List<Statement>> factsByKey = new HashMap<>();
        Map<String, String> aliasToKey = new HashMap<>();

        for (Statement statement : statements) {
            if (StatementTextNormalizer.isInvalidSubject(statement.subject())) {
                continue;
            }
            String subjectKey = StatementTextNormalizer.entityKey(statement.subject());
            keyToLabel.putIfAbsent(subjectKey, statement.subject().trim());
            factsByKey.computeIfAbsent(subjectKey, key -> new ArrayList<>()).add(statement);
            registerAlias(aliasToKey, subjectKey, statement.subject());

            String object = statement.object();
            if (object != null && !object.isBlank() && !StatementTextNormalizer.isVagueObject(object)) {
                String objectKey = StatementTextNormalizer.entityKey(object);
                if (looksLikeNamedEntity(object)) {
                    keyToLabel.putIfAbsent(objectKey, object.trim());
                    factsByKey.computeIfAbsent(objectKey, key -> new ArrayList<>()).add(statement);
                    registerAlias(aliasToKey, objectKey, object);
                }
                if (looksLikeRoleAlias(statement.predicate(), object)) {
                    registerAlias(aliasToKey, subjectKey, object);
                }
            }
        }
        return new EntityIndex(keyToLabel, factsByKey, aliasToKey);
    }

    public static EntityIndex empty() {
        return new EntityIndex(Map.of(), Map.of(), Map.of());
    }

    public boolean isEmpty() {
        return keyToLabel.isEmpty();
    }

    public boolean hasEntity(String entityKey) {
        return entityKey != null && factsByKey.containsKey(entityKey);
    }

    public String labelFor(String entityKey) {
        return keyToLabel.getOrDefault(entityKey, entityKey);
    }

    public List<Statement> factsFor(String entityKey) {
        return factsByKey.getOrDefault(entityKey, List.of());
    }

    public Optional<String> entityKeyForAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizeAlias(alias);
        if (aliasToKey.containsKey(normalized)) {
            return Optional.of(aliasToKey.get(normalized));
        }
        return Optional.empty();
    }

    public Optional<String> entityKeyForRoleMention(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        List<Map.Entry<String, String>> aliases = aliasToKey.entrySet().stream()
                .filter(entry -> entry.getKey().length() >= 4)
                .sorted(Comparator.comparingInt(entry -> -entry.getKey().length()))
                .toList();
        for (Map.Entry<String, String> entry : aliases) {
            if (TermMatcher.matches(lower, entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public List<String> entityKeysMentionedIn(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String label : labelsByLength) {
            if (TermMatcher.matches(text, label)) {
                keys.add(StatementTextNormalizer.entityKey(label));
            }
        }
        Optional<String> roleKey = entityKeyForRoleMention(text);
        roleKey.ifPresent(keys::add);
        return List.copyOf(keys);
    }

    private static void registerAlias(Map<String, String> aliasToKey, String entityKey, String alias) {
        String normalized = normalizeAlias(alias);
        if (normalized.length() >= 3) {
            aliasToKey.putIfAbsent(normalized, entityKey);
        }
        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 4) {
                aliasToKey.putIfAbsent(token, entityKey);
            }
        }
    }

    private static String normalizeAlias(String alias) {
        return alias.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9äöüß\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean looksLikeNamedEntity(String object) {
        String trimmed = object.trim();
        if (trimmed.length() < 3) {
            return false;
        }
        if (Character.isDigit(trimmed.charAt(0))) {
            return false;
        }
        return trimmed.chars().anyMatch(Character::isUpperCase)
                || trimmed.split("\\s+").length >= 2;
    }

    private static boolean looksLikeRoleAlias(String predicate, String object) {
        if (object == null || object.isBlank()) {
            return false;
        }
        String normalizedObject = object.toLowerCase(Locale.ROOT);
        if (normalizedObject.length() < 4) {
            return false;
        }
        String normalizedPredicate = predicate == null ? "" : predicate.toLowerCase(Locale.ROOT);
        return normalizedPredicate.contains("position")
                || normalizedPredicate.contains("amt")
                || normalizedPredicate.contains("rolle")
                || normalizedPredicate.contains("ist")
                || normalizedPredicate.contains("praesident")
                || normalizedPredicate.contains("präsident")
                || normalizedPredicate.contains("kanzler")
                || normalizedPredicate.contains("minister")
                || normalizedPredicate.contains("pressesprecher");
    }
}
