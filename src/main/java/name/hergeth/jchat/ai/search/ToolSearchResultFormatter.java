package name.hergeth.jchat.ai.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Compact search formatting for tool results — keeps agent context small.
 */
final class ToolSearchResultFormatter {

    private static final Pattern WIKI_SECTION = Pattern.compile("\n=+[^=\n]+=+\n?");
    private static final int MAX_SNIPPETS = 3;
    private static final int MAX_SENTENCES_PER_SNIPPET = 1;
    private static final int MAX_TOTAL_CHARS = 1200;

    private ToolSearchResultFormatter() {}

    static String format(SearchTrace trace) {
        if (trace == null || !trace.searched() || !"success".equals(trace.status())) {
            return "";
        }

        String query = trace.query() == null ? "" : trace.query();
        List<SearchSnippet> snippets = SnippetRelevanceRanker.rank(trace.snippets(), query);

        StringBuilder sb = new StringBuilder();
        sb.append("Suchergebnisse für \"").append(query).append("\":\n");
        Set<String> mentioned = new HashSet<>();
        int snippetsUsed = 0;

        for (SearchSnippet snippet : snippets) {
            if (snippetsUsed >= MAX_SNIPPETS) {
                break;
            }
            var holder = WikiOfficeHolderExtractor.extractOfficeHolder(snippet);
            if (holder.isPresent()) {
                String line = compactOfficeHolder(holder.get());
                if (mentioned.add(line.toLowerCase())) {
                    sb.append("- ").append(line).append('\n');
                    snippetsUsed++;
                }
                continue;
            }
            if (!SearchSnippetQuality.isSubstantive(snippet)) {
                continue;
            }
            String excerpt = SnippetRelevanceRanker.bestSentences(
                    stripWikiSections(snippet.snippet()), query, MAX_SENTENCES_PER_SNIPPET);
            excerpt = truncateExcerpt(excerpt, 220);
            if (excerpt.isBlank()) {
                continue;
            }
            String title = snippet.title() == null || snippet.title().isBlank() ? "Treffer" : snippet.title();
            String line = excerpt + " (" + title + ")";
            if (mentioned.add(line.toLowerCase())) {
                sb.append("- ").append(line).append('\n');
                snippetsUsed++;
            }
        }

        if (snippetsUsed == 0) {
            return "";
        }
        return truncateTotal(sb.toString().trim());
    }

    private static String compactOfficeHolder(WikiOfficeHolderExtractor.OfficeHolderFact holder) {
        if (holder.office() != null && !holder.office().isBlank()) {
            return holder.person() + " — " + holder.office();
        }
        return holder.person();
    }

    private static String stripWikiSections(String text) {
        if (text == null) {
            return "";
        }
        return WIKI_SECTION.matcher(text).replaceAll(" ").trim();
    }

    private static String truncateExcerpt(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxChars).trim() + "...";
    }

    private static String truncateTotal(String text) {
        if (text.length() <= MAX_TOTAL_CHARS) {
            return text;
        }
        return text.substring(0, MAX_TOTAL_CHARS).trim() + "...";
    }
}
