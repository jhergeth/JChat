package name.hergeth.jchat.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        Boolean stream,
        @JsonProperty("conversation_id") String conversationId
) {}
