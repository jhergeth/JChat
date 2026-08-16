package name.hergeth.jchat.scenario;

import java.util.List;

public record ScenarioDefinition(
        String name,
        String conversationId,
        String description,
        List<String> turns
) {
    public ScenarioDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("scenario name required");
        }
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = "scenario-" + name;
        }
        if (turns == null || turns.isEmpty()) {
            throw new IllegalArgumentException("scenario turns required: " + name);
        }
    }
}
