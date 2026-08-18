package name.hergeth.jchat.tools;

import java.util.List;
import java.util.stream.Collectors;

final class AgentFallbackAnswer {

    private AgentFallbackAnswer() {}

    static String fromToolResults(List<ToolExecutionRecord> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }
        String combined = records.stream()
                .filter(record -> !record.error())
                .map(ToolExecutionRecord::result)
                .filter(result -> result != null && !result.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining("\n"));
        if (combined.isBlank()) {
            return null;
        }
        combined = ToolResultTruncator.forLlm(combined);
        return "Laut Recherche: " + combined.replace('\n', ' ').trim();
    }
}
