package name.hergeth.jchat.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import name.hergeth.jchat.ai.search.SearchOrchestrator;
import name.hergeth.jchat.ai.search.SearchTrace;

@Singleton
public class WebSearchTool implements JChatTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SearchOrchestrator searchOrchestrator;
    private final boolean enabled;

    public WebSearchTool(
            SearchOrchestrator searchOrchestrator,
            @Value("${app.tools.web-search.enabled:true}") boolean enabled) {
        this.searchOrchestrator = searchOrchestrator;
        this.enabled = enabled;
    }

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public String description() {
        return "Search the web (Wikipedia / configured providers) for current public facts. "
                + "Always performs an external search (ignores the knowledge store). "
                + "Use when the user asks about recent events, offices, governments, or facts not in context.";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode query = MAPPER.createObjectNode();
        query.put("type", "string");
        query.put("description", "Short, precise search query (max ~10 words)");

        ObjectNode properties = MAPPER.createObjectNode();
        properties.set("query", query);

        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", properties);
        schema.putArray("required").add("query");
        return schema;
    }

    @Override
    public String usageHint() {
        return "- web_search(query): Externe Websuche (Knowledge Store wird ignoriert). "
                + "Maximal ein Aufruf pro Antwort; präzise, kurze Query.";
    }

    @Override
    public boolean enabled() {
        return enabled && searchOrchestrator.isEnabled();
    }

    @Override
    public ToolExecutionResult execute(JsonNode arguments, ToolContext context) {
        String query = readQuery(arguments);
        if (query.isBlank()) {
            return ToolExecutionResult.fail("Parameter 'query' fehlt oder ist leer.");
        }

        String conversationId = context == null ? null : context.conversationId();
        SearchTrace trace = searchOrchestrator.searchWithQueryForTool(conversationId, query);
        if (!trace.searched()) {
            return ToolExecutionResult.fail("Websuche nicht verfügbar: " + trace.detail());
        }
        if (!"success".equals(trace.status())) {
            return ToolExecutionResult.fail("Websuche fehlgeschlagen (" + trace.status() + "): " + trace.detail());
        }
        String formatted = trace.promptContext();
        if (formatted.isBlank()) {
            return ToolExecutionResult.ok("Keine Treffer für: " + query, "web", trace);
        }
        return ToolExecutionResult.ok(formatted, "web", trace);
    }

    private static String readQuery(JsonNode arguments) {
        if (arguments == null || arguments.isMissingNode()) {
            return "";
        }
        JsonNode queryNode = arguments.get("query");
        return queryNode == null || queryNode.isNull() ? "" : queryNode.asText("").trim();
    }
}
