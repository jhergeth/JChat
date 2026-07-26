package com.example.ai;

import com.example.openai.dto.ChatMessage;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class FullHistoryPromptBuilder implements PromptBuilder {

    @Override
    public List<ChatMessage> build(List<ChatMessage> history, String systemPrompt) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt));
        messages.addAll(history);
        return messages;
    }
}
