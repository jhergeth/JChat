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
        List<String> storeFacts,
        List<SearchSnippetView> snippets,
        String promptContext,
        String contextSource
) {
    public static SearchTraceView from(SearchTrace trace) {
        if (trace == null) {
            return new SearchTraceView(false, "none", "", "", 0, List.of(), List.of(), List.of(), "", "");
        }
        return new SearchTraceView(
                trace.searched(),
                trace.status(),
                trace.detail(),
                trace.query(),
                trace.snippetCount(),
                trace.extractedTriples(),
                trace.storeFacts(),
                trace.snippets().stream()
                        .map(SearchTraceView::toSnippetView)
                        .toList(),
                trace.promptContext(),
                resolveContextSource(trace));
    }

    private static String resolveContextSource(SearchTrace trace) {
        if (!trace.searched()) {
            return "";
        }
        if ("knowledge_store".equals(trace.status())) {
            return "store";
        }
        if ("success".equals(trace.status()) || "no_snippets".equals(trace.status())) {
            return "web";
        }
        return "";
    }

    private static SearchSnippetView toSnippetView(SearchSnippet snippet) {
        return new SearchSnippetView(snippet.title(), snippet.url(), snippet.snippet());
    }
}
