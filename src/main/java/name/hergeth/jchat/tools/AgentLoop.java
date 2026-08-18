package name.hergeth.jchat.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import name.hergeth.jchat.ai.llm.ChatMessageMapper;
import name.hergeth.jchat.ai.llm.ChatModelRegistry;
import name.hergeth.jchat.ai.llm.LlmResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Singleton
public class AgentLoop {

    private static final Logger LOG = LoggerFactory.getLogger(AgentLoop.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final java.util.function.Function<String, ChatLanguageModel> modelLookup;
    private final ToolRegistry toolRegistry;
    private final int maxSteps;
    private final int maxToolCalls;
    private final int maxToolCallsPerStep;

    public AgentLoop(
            ChatModelRegistry modelRegistry,
            ToolRegistry toolRegistry,
            @Value("${app.agent.max-steps:5}") int maxSteps,
            @Value("${app.agent.max-tool-calls:3}") int maxToolCalls,
            @Value("${app.agent.max-tool-calls-per-step:1}") int maxToolCallsPerStep) {
        this(modelRegistry::get, toolRegistry, maxSteps, maxToolCalls, maxToolCallsPerStep);
    }

    AgentLoop(
            java.util.function.Function<String, ChatLanguageModel> modelLookup,
            ToolRegistry toolRegistry,
            int maxSteps,
            int maxToolCalls,
            int maxToolCallsPerStep) {
        this.modelLookup = modelLookup;
        this.toolRegistry = toolRegistry;
        this.maxSteps = Math.max(1, maxSteps);
        this.maxToolCalls = Math.max(1, maxToolCalls);
        this.maxToolCallsPerStep = Math.max(1, maxToolCallsPerStep);
    }

    public AgentChatResult run(
            String providerName,
            List<name.hergeth.jchat.openai.dto.ChatMessage> messages,
            ToolContext context) {
        List<ToolSpecification> toolSpecs = toolRegistry.enabledToolSpecifications();
        if (toolSpecs.isEmpty()) {
            throw new IllegalStateException("Agent loop started without enabled tools");
        }

        ChatLanguageModel model = modelLookup.apply(providerName);
        List<ChatMessage> lcMessages = new ArrayList<>();
        for (name.hergeth.jchat.openai.dto.ChatMessage message : messages) {
            lcMessages.add(ChatMessageMapper.toLangChain4j(message));
        }

        List<ToolExecutionRecord> records = new ArrayList<>();
        int stepsUsed = 0;
        int toolCallsUsed = 0;

        for (int step = 0; step < maxSteps; step++) {
            stepsUsed = step + 1;
            AiMessage aiMessage;
            try {
                Response<AiMessage> response = model.generate(lcMessages, toolSpecs);
                aiMessage = response.content();
            } catch (RuntimeException e) {
                if (isTimeout(e)) {
                    return fallbackOrThrow(providerName, records, stepsUsed, e);
                }
                throw new LlmResponseException(providerName, e.getMessage(), e);
            }

            if (aiMessage == null) {
                throw new LlmResponseException(providerName, "LLM returned no assistant message");
            }

            lcMessages.add(aiMessage);

            if (!aiMessage.hasToolExecutionRequests()) {
                String text = aiMessage.text();
                if (text == null || text.isBlank()) {
                    return fallbackOrThrow(providerName, records, stepsUsed,
                            new LlmResponseException(providerName, "LLM returned empty text"));
                }
                return new AgentChatResult(text.trim(), List.copyOf(records), stepsUsed);
            }

            List<ToolExecutionRequest> requests = aiMessage.toolExecutionRequests();
            int executedThisStep = 0;
            for (ToolExecutionRequest request : requests) {
                ToolExecutionRecord record;
                if (toolCallsUsed >= maxToolCalls) {
                    record = limitRecord(step + 1, request, "Tool-Limit für diesen Turn erreicht.");
                } else if (executedThisStep >= maxToolCallsPerStep) {
                    record = limitRecord(step + 1, request,
                            "Parallele Tool-Aufrufe begrenzt — nutze vorhandene Suchergebnisse.");
                } else {
                    record = executeTool(step + 1, request, context);
                    toolCallsUsed++;
                    executedThisStep++;
                }
                records.add(record);
                lcMessages.add(ToolExecutionResultMessage.toolExecutionResultMessage(
                        request,
                        ToolResultTruncator.forLlm(record.result())));
            }

            LOG.debug("Agent step {} completed with {} tool call(s)", step + 1, requests.size());
        }

        return fallbackOrThrow(providerName, records, stepsUsed,
                new LlmResponseException(providerName,
                        "Agent loop exceeded max steps (" + maxSteps + ") without final answer"));
    }

    private AgentChatResult fallbackOrThrow(
            String providerName,
            List<ToolExecutionRecord> records,
            int stepsUsed,
            RuntimeException cause) {
        String fallback = AgentFallbackAnswer.fromToolResults(records);
        if (fallback != null) {
            LOG.warn("Agent fallback for {} after {} step(s): {}",
                    providerName, stepsUsed, cause.getMessage());
            return new AgentChatResult(fallback, List.copyOf(records), stepsUsed);
        }
        if (cause instanceof LlmResponseException llm) {
            throw llm;
        }
        throw new LlmResponseException(providerName, cause.getMessage(), cause);
    }

    private static ToolExecutionRecord limitRecord(int step, ToolExecutionRequest request, String message) {
        String argsJson = request.arguments() == null ? "{}" : request.arguments();
        return new ToolExecutionRecord(step, request.name(), argsJson, message, true, 0);
    }

    private static boolean isTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("timeout") || lower.contains("timed out") || lower.contains("504")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private ToolExecutionRecord executeTool(int step, ToolExecutionRequest request, ToolContext context) {
        long start = System.currentTimeMillis();
        String toolName = request.name();
        String argsJson = request.arguments() == null ? "{}" : request.arguments();

        JChatTool tool = toolRegistry.find(toolName)
                .orElse(null);
        if (tool == null) {
            String msg = "Unbekanntes Tool: " + toolName;
            return new ToolExecutionRecord(step, toolName, argsJson, msg, true, elapsed(start));
        }
        if (!tool.enabled()) {
            String msg = "Tool deaktiviert: " + toolName;
            return new ToolExecutionRecord(step, toolName, argsJson, msg, true, elapsed(start));
        }

        try {
            JsonNode args = MAPPER.readTree(argsJson);
            ToolExecutionResult result = tool.execute(args, context);
            String query = "web_search".equals(toolName) ? readQueryFromArgs(args) : "";
            return new ToolExecutionRecord(
                    step,
                    toolName,
                    argsJson,
                    result.content(),
                    result.error(),
                    elapsed(start),
                    result.dataSource(),
                    query,
                    result.searchTrace());
        } catch (Exception e) {
            LOG.warn("Tool {} failed: {}", toolName, e.getMessage());
            return new ToolExecutionRecord(
                    step,
                    toolName,
                    argsJson,
                    "Tool-Fehler: " + e.getMessage(),
                    true,
                    elapsed(start));
        }
    }

    private static long elapsed(long start) {
        return Math.max(0, System.currentTimeMillis() - start);
    }

    private static String readQueryFromArgs(JsonNode arguments) {
        if (arguments == null || arguments.isMissingNode()) {
            return "";
        }
        JsonNode queryNode = arguments.get("query");
        return queryNode == null || queryNode.isNull() ? "" : queryNode.asText("").trim();
    }
}
