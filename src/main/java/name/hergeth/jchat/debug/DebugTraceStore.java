package name.hergeth.jchat.debug;

import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class DebugTraceStore {

    private static final int MAX_TRACES = 100;

    private final CopyOnWriteArrayList<TurnDebugSnapshot> traces = new CopyOnWriteArrayList<>();

    public void add(TurnDebugSnapshot snapshot) {
        traces.add(0, snapshot);
        while (traces.size() > MAX_TRACES) {
            traces.remove(traces.size() - 1);
        }
    }

    public Optional<TurnDebugSnapshot> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return traces.stream()
                .filter(t -> id.equals(t.id()))
                .findFirst();
    }

    public Optional<TurnDebugSnapshot> latest(String conversationId, boolean chatOnly) {
        return filtered(conversationId, !chatOnly).stream().findFirst();
    }

    public List<TurnDebugSnapshot> recent(int limit, String conversationId, boolean includeMeta) {
        return filtered(conversationId, includeMeta).stream()
                .limit(limit)
                .toList();
    }

    private List<TurnDebugSnapshot> filtered(String conversationId, boolean includeMeta) {
        List<TurnDebugSnapshot> result = new ArrayList<>();
        for (TurnDebugSnapshot trace : traces) {
            if (conversationId != null && !conversationId.isBlank()
                    && !conversationId.equals(trace.conversationId())) {
                continue;
            }
            if (!includeMeta && !DebugRequestClassifier.isChat(trace.requestType())) {
                continue;
            }
            result.add(trace);
        }
        return result;
    }
}
