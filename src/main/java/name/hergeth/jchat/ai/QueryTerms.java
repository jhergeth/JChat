package name.hergeth.jchat.ai;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class QueryTerms {

    private static final Set<String> STOP_WORDS = Set.of(
            "ich", "du", "der", "die", "das", "ein", "eine", "und", "oder", "ist", "sind",
            "was", "wie", "wo", "wer", "wann", "warum", "welche", "welcher", "welches",
            "mein", "meine", "dein", "deine", "nicht", "auch", "noch", "schon", "bei",
            "mit", "auf", "fuer", "fur", "von", "aus", "dem", "den", "des", "mir", "dir",
            "the", "and", "for", "with", "what", "how", "where", "who", "which", "is", "are");

    private QueryTerms() {}

    static Set<String> from(String query) {
        if (query == null || query.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^a-z0-9äöüß]+"))
                .filter(term -> term.length() >= 2)
                .filter(term -> !STOP_WORDS.contains(term))
                .collect(Collectors.toSet());
    }
}
