package name.hergeth.jchat.ai.llm;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ChatModelRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ChatModelRegistry.class);

    private final Map<String, ChatLanguageModel> models = new HashMap<>();
    private final int defaultTimeoutSeconds;
    private final int defaultMaxRetries;

    public ChatModelRegistry(
            List<LlmProviderConfig> configs,
            @Value("${llm.default-timeout-seconds:120}") int defaultTimeoutSeconds,
            @Value("${llm.default-max-retries:0}") int defaultMaxRetries) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.defaultMaxRetries = defaultMaxRetries;
        for (LlmProviderConfig config : configs) {
            if (!isConfigured(config)) {
                LOG.warn("Skipping unconfigured provider: {}", config.getName());
                continue;
            }
            models.put(config.getName(), build(config));
        }
        if (models.isEmpty()) {
            throw new IllegalStateException("No LLM providers configured");
        }
    }

    private boolean isConfigured(LlmProviderConfig config) {
        return switch (config.getType()) {
            case "ollama" -> config.getBaseUrl() != null && !config.getBaseUrl().isBlank();
            case "anthropic", "openai" -> hasApiKey(config.getApiKey());
            default -> false;
        };
    }

    private boolean hasApiKey(String apiKey) {
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("${");
    }

    private ChatLanguageModel build(LlmProviderConfig config) {
        return switch (config.getType()) {
            case "anthropic" -> AnthropicChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .build();
            case "openai" -> {
                var builder = OpenAiChatModel.builder()
                        .apiKey(config.getApiKey())
                        .modelName(config.getModelName())
                        .timeout(Duration.ofSeconds(timeoutSeconds(config)))
                        .maxRetries(maxRetries(config));
                if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
                    builder.baseUrl(config.getBaseUrl());
                }
                yield builder.build();
            }
            case "ollama" -> {
                var builder = OllamaChatModel.builder()
                        .baseUrl(config.getBaseUrl())
                        .modelName(config.getModelName())
                        .timeout(Duration.ofSeconds(timeoutSeconds(config)));
                if (config.getNumCtx() != null) {
                    builder.numCtx(config.getNumCtx());
                }
                yield builder.build();
            }
            default -> throw new IllegalArgumentException(
                    "Unbekannter Provider-Typ: " + config.getType());
        };
    }

    public ChatLanguageModel get(String providerName) {
        ChatLanguageModel model = models.get(providerName);
        if (model == null) {
            throw new IllegalArgumentException("Kein Provider konfiguriert: " + providerName);
        }
        return model;
    }

    public boolean has(String providerName) {
        return models.containsKey(providerName);
    }

    public List<String> names() {
        return List.copyOf(models.keySet());
    }

    private int timeoutSeconds(LlmProviderConfig config) {
        return config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : defaultTimeoutSeconds;
    }

    private int maxRetries(LlmProviderConfig config) {
        return config.getMaxRetries() != null ? config.getMaxRetries() : defaultMaxRetries;
    }
}
