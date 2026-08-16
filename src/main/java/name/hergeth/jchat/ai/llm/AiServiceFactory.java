package name.hergeth.jchat.ai.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.Response;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Singleton
public class AiServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AiServiceFactory.class);

    private final ChatModelRegistry registry;

    public AiServiceFactory(ChatModelRegistry registry) {
        this.registry = registry;
    }

    public String chat(String providerName, List<name.hergeth.jchat.openai.dto.ChatMessage> messages) {
        List<name.hergeth.jchat.openai.dto.ChatMessage> requestMessages = messages;
        if (registry.disableReasoning(providerName)) {
            requestMessages = JsonFastPrompt.wrap(messages);
        }
        List<ChatMessage> lcMessages = requestMessages.stream()
                .map(ChatMessageMapper::toLangChain4j)
                .toList();
        try {
            Response<AiMessage> response = registry.get(providerName).generate(lcMessages);
            return extractText(providerName, response);
        } catch (LlmResponseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LlmResponseException(providerName, "LLM call failed: " + e.getMessage(), e);
        }
    }

    private static String extractText(String providerName, Response<AiMessage> response) {
        if (response == null || response.content() == null) {
            throw new LlmResponseException(providerName, "LLM returned no assistant message");
        }
        String text;
        try {
            text = response.content().text();
        } catch (RuntimeException e) {
            throw new LlmResponseException(providerName,
                    "LLM assistant message has no readable text", e);
        }
        if (text == null || text.isBlank()) {
            LOG.warn("LLM provider '{}' returned empty text (finishReason={})",
                    providerName, response.finishReason());
            throw new LlmResponseException(providerName, "LLM returned empty text");
        }
        return text;
    }
}
