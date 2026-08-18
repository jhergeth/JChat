package name.hergeth.jchat.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void exposesOnlyEnabledTools() throws Exception {
        JChatTool enabled = stubTool("alpha", true);
        JChatTool disabled = stubTool("beta", false);
        ToolRegistry registry = new ToolRegistry(List.of(enabled, disabled));

        assertTrue(registry.hasEnabledTools());
        assertEquals(1, registry.enabledToolSpecifications().size());
        assertEquals("alpha", registry.enabledToolSpecifications().get(0).name());
        assertTrue(registry.find("alpha").isPresent());
        assertTrue(registry.find("beta").isPresent());
        assertTrue(registry.combinedUsageHints().contains("alpha"));
    }

    @Test
    void reportsNoEnabledToolsWhenAllDisabled() throws Exception {
        ToolRegistry registry = new ToolRegistry(List.of(stubTool("x", false)));
        assertFalse(registry.hasEnabledTools());
        assertTrue(registry.enabledToolSpecifications().isEmpty());
    }

    private static JChatTool stubTool(String name, boolean enabled) throws Exception {
        JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"q":{"type":"string"}},"required":["q"]}
                """);
        return new JChatTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return name + " tool";
            }

            @Override
            public JsonNode parameterSchema() {
                return schema;
            }

            @Override
            public String usageHint() {
                return "- " + name + "(q): test";
            }

            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public ToolExecutionResult execute(JsonNode arguments, ToolContext context) {
                return ToolExecutionResult.ok("ok");
            }
        };
    }
}
