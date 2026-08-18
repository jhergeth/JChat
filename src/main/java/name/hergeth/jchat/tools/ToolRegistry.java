package name.hergeth.jchat.tools;

import dev.langchain4j.agent.tool.ToolSpecification;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class ToolRegistry {

    private final Map<String, JChatTool> toolsByName;

    public ToolRegistry(List<JChatTool> tools) {
        Map<String, JChatTool> map = new LinkedHashMap<>();
        for (JChatTool tool : tools) {
            map.put(tool.name(), tool);
        }
        this.toolsByName = Map.copyOf(map);
    }

    public boolean hasEnabledTools() {
        return toolsByName.values().stream().anyMatch(JChatTool::enabled);
    }

    public List<ToolSpecification> enabledToolSpecifications() {
        return toolsByName.values().stream()
                .filter(JChatTool::enabled)
                .map(JChatTool::toToolSpecification)
                .toList();
    }

    public Optional<JChatTool> find(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    public String combinedUsageHints() {
        String hints = toolsByName.values().stream()
                .filter(JChatTool::enabled)
                .map(JChatTool::usageHint)
                .filter(h -> h != null && !h.isBlank())
                .collect(Collectors.joining("\n"));
        if (hints.isBlank()) {
            return "";
        }
        return "\n\nVerfügbare Tools:\n" + hints;
    }
}
