package name.hergeth.jchat.openai;

import name.hergeth.jchat.ai.ConversationIds;
import name.hergeth.jchat.ai.PromptBuilder;
import name.hergeth.jchat.ai.Retriever;
import name.hergeth.jchat.ai.SystemPromptProvider;
import name.hergeth.jchat.ai.TurnFactory;
import name.hergeth.jchat.ai.TurnProcessor;
import name.hergeth.jchat.ai.context.AmbientContext;
import name.hergeth.jchat.ai.context.SessionContextHints;
import name.hergeth.jchat.ai.context.SessionContextResolver;
import name.hergeth.jchat.ai.llm.AiServiceFactory;
import name.hergeth.jchat.ai.llm.ChatModelRegistry;
import name.hergeth.jchat.ai.llm.TaskRouter;
import name.hergeth.jchat.ai.llm.LlmResponseException;
import name.hergeth.jchat.ai.search.SearchOrchestrator;
import name.hergeth.jchat.ai.search.SearchPostProcessor;
import name.hergeth.jchat.ai.search.SearchTrace;
import name.hergeth.jchat.debug.DebugTraceService;
import name.hergeth.jchat.openai.dto.ChatCompletionRequest;
import name.hergeth.jchat.openai.dto.ChatCompletionResponse;
import name.hergeth.jchat.openai.dto.ChatMessage;
import name.hergeth.jchat.openai.dto.Choice;
import name.hergeth.jchat.openai.dto.Usage;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.exceptions.HttpStatusException;
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

    @Inject
    SearchOrchestrator searchOrchestrator;

    @Inject
    SearchPostProcessor searchPostProcessor;

    @Inject
    SessionContextResolver sessionContextResolver;

    @Post("/chat/completions")
    public ChatCompletionResponse chatCompletions(
            @Body ChatCompletionRequest request,
            @Header(value = "Accept-Language", defaultValue = "") String acceptLanguage,
            @Header(value = "X-Timezone", defaultValue = "") String timezoneHeader) {

        SessionContextHints contextHints = SessionContextHints.fromHeadersAndMetadata(
                acceptLanguage, timezoneHeader, request.metadata());
        AmbientContext ambientContext = sessionContextResolver.resolve(contextHints);

        String conversationId = ConversationIds.resolve(request.conversationId());
        String lastUserMessage = lastUserMessage(request);
        String requestType = RequestClassifier.classify(lastUserMessage);
        boolean isChat = RequestClassifier.isChat(requestType);

        SearchTrace searchTrace = SearchTrace.disabled("kein Chat-Request");
        if (isChat) {
            searchTrace = searchOrchestrator.maybeSearch(
                    conversationId, lastUserMessage, request.messages(), ambientContext);
        }

        List<name.hergeth.jchat.ai.model.Statement> retrievedStatements = isChat
                ? retriever.retrieve(conversationId, lastUserMessage)
                : List.of();

        List<String> retrievedContext = retrievedStatements.stream()
                .map(name.hergeth.jchat.ai.model.Statement::formatForPrompt)
                .toList();

        List<ChatMessage> messages = isChat
                ? promptBuilder.build(
                        request.messages(),
                        systemPromptProvider.get(),
                        retrievedStatements,
                        searchTrace.promptContext(),
                        ambientContext)
                : MetaRequestMessages.passthrough(request.messages());

        String provider = resolveProvider(request, isChat);

        if (!isChat) {
            LOG.debug("Meta-request ({}) — skipping knowledge store and extraction", requestType);
        }

        LOG.info("Chat completion for conversation {} via {} (type={})", conversationId, provider, requestType);
        String answer;
        try {
            answer = aiServiceFactory.chat(provider, messages);
        } catch (LlmResponseException e) {
            LOG.error("LLM returned no usable text for conversation {} via {}: {}",
                    conversationId, provider, e.getMessage());
            throw new HttpStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }

        if (isChat) {
            turnProcessor.scheduleProcess(
                    TurnFactory.fromExchange(conversationId, request.messages(), answer));
        }

        String debugTraceId = debugTraceService.record(
                conversationId, requestType, lastUserMessage, retrievedContext, ambientContext,
                messages, answer, provider, searchTrace);

        if (isChat) {
            searchPostProcessor.scheduleAfterAnswer(
                    conversationId, lastUserMessage, answer, provider, searchTrace, debugTraceId);
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
        if (request.model() != null && modelRegistry.has(request.model())) {
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
