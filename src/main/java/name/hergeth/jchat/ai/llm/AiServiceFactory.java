package name.hergeth.jchat.ai.llm;

import dev.langchain4j.data.message.ChatMessage;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class AiServiceFactory {

    private final ChatModelRegistry registry;

    public AiServiceFactory(ChatModelRegistry registry) {
        this.registry = registry;
    }

    public String chat(String providerName, List<name.hergeth.jchat.openai.dto.ChatMessage> messages) {
        List<ChatMessage> lcMessages = messages.stream()
                .map(ChatMessageMapper::toLangChain4j)
                .toList();
        return registry.get(providerName).generate(lcMessages).content().text();
    }
}
