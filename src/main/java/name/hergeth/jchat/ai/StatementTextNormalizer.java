package name.hergeth.jchat.ai;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StatementTextNormalizer {

    private static final Set<String> INVALID_SUBJECTS = Set.of(
            "user", "assistant", "tool", "system");

    private static final Set<String> VAGUE_OBJECTS = Set.of(
            "stadt", "unternehmen", "firma", "company", "ort", "land");

    private static final Map<String, String> PREDICATE_ALIASES = Map.ofEntries(
            Map.entry("arbeite_bei", "arbeitet_bei"),
            Map.entry("arbeitetbei", "arbeitet_bei"),
            Map.entry("arbeitet_im", "arbeitet_bei"),
            Map.entry("arbeitet_im_unternehmen", "arbeitet_bei"),
            Map.entry("wohntin", "wohnt_in"),
            Map.entry("lebtin", "wohnt_in"),
            Map.entry("lieblingsfarbe_ist", "lieblingsfarbe"),
            Map.entry("lieblingsfarbe_verwendet", "lieblingsfarbe"),
            Map.entry("nutzt_plattform", "nutzt"),
            Map.entry("nutzt_system", "nutzt"),
            Map.entry("laeuftauf", "laeuft_auf"),
            Map.entry("runson", "laeuft_auf"),
            Map.entry("runs_on", "laeuft_auf"),
            Map.entry("uses", "nutzt"),
            Map.entry("use", "nutzt"),
            Map.entry("hasgpus", "hat_gpu"),
            Map.entry("has_gpus", "hat_gpu"),
            Map.entry("has_gpu", "hat_gpu"),
            Map.entry("hatgpu", "hat_gpu"),
            Map.entry("nutztgpu", "nutzt_gpu"),
            Map.entry("usedfor", "nutzt_gpu"),
            Map.entry("used_for", "nutzt_gpu"),
            Map.entry("verwendet_auf_am5", "laeuft_auf"),
            Map.entry("verwendet_auf", "laeuft_auf"),
            Map.entry("projektname", "name"),
            Map.entry("hat_neuer_name", "name"),
            Map.entry("ist_name", "name"),
            Map.entry("heisst", "name"));

    private static final Pattern MARKDOWN = Pattern.compile("[«»\"`*_]+");
    private static final Pattern CAMEL_CASE = Pattern.compile("([a-z])([A-Z])");
    private static final Pattern GLUED_PREP = Pattern.compile(
            "^([a-zäöüß]+?)(in|bei|auf|mit|an|von|fuer|fur|im|am|zum|zur)$");
    private static final int MAX_PREDICATE_LENGTH = 32;

    private StatementTextNormalizer() {}

    static String normalizeSubject(String value) {
        String cleaned = clean(value);
        cleaned = stripRolePrefix(cleaned);
        return cleaned;
    }

    static String normalizePredicate(String value) {
        String cleaned = clean(value);
        cleaned = CAMEL_CASE.matcher(cleaned).replaceAll("$1_$2");
        cleaned = cleaned.toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replaceAll("_+", "_")
                .replaceAll("[^a-z0-9_äöüß]", "");
        cleaned = splitGluedPreposition(cleaned);
        return PREDICATE_ALIASES.getOrDefault(cleaned, cleaned);
    }

    static String normalizeObject(String value) {
        return clean(value);
    }

    static String factKey(String subject, String predicate) {
        return normalizeSubject(subject).toLowerCase(Locale.ROOT) + "|" + normalizePredicate(predicate);
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

    private static String splitGluedPreposition(String predicate) {
        Matcher matcher = GLUED_PREP.matcher(predicate);
        if (matcher.matches() && matcher.group(1).length() >= 3) {
            return matcher.group(1) + "_" + matcher.group(2);
        }
        return predicate;
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
