package name.hergeth.jchat.ai.search;

import java.util.Locale;

final class SearchQueryNormalizer {

    private static final int MAX_QUERY_LENGTH = 80;

    private SearchQueryNormalizer() {}

    static String sanitize(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        String trimmed = query.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= MAX_QUERY_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_QUERY_LENGTH).trim();
    }
}
