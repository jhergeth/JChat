package name.hergeth.jchat.openai.dto;

public record Choice(int index, ChatMessage message, String finish_reason) {}
