package name.hergeth.jchat.debug;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import jakarta.inject.Inject;
import name.hergeth.jchat.ai.llm.BackgroundLlmExecutor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller("/api/debug")
public class DebugController {

    @Inject
    DebugTraceStore traceStore;

    @Inject
    DebugTraceService debugTraceService;

    @Inject
    BackgroundLlmExecutor backgroundLlmExecutor;

    @Get("/latest")
    public Optional<TurnDebugSnapshot> latest(
            @QueryValue(defaultValue = "") String conversationId,
            @QueryValue(defaultValue = "true") boolean chatOnly) {
        String id = conversationId.isBlank() ? null : conversationId;
        return traceStore.latest(id, chatOnly);
    }

    @Get("/traces")
    public List<TurnDebugSnapshot> traces(
            @QueryValue(defaultValue = "30") int limit,
            @QueryValue(defaultValue = "") String conversationId,
            @QueryValue(defaultValue = "true") boolean includeMeta) {
        String id = conversationId.isBlank() ? null : conversationId;
        return traceStore.recent(Math.min(limit, 100), id, includeMeta);
    }

    @Get("/conversation-ids")
    public List<String> conversationIds(@QueryValue(defaultValue = "100") int limit) {
        return traceStore.conversationIds(Math.min(limit, 100));
    }

    @Get("/trace/{id}")
    public Optional<TurnDebugSnapshot> trace(String id) {
        return traceStore.findById(id);
    }

    @Get("/knowledge-store")
    public Map<String, Object> knowledgeStore(@QueryValue(defaultValue = "default") String conversationId) {
        return Map.of(
                "conversationId", conversationId,
                "statements", debugTraceService.knowledgeStore(conversationId));
    }

    @Get("/pending-llm-tasks")
    public Map<String, Object> pendingLlmTasks() {
        return Map.of("pending", backgroundLlmExecutor.pendingCount());
    }
}
