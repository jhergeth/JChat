package name.hergeth.jchat.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        Boolean stream,
        @JsonProperty("conversation_id") String conversationId,
        Map<String, String> metadata
) {
    public ChatCompletionRequest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
