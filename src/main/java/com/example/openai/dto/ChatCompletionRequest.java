package com.example.openai.dto;

import java.util.List;

public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        Boolean stream
) {}
