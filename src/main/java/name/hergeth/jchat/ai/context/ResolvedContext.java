package name.hergeth.jchat.ai.context;

import java.util.List;

public record ResolvedContext(
        String userMessage,
        String resolvedQuery,
        List<String> focusEntityKeys,
        List<String> focusEntityLabels,
        String resolutionNotes
) {
    public ResolvedContext {
        focusEntityKeys = focusEntityKeys == null ? List.of() : List.copyOf(focusEntityKeys);
        focusEntityLabels = focusEntityLabels == null ? List.of() : List.copyOf(focusEntityLabels);
        resolutionNotes = resolutionNotes == null ? "" : resolutionNotes;
    }

    public static ResolvedContext plain(String userMessage) {
        String message = userMessage == null ? "" : userMessage.trim();
        return new ResolvedContext(message, message, List.of(), List.of(), "");
    }

    public String queryForScoring() {
        if (resolvedQuery != null && !resolvedQuery.isBlank()) {
            return resolvedQuery.trim();
        }
        return userMessage == null ? "" : userMessage.trim();
    }

    public boolean hasFocusEntities() {
        return !focusEntityKeys.isEmpty();
    }

    public boolean hasPronounResolution() {
        return resolutionNotes.startsWith("pronoun->");
    }
}
