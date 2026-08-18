package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class StatementTextNormalizer {

    private static final Set<String> INVALID_SUBJECTS = Set.of(
            "user", "assistant", "tool", "system");

    private static final Set<String> VAGUE_OBJECTS = Set.of(
            "stadt", "unternehmen", "firma", "company", "ort", "land");

    private static final Pattern MARKDOWN = Pattern.compile("[«»\"`_*]+");
    private static final int MAX_PREDICATE_LENGTH = 32;

    private StatementTextNormalizer() {}

    static String normalizeSubject(String value) {
        String cleaned = clean(value);
        cleaned = stripRolePrefix(cleaned);
        return cleaned;
    }

    static String normalizePredicate(String value) {
        return PredicateNormalizer.normalize(value);
    }

    static String normalizeObject(String value) {
        return clean(value);
    }

    static String factKey(String subject, String predicate) {
        return entityKey(subject) + "|" + normalizePredicate(predicate);
    }

    static String entityKey(String label) {
        return compactSubjectKey(label == null ? "" : label);
    }

    static String statementDedupKey(Statement statement) {
        return factKey(statement.subject(), statement.predicate()) + "|"
                + normalizeObject(statement.object()).toLowerCase(Locale.ROOT);
    }

    private static String compactSubjectKey(String subject) {
        String normalized = normalizeSubject(subject).toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9äöüß]", "");
    }

    static boolean isInvalidSubject(String subject) {
        String normalized = stripRolePrefix(subject).toLowerCase(Locale.ROOT).trim();
        return INVALID_SUBJECTS.contains(normalized) || normalized.startsWith("assistant:");
    }

    static boolean isVagueObject(String object) {
        return VAGUE_OBJECTS.contains(object.toLowerCase(Locale.ROOT).trim());
    }

    static boolean isPredicateTooLong(String predicate) {
        return predicate.length() > MAX_PREDICATE_LENGTH;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return MARKDOWN.matcher(value.trim()).replaceAll("").trim();
    }

    private static String stripRolePrefix(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("user:")) {
            return value.substring(5).trim();
        }
        if (lower.startsWith("assistant:")) {
            return value.substring(10).trim();
        }
        return value;
    }
}
