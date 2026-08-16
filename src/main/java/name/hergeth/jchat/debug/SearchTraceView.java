package name.hergeth.jchat.debug;

import name.hergeth.jchat.ai.search.SearchSnippet;
import name.hergeth.jchat.ai.search.SearchTrace;

import java.util.List;

public record SearchTraceView(
        boolean searched,
        String status,
        String detail,
        String query,
        int snippetCount,
        List<String> extractedTriples,
        List<SearchSnippetView> snippets,
        String promptContext
) {
    public static SearchTraceView from(SearchTrace trace) {
        if (trace == null) {
            return new SearchTraceView(false, "none", "", "", 0, List.of(), List.of(), "");
        }
        return new SearchTraceView(
                trace.searched(),
                trace.status(),
                trace.detail(),
                trace.query(),
                trace.snippetCount(),
                trace.extractedTriples(),
                trace.snippets().stream()
                        .map(SearchTraceView::toSnippetView)
                        .toList(),
                trace.promptContext());
    }

    private static SearchSnippetView toSnippetView(SearchSnippet snippet) {
        return new SearchSnippetView(snippet.title(), snippet.url(), snippet.snippet());
    }
}
