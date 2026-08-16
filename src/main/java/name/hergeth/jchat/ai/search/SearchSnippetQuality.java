package name.hergeth.jchat.ai.search;

import java.util.List;

final class SearchSnippetQuality {

    private static final int MIN_SNIPPET_CHARS = 40;

    private SearchSnippetQuality() {}

    static boolean hasSubstantiveContent(List<SearchSnippet> snippets) {
        return snippets.stream().anyMatch(SearchSnippetQuality::isSubstantive);
    }

    static boolean isSubstantive(SearchSnippet snippet) {
        if (snippet == null) {
            return false;
        }
        String text = snippet.snippet();
        return text != null && text.trim().length() >= MIN_SNIPPET_CHARS;
    }

    static boolean isUsableLink(SearchSnippet snippet) {
        if (snippet == null) {
            return false;
        }
        String url = snippet.url();
        if (url == null || url.isBlank() || !url.startsWith("http")) {
            return false;
        }
        String title = snippet.title();
        return title != null && !title.isBlank();
    }

    static boolean hasUsableLinks(List<SearchSnippet> snippets) {
        return snippets.stream().anyMatch(SearchSnippetQuality::isUsableLink);
    }
}
