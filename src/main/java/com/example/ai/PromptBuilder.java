package com.example.ai;

import com.example.openai.dto.ChatMessage;

import java.util.List;

public interface PromptBuilder {
    List<ChatMessage> build(List<ChatMessage> history, String systemPrompt);
}
