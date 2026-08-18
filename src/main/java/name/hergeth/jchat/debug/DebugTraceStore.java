package name.hergeth.jchat.debug;

import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    public boolean replace(TurnDebugSnapshot snapshot) {
        for (int i = 0; i < traces.size(); i++) {
            if (snapshot.id().equals(traces.get(i).id())) {
                traces.set(i, snapshot);
                return true;
            }
        }
        return false;
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

    /** Distinct conversation IDs, most recently seen first (max {@value #MAX_TRACES}). */
    public List<String> conversationIds(int limit) {
        int cap = Math.min(Math.max(limit, 1), MAX_TRACES);
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (TurnDebugSnapshot trace : traces) {
            ids.add(trace.conversationId());
            if (ids.size() >= cap) {
                break;
            }
        }
        return List.copyOf(ids);
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
