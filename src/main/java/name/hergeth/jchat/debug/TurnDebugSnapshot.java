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
        List<PromptLine> prompt,
        String llmResponse,
        String chatProvider,
        List<StatementView> knowledgeStore
) {}
