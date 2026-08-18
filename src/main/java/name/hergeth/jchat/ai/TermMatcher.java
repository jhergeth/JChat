package name.hergeth.jchat.ai;

import java.util.Locale;
import java.util.regex.Pattern;

final class TermMatcher {

    private TermMatcher() {}

    static boolean matches(String text, String term) {
        if (text == null || text.isBlank() || term == null || term.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        if (term.length() <= 3) {
            return Pattern.compile("\\b" + Pattern.quote(term) + "\\b")
                    .matcher(normalized)
                    .find();
        }
        return normalized.contains(term);
    }
}
