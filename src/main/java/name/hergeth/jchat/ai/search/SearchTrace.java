package name.hergeth.jchat.ai.search;

import java.util.List;

public record SearchTrace(
        boolean searched,
        String status,
        String detail,
        String query,
        int snippetCount,
        List<String> extractedTriples,
        List<String> storeFacts,
        List<SearchSnippet> snippets,
        String promptContext
) {
    public SearchTrace {
        extractedTriples = extractedTriples == null ? List.of() : List.copyOf(extractedTriples);
        storeFacts = storeFacts == null ? List.of() : List.copyOf(storeFacts);
        snippets = snippets == null ? List.of() : List.copyOf(snippets);
        promptContext = promptContext == null ? "" : promptContext;
    }

    public SearchTrace(
            boolean searched,
            String status,
            String detail,
            String query,
            int snippetCount,
            List<String> extractedTriples,
            List<SearchSnippet> snippets,
            String promptContext) {
        this(searched, status, detail, query, snippetCount, extractedTriples, List.of(), snippets, promptContext);
    }

    public SearchTrace(
            boolean searched,
            String status,
            String detail,
            String query,
            int snippetCount,
            List<String> extractedTriples,
            List<SearchSnippet> snippets) {
        this(searched, status, detail, query, snippetCount, extractedTriples, List.of(), snippets, "");
    }

    public SearchTrace(
            boolean searched,
            String status,
            String detail,
            String query,
            int snippetCount,
            List<String> extractedTriples) {
        this(searched, status, detail, query, snippetCount, extractedTriples, List.of(), List.of(), "");
    }

    public static SearchTrace disabled(String detail) {
        return new SearchTrace(false, "disabled", detail, "", 0, List.of(), List.of(), List.of(), "");
    }

    public static SearchTrace plannerSkip() {
        return new SearchTrace(false, "planner_skip", "LLM: keine Suche noetig", "", 0, List.of(), List.of(), List.of(), "");
    }

    public static SearchTrace error(String detail) {
        return new SearchTrace(false, "error", detail, "", 0, List.of(), List.of(), List.of(), "");
    }

    @Deprecated
    public static SearchTrace skipped() {
        return plannerSkip();
    }
}
