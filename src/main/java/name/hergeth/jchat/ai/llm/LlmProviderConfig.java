package name.hergeth.jchat.ai.llm;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;

@EachProperty("llm.providers")
public class LlmProviderConfig {

    private final String name;
    private String type;
    private String apiKey;
    private String modelName;
    private String baseUrl;
    private Integer numCtx;
    private Integer timeoutSeconds;
    private Integer maxRetries;

    public LlmProviderConfig(@Parameter String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Integer getNumCtx() { return numCtx; }
    public void setNumCtx(Integer numCtx) { this.numCtx = numCtx; }
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
}
