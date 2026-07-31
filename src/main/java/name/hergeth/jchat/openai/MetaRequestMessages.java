package name.hergeth.jchat.openai;

import name.hergeth.jchat.openai.dto.ChatMessage;

import java.util.List;

final class MetaRequestMessages {

    private MetaRequestMessages() {}

    static List<ChatMessage> passthrough(List<ChatMessage> messages) {
        return messages.stream()
                .filter(m -> !"system".equals(m.role()))
                .toList();
    }
}
