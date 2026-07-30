package name.hergeth.jchat.ai.llm;

import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class TaskRouter {

    private final Map<String, String> taskToProvider = new HashMap<>();

    public TaskRouter(List<TaskMapping> mappings) {
        for (TaskMapping mapping : mappings) {
            taskToProvider.put(mapping.getTask(), mapping.getProvider());
        }
    }

    public String providerFor(String task) {
        return taskToProvider.getOrDefault(task, taskToProvider.get("default"));
    }
}
