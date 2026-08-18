package name.hergeth.jchat.scenario;

import name.hergeth.jchat.ai.PredicateNormalizer;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Soft-matches expected scenario triples against extracted knowledge-store statements.
 */
public final class TripleMatcher {

    private static final int MIN_SUBJECT_PREFIX_LENGTH = 3;

    private static final Map<String, Set<String>> PREDICATE_EQUIVALENCE = Map.ofEntries(
            Map.entry("hobby", Set.of("hobby", "hat_hobby", "hat_hobbies")),
            Map.entry("lieblingsessen", Set.of("lieblingsessen", "lieblings_essen", "magessen", "mag_essen")),
            Map.entry("faehrt", Set.of("faehrt", "fahrt", "fahrt_auto", "hat_auto", "faehrt_auto")),
            Map.entry("ist_hauptstadt_von", Set.of(
                    "ist_hauptstadt_von", "hauptstadt_von", "ist_hauptstadt", "hauptstadt")));

    private TripleMatcher() {}

    public static boolean matches(StatementSnapshot actual, TripleExpectation expected) {
        return matchesSubject(actual.subject(), expected.subject())
                && matchesPredicate(actual.predicate(), expected.predicate())
                && matchesObject(actual.object(), expected.object());
    }

    static boolean matchesSubject(String actualSubject, String expectedSubject) {
        String actual = normalizeText(actualSubject);
        String expected = normalizeText(expectedSubject);
        if (actual.isEmpty() || expected.isEmpty()) {
            return false;
        }
        if (actual.equals(expected)) {
            return true;
        }
        String actualCompact = compact(actualSubject);
        String expectedCompact = compact(expectedSubject);
        if (!actualCompact.isEmpty() && actualCompact.equals(expectedCompact)) {
            return true;
        }
        if (expectedCompact.length() >= MIN_SUBJECT_PREFIX_LENGTH
                && actualCompact.startsWith(expectedCompact)) {
            return true;
        }
        if (actualCompact.length() >= MIN_SUBJECT_PREFIX_LENGTH
                && expectedCompact.startsWith(actualCompact)) {
            return true;
        }
        if (expected.length() >= MIN_SUBJECT_PREFIX_LENGTH
                && (actual.startsWith(expected + " ") || actual.equals(expected))) {
            return true;
        }
        if (actual.length() >= MIN_SUBJECT_PREFIX_LENGTH
                && (expected.startsWith(actual + " ") || expected.equals(actual))) {
            return true;
        }
        return false;
    }

    static boolean matchesPredicate(String actualPredicate, String expectedPredicate) {
        String actual = normalizePredicate(actualPredicate);
        String expected = normalizePredicate(expectedPredicate);
        if (actual.isEmpty() || expected.isEmpty()) {
            return false;
        }
        if (actual.equals(expected)) {
            return true;
        }
        if (actual.contains(expected) || expected.contains(actual)) {
            return true;
        }
        return sameEquivalenceGroup(actual, expected);
    }

    static boolean matchesObject(String actualObject, String expectedObject) {
        String actualCompact = compact(actualObject);
        String expectedCompact = compact(expectedObject);
        if (actualCompact.isEmpty() || expectedCompact.isEmpty()) {
            return false;
        }
        return actualCompact.contains(expectedCompact) || expectedCompact.contains(actualCompact);
    }

    private static String normalizePredicate(String predicate) {
        return foldUmlauts(PredicateNormalizer.normalize(predicate));
    }

    private static boolean sameEquivalenceGroup(String actual, String expected) {
        for (Set<String> group : PREDICATE_EQUIVALENCE.values()) {
            if (group.contains(actual) && group.contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return foldUmlauts(value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim());
    }

    private static String compact(String value) {
        return normalizeText(value).replaceAll("[^a-z0-9]", "");
    }

    private static String foldUmlauts(String value) {
        return value
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss");
    }
}
