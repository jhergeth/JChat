package name.hergeth.jchat.ai;

import name.hergeth.jchat.openai.dto.ChatMessage;

import java.util.ArrayList;
import java.util.List;

final class ConversationTurns {

    private ConversationTurns() {}

    static List<ChatMessage> lastTurns(List<ChatMessage> history, int maxTurns) {
        if (maxTurns <= 0 || history == null || history.isEmpty()) {
            return List.of();
        }

        List<ChatMessage> nonSystem = history.stream()
                .filter(m -> !"system".equals(m.role()))
                .toList();

        List<List<ChatMessage>> turns = new ArrayList<>();
        List<ChatMessage> current = new ArrayList<>();
        for (ChatMessage message : nonSystem) {
            if ("user".equals(message.role()) && !current.isEmpty()) {
                turns.add(List.copyOf(current));
                current.clear();
            }
            current.add(message);
        }
        if (!current.isEmpty()) {
            turns.add(List.copyOf(current));
        }

        int fromIndex = Math.max(0, turns.size() - maxTurns);
        List<ChatMessage> result = new ArrayList<>();
        for (int i = fromIndex; i < turns.size(); i++) {
            result.addAll(turns.get(i));
        }
        return result;
    }
}
