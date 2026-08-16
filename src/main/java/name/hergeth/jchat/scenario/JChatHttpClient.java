package name.hergeth.jchat.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import name.hergeth.jchat.openai.dto.ChatCompletionRequest;
import name.hergeth.jchat.openai.dto.ChatCompletionResponse;
import name.hergeth.jchat.openai.dto.ChatMessage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JChatHttpClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final URI chatCompletionsUri;
    private final URI knowledgeStoreUri;
    private final URI pendingLlmTasksUri;

    public JChatHttpClient(String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.chatCompletionsUri = URI.create(normalized + "/v1/chat/completions");
        this.knowledgeStoreUri = URI.create(normalized + "/api/debug/knowledge-store");
        this.pendingLlmTasksUri = URI.create(normalized + "/api/debug/pending-llm-tasks");
    }

    public String chat(String conversationId, List<ChatMessage> messages) throws IOException, InterruptedException {
        return chat(null, conversationId, messages);
    }

    public String chat(String model, String conversationId, List<ChatMessage> messages) throws IOException, InterruptedException {
        ChatCompletionRequest request = new ChatCompletionRequest(model, messages, false, conversationId, Map.of());
        String body = MAPPER.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder(chatCompletionsUri)
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Chat failed HTTP " + response.statusCode() + ": " + response.body());
        }

        ChatCompletionResponse completion = MAPPER.readValue(response.body(), ChatCompletionResponse.class);
        if (completion.choices() == null || completion.choices().isEmpty()) {
            throw new IOException("Chat response has no choices");
        }
        return completion.choices().get(0).message().content();
    }

    public void waitForBackgroundLlmTasks(Duration timeout) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (pendingLlmTaskCount() == 0) {
                return;
            }
            Thread.sleep(200);
        }
        throw new IOException("Timed out after " + timeout.toSeconds()
                + "s waiting for background LLM tasks (pending=" + pendingLlmTaskCount() + ")");
    }

    private int pendingLlmTaskCount() throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder(pendingLlmTasksUri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Pending LLM tasks failed HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode root = MAPPER.readTree(response.body());
        JsonNode pending = root.get("pending");
        return pending == null || pending.isNull() ? 0 : pending.asInt();
    }

    public List<StatementSnapshot> knowledgeStore(String conversationId) throws IOException, InterruptedException {
        URI uri = URI.create(knowledgeStoreUri + "?conversationId=" + conversationId);
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Knowledge store failed HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = MAPPER.readTree(response.body());
        JsonNode statements = root.get("statements");
        List<StatementSnapshot> result = new ArrayList<>();
        if (statements == null || !statements.isArray()) {
            return result;
        }
        for (JsonNode node : statements) {
            result.add(new StatementSnapshot(
                    text(node, "subject"),
                    text(node, "predicate"),
                    text(node, "object")));
        }
        return result;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }
}
