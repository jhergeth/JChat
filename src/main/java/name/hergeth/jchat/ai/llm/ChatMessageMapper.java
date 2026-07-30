package name.hergeth.jchat.ai.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

final class ChatMessageMapper {

    private ChatMessageMapper() {}

    static ChatMessage toLangChain4j(name.hergeth.jchat.openai.dto.ChatMessage message) {
        return switch (message.role()) {
            case "system" -> SystemMessage.from(message.content());
            case "assistant" -> AiMessage.from(message.content());
            default -> UserMessage.from(message.content());
        };
    }
}
