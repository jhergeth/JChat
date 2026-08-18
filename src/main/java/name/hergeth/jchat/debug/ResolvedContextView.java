package name.hergeth.jchat.debug;

import name.hergeth.jchat.ai.context.ResolvedContext;

import java.util.List;

public record ResolvedContextView(
        String resolvedQuery,
        List<String> focusEntities,
        int entityBundleSize,
        String resolutionNotes
) {
    public static ResolvedContextView from(ResolvedContext context, int entityBundleSize) {
        if (context == null) {
            return empty();
        }
        return new ResolvedContextView(
                context.queryForScoring(),
                context.focusEntityLabels(),
                entityBundleSize,
                context.resolutionNotes());
    }

    public static ResolvedContextView empty() {
        return new ResolvedContextView("", List.of(), 0, "");
    }
}
