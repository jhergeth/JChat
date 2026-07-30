package name.hergeth.jchat.ai.llm;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;

@EachProperty("llm.tasks")
public class TaskMapping {

    private final String task;
    private String provider;

    public TaskMapping(@Parameter String task) {
        this.task = task;
    }

    public String getTask() { return task; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
