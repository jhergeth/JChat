package name.hergeth.jchat.tools;

import name.hergeth.jchat.ai.search.SearchTrace;

public record ToolExecutionRecord(
        int step,
        String toolName,
        String argumentsJson,
        String result,
        boolean error,
        long durationMs,
        String dataSource,
        String searchQuery,
        SearchTrace searchTrace
) {
    public ToolExecutionRecord(
            int step,
            String toolName,
            String argumentsJson,
            String result,
            boolean error,
            long durationMs) {
        this(step, toolName, argumentsJson, result, error, durationMs, "", "", null);
    }

    public boolean hasWebSearchTrace() {
        return "web_search".equals(toolName)
                && searchTrace != null
                && searchTrace.searched()
                && "success".equals(searchTrace.status())
                && !searchTrace.snippets().isEmpty();
    }
}
