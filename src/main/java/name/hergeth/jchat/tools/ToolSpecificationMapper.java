package name.hergeth.jchat.tools;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class ToolSpecificationMapper {

    private ToolSpecificationMapper() {}

    static ToolSpecification toSpecification(String name, String description, JsonNode schema) {
        JsonNode root = schema == null || schema.isMissingNode() ? null : schema;
        String type = root != null && root.has("type") ? root.get("type").asText("object") : "object";
        Map<String, Map<String, Object>> properties = new HashMap<>();
        if (root != null && root.has("properties") && root.get("properties").isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = root.get("properties").fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                properties.put(field.getKey(), jsonObjectToMap(field.getValue()));
            }
        }
        List<String> required = new ArrayList<>();
        if (root != null && root.has("required") && root.get("required").isArray()) {
            root.get("required").forEach(node -> required.add(node.asText()));
        }
        ToolParameters parameters = ToolParameters.builder()
                .type(type)
                .properties(properties)
                .required(required)
                .build();
        return ToolSpecification.builder()
                .name(name)
                .description(description)
                .parameters(parameters)
                .build();
    }

    private static Map<String, Object> jsonObjectToMap(JsonNode node) {
        Map<String, Object> map = new HashMap<>();
        if (node == null || !node.isObject()) {
            return map;
        }
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                map.put(entry.getKey(), value.asText());
            } else if (value.isBoolean()) {
                map.put(entry.getKey(), value.asBoolean());
            } else if (value.isNumber()) {
                map.put(entry.getKey(), value.numberValue());
            } else if (value.isArray()) {
                List<String> items = new ArrayList<>();
                value.forEach(item -> items.add(item.asText()));
                map.put(entry.getKey(), items);
            } else if (value.isObject()) {
                map.put(entry.getKey(), jsonObjectToMap(value));
            }
        });
        return map;
    }
}
