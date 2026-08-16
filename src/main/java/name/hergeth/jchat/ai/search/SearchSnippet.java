package name.hergeth.jchat.ai.search;

public record SearchSnippet(String title, String url, String snippet) {

    private static final int MAX_SNIPPET_CHARS = 2500;

    public SearchSnippet {
        title = title == null ? "" : title;
        url = url == null ? "" : url;
        snippet = truncate(snippet, MAX_SNIPPET_CHARS);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    String formatForExtraction() {
        return "Titel: " + title + "\nURL: " + url + "\nSnippet: " + snippet;
    }
}
