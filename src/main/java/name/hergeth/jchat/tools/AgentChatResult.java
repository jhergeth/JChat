package name.hergeth.jchat.tools;

import java.util.List;

public record AgentChatResult(
        String text,
        List<ToolExecutionRecord> toolCalls,
        int stepsUsed
) {}
