package name.hergeth.jchat.debug;

public record ToolCallView(
        int step,
        String toolName,
        String argumentsJson,
        String result,
        boolean error,
        long durationMs,
        String dataSource,
        String searchQuery
) {}
