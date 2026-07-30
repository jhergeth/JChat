package name.hergeth.jchat.openai;

import name.hergeth.jchat.ai.ConversationIds;
import name.hergeth.jchat.ai.PromptBuilder;
import name.hergeth.jchat.ai.Retriever;
import name.hergeth.jchat.ai.SystemPromptProvider;
import name.hergeth.jchat.ai.llm.AiServiceFactory;
import name.hergeth.jchat.ai.llm.ChatModelRegistry;
import name.hergeth.jchat.ai.llm.TaskRouter;
import name.hergeth.jchat.openai.dto.ChatCompletionRequest;
import name.hergeth.jchat.openai.dto.ChatCompletionResponse;
import name.hergeth.jchat.openai.dto.ChatMessage;
import name.hergeth.jchat.openai.dto.Choice;
import name.hergeth.jchat.openai.dto.Usage;
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
    AiServiceFactory aiServiceFactory;

    @Inject
    ChatModelRegistry modelRegistry;

    @Inject
    TaskRouter taskRouter;

    @Inject
    SystemPromptProvider systemPromptProvider;

    @Inject
    PromptBuilder promptBuilder;

    @Inject
    Retriever retriever;

    @Post("/chat/completions")
    public ChatCompletionResponse chatCompletions(@Body ChatCompletionRequest request) {

        String conversationId = ConversationIds.resolve(request.conversationId());
        String lastUserMessage = lastUserMessage(request);

        List<name.hergeth.jchat.ai.model.Statement> retrievedStatements =
                retriever.retrieve(conversationId, lastUserMessage);

        List<ChatMessage> messages = promptBuilder.build(
                request.messages(), systemPromptProvider.get(), retrievedStatements);

        String chatProvider = modelRegistry.has(request.model())
                ? request.model()
                : taskRouter.providerFor("chat");

        String answer = aiServiceFactory.chat(chatProvider, messages);

        // Schritt 2: Turn turn = TurnFactory.fromExchange(...);
        //           extractor → normalizer → knowledgeStore

        ChatMessage responseMessage = new ChatMessage("assistant", answer);
        Choice choice = new Choice(0, responseMessage, "stop");

        return new ChatCompletionResponse(
                "chatcmpl-" + UUID.randomUUID(),
                "chat.completion",
                Instant.now().getEpochSecond(),
                chatProvider,
                List.of(choice),
                new Usage(0, 0, 0)
        );
    }

    @Get("/models")
    public Object models() {
        List<Map<String, String>> data = modelRegistry.names().stream()
                .map(id -> Map.of("id", id, "object", "model", "owned_by", "you"))
                .collect(Collectors.toList());

        return Map.of("object", "list", "data", data);
    }

    private String lastUserMessage(ChatCompletionRequest request) {
        if (request.messages() == null || request.messages().isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        for (int i = request.messages().size() - 1; i >= 0; i--) {
            ChatMessage msg = request.messages().get(i);
            if ("user".equals(msg.role())) {
                return msg.content();
            }
        }
        throw new IllegalArgumentException("no user message found");
    }
}
