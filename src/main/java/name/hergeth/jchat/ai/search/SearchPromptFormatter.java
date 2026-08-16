package name.hergeth.jchat.ai.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SearchPromptFormatter {

    private SearchPromptFormatter() {}

    static String format(SearchTrace trace) {
        if (trace == null || !trace.searched() || !"success".equals(trace.status())) {
            return "";
        }

        String query = trace.query() == null ? "" : trace.query();
        List<SearchSnippet> snippets = SnippetRelevanceRanker.rank(trace.snippets(), query);

        StringBuilder sb = new StringBuilder();
        sb.append("\n\nAktuelle Recherche (diesen Fakten vor Trainingswissen vertrauen):\n");
        Set<String> mentioned = new HashSet<>();

        for (SearchSnippet snippet : snippets) {
            WikiOfficeHolderExtractor.extractOfficeHolder(snippet).ifPresent(holder -> {
                String line = formatOfficeHolder(holder);
                if (!mentioned.contains(line.toLowerCase())) {
                    sb.append("- ").append(line);
                    if (snippet.title() != null && !snippet.title().isBlank()) {
                        sb.append(" (Quelle: Wikipedia, ").append(snippet.title()).append(')');
                    }
                    sb.append('\n');
                    mentioned.add(line.toLowerCase());
                }
            });
        }

        if (!trace.extractedTriples().isEmpty()) {
            for (String triple : trace.extractedTriples()) {
                String line = naturalizeTriple(triple);
                if (!mentioned.contains(line.toLowerCase())) {
                    sb.append("- ").append(line).append('\n');
                    mentioned.add(line.toLowerCase());
                }
            }
        }

        if (mentioned.isEmpty()) {
            for (SearchSnippet snippet : snippets) {
                if (SearchSnippetQuality.isSubstantive(snippet)) {
                    String excerpt = SnippetRelevanceRanker.bestSentences(snippet.snippet(), query, 2);
                    sb.append("- Auszug (").append(snippet.title()).append("): ")
                            .append(excerpt).append('\n');
                }
            }
        }

        return mentioned.isEmpty() && sb.toString().endsWith("vertrauen):\n")
                ? ""
                : sb.toString().trim();
    }

    private static String formatOfficeHolder(WikiOfficeHolderExtractor.OfficeHolderFact holder) {
        if (holder.jurisdiction() == null || holder.jurisdiction().isBlank()) {
            if (holder.office() == null || holder.office().isBlank()) {
                return holder.person() + " ist derzeit amtierende Person.";
            }
            return holder.person() + " ist derzeit " + holder.office() + ".";
        }
        if (holder.office() == null || holder.office().isBlank()) {
            return holder.person() + " ist derzeit Amtsinhaber der " + holder.jurisdiction() + ".";
        }
        return holder.person() + " ist derzeit " + holder.office() + " der " + holder.jurisdiction() + ".";
    }

    private static String naturalizeTriple(String triple) {
        String[] parts = triple.split("\\|");
        if (parts.length != 3) {
            return triple;
        }
        String subject = parts[0].trim();
        String predicate = parts[1].trim();
        String object = parts[2].trim();
        if (predicate.contains("amtsinhaber") || predicate.contains("bundeskanzler")
                || predicate.contains("präsident") || predicate.contains("prasident")
                || predicate.contains("president")) {
            if (object.isBlank()) {
                return subject + " ist Amtsinhaber.";
            }
            return subject + " ist Amtsinhaber der " + object + ".";
        }
        return subject + " " + predicate.replace('_', ' ') + " " + object + ".";
    }
}
