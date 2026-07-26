package com.example.openai.dto;

public record Choice(int index, ChatMessage message, String finish_reason) {}
