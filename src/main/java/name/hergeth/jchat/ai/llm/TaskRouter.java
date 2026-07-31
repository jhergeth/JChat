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
        String provider = taskToProvider.get(task);
        if (provider == null) {
            provider = taskToProvider.get("default");
        }
        if (provider == null) {
            throw new IllegalStateException(
                    "Kein Provider fuer Aufgabe '" + task + "' konfiguriert (llm.tasks." + task + ".provider)");
        }
        return provider;
    }

    public boolean hasTask(String task) {
        return taskToProvider.containsKey(task);
    }
}
