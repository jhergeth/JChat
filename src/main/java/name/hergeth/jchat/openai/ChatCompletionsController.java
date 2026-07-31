package name.hergeth.jchat.openai;

import name.hergeth.jchat.ai.ConversationIds;
import name.hergeth.jchat.ai.PromptBuilder;
import name.hergeth.jchat.ai.Retriever;
import name.hergeth.jchat.ai.SystemPromptProvider;
import name.hergeth.jchat.ai.TurnFactory;
import name.hergeth.jchat.ai.TurnProcessor;
import name.hergeth.jchat.ai.llm.AiServiceFactory;
import name.hergeth.jchat.ai.llm.ChatModelRegistry;
import name.hergeth.jchat.ai.llm.TaskRouter;
import name.hergeth.jchat.debug.DebugTraceService;
import name.hergeth.jchat.openai.dto.ChatCompletionRequest;
import name.hergeth.jchat.openai.dto.ChatCompletionResponse;
import name.hergeth.jchat.openai.dto.ChatMessage;
import name.hergeth.jchat.openai.dto.Choice;
import name.hergeth.jchat.openai.dto.Usage;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller("/v1")
public class ChatCompletionsController {

    private static final Logger LOG = LoggerFactory.getLogger(ChatCompletionsController.class);

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

    @Inject
    TurnProcessor turnProcessor;

    @Inject
    DebugTraceService debugTraceService;

    @Value("${app.record-meta-in-debug:false}")
    boolean recordMetaInDebug;

    @Post("/chat/completions")
    public ChatCompletionResponse chatCompletions(@Body ChatCompletionRequest request) {

        String conversationId = ConversationIds.resolve(request.conversationId());
        String lastUserMessage = lastUserMessage(request);
        String requestType = RequestClassifier.classify(lastUserMessage);
        boolean isChat = RequestClassifier.isChat(requestType);

        List<name.hergeth.jchat.ai.model.Statement> retrievedStatements = isChat
                ? retriever.retrieve(conversationId, lastUserMessage)
                : List.of();

        List<String> retrievedContext = retrievedStatements.stream()
                .map(name.hergeth.jchat.ai.model.Statement::formatForPrompt)
                .toList();

        List<ChatMessage> messages = isChat
                ? promptBuilder.build(request.messages(), systemPromptProvider.get(), retrievedStatements)
                : MetaRequestMessages.passthrough(request.messages());

        String provider = resolveProvider(request, isChat);

        if (!isChat) {
            LOG.debug("Meta-request ({}) — skipping knowledge store and extraction", requestType);
        }

        String answer = aiServiceFactory.chat(provider, messages);

        if (isChat) {
            try {
                turnProcessor.process(TurnFactory.fromExchange(conversationId, request.messages(), answer));
            } catch (Exception e) {
                LOG.warn("Statement extraction failed for conversation {}", conversationId, e);
            }
        }

        if (isChat || recordMetaInDebug) {
            debugTraceService.record(
                    conversationId, lastUserMessage, retrievedContext, messages, answer, provider);
        }

        ChatMessage responseMessage = new ChatMessage("assistant", answer);
        Choice choice = new Choice(0, responseMessage, "stop");

        return new ChatCompletionResponse(
                "chatcmpl-" + UUID.randomUUID(),
                "chat.completion",
                Instant.now().getEpochSecond(),
                provider,
                List.of(choice),
                new Usage(0, 0, 0)
        );
    }

    private String resolveProvider(ChatCompletionRequest request, boolean isChat) {
        if (!isChat) {
            if (taskRouter.hasTask("meta")) {
                return taskRouter.providerFor("meta");
            }
            if (modelRegistry.has("ollama-extract")) {
                return "ollama-extract";
            }
        }
        if (modelRegistry.has(request.model())) {
            return request.model();
        }
        return taskRouter.providerFor("chat");
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
