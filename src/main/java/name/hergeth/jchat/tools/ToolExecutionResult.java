package name.hergeth.jchat.tools;

import name.hergeth.jchat.ai.search.SearchTrace;

public record ToolExecutionResult(
        String content,
        boolean error,
        String dataSource,
        SearchTrace searchTrace
) {
    public ToolExecutionResult {
        dataSource = dataSource == null ? "" : dataSource;
    }

    public static ToolExecutionResult ok(String content) {
        return ok(content, "", null);
    }

    public static ToolExecutionResult ok(String content, String dataSource, SearchTrace searchTrace) {
        return new ToolExecutionResult(content, false, dataSource, searchTrace);
    }

    public static ToolExecutionResult fail(String message) {
        return new ToolExecutionResult(message, true, "", null);
    }
}
