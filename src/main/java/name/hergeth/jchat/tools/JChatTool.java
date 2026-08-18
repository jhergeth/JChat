package name.hergeth.jchat.tools;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.ToolSpecification;

public interface JChatTool {

    String name();

    String description();

    /** JSON-Schema properties for tool parameters (OpenAI function calling). */
    JsonNode parameterSchema();

    /** Optional hint appended to the system prompt when this tool is registered. */
    default String usageHint() {
        return "";
    }

    boolean enabled();

    ToolExecutionResult execute(JsonNode arguments, ToolContext context);

    default ToolSpecification toToolSpecification() {
        return ToolSpecificationMapper.toSpecification(name(), description(), parameterSchema());
    }
}
