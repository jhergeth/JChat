package com.example.openai;

import com.example.ai.ChatAssistant;
import com.example.ai.KnowledgeStore;
import com.example.ai.PromptBuilder;
import com.example.ai.Retriever;
import com.example.ai.StatementExtractor;
import com.example.ai.SystemPromptProvider;
import com.example.openai.dto.ChatCompletionRequest;
import com.example.openai.dto.ChatCompletionResponse;
import com.example.openai.dto.ChatMessage;
import com.example.openai.dto.Choice;
import com.example.openai.dto.Usage;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller("/v1")
public class ChatCompletionsController {

    @Inject
    ChatAssistant chatAssistant;

    @Inject
    SystemPromptProvider systemPromptProvider;

    @Inject
    PromptBuilder promptBuilder;

    @Inject
    Retriever retriever;

    @Inject
    KnowledgeStore knowledgeStore;

    @Inject
    StatementExtractor statementExtractor;

    @Post("/chat/completions")
    public ChatCompletionResponse chatCompletions(@Body ChatCompletionRequest request) {

        String lastUserMessage = lastUserMessage(request);

        // aktuell wirkungslos (Retriever liefert []), aber schon verdrahtet
        List<String> context = retriever.retrieve(lastUserMessage);

        // baut die vollständige Historie inkl. System-Prompt zusammen
        List<ChatMessage> fullPrompt = promptBuilder.build(
                request.messages(), systemPromptProvider.get());

        // an das LLM senden
        String answer = chatAssistant.chat(renderAsSingleString(fullPrompt));

        // extrahierte Aussagen sammeln (aktuell leer, da Extractor noop)
        statementExtractor.extract(answer).forEach(knowledgeStore::add);

        ChatMessage responseMessage = new ChatMessage("assistant", answer);
        Choice choice = new Choice(0, responseMessage, "stop");

        return new ChatCompletionResponse(
                "chatcmpl-" + UUID.randomUUID(),
                "chat.completion",
                Instant.now().getEpochSecond(),
                request.model(),
                List.of(choice),
                new Usage(0, 0, 0)
        );
    }

    @Get("/models")
    public Object models() {
        return Map.of(
                "object", "list",
                "data", List.of(
                        Map.of(
                                "id", "mein-eigener-assistant",
                                "object", "model",
                                "owned_by", "you"
                        )
                )
        );
    }

    private String lastUserMessage(ChatCompletionRequest request) {
        return request.messages().get(request.messages().size() - 1).content();
    }

    private String renderAsSingleString(List<ChatMessage> messages) {
        return messages.stream()
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n"));
    }
}
