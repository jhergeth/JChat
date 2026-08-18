package name.hergeth.jchat.debug;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.List;

public record TurnDebugSnapshot(
        String id,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        Instant timestamp,
        String conversationId,
        String requestType,
        String userInput,
        List<String> retrievedContext,
        AmbientContextView ambientContext,
        List<PromptLine> prompt,
        String llmResponse,
        String chatProvider,
        SearchTraceView searchTrace,
        List<StatementView> knowledgeStore,
        List<ToolCallView> toolCalls
) {
    public TurnDebugSnapshot {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }
}
