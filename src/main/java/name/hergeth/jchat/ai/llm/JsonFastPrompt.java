package name.hergeth.jchat.ai.llm;

import name.hergeth.jchat.ai.PromptLoader;
import name.hergeth.jchat.openai.dto.ChatMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps prompts for {@code json-fast} models so they skip reasoning / thinking blocks.
 */
public final class JsonFastPrompt {

    private static final String DIRECTIVE_PATH = "json-fast-directive.txt";
    private static final String USER_SUFFIX = " /no_think";

    private static volatile String directive;

    private JsonFastPrompt() {}

    public static List<ChatMessage> wrap(List<ChatMessage> messages) {
        ensureDirectiveLoaded();
        List<ChatMessage> wrapped = new ArrayList<>();
        boolean hasSystem = false;
        for (ChatMessage message : messages) {
            if ("system".equals(message.role())) {
                hasSystem = true;
                wrapped.add(new ChatMessage("system", directive + "\n\n" + message.content()));
            } else {
                wrapped.add(message);
            }
        }
        if (!hasSystem) {
            wrapped.add(0, new ChatMessage("system", directive));
        }
        suffixLastUserMessage(wrapped);
        return List.copyOf(wrapped);
    }

    private static void suffixLastUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if ("user".equals(message.role())) {
                String content = message.content() == null ? "" : message.content().stripTrailing();
                messages.set(i, new ChatMessage("user", content + USER_SUFFIX));
                return;
            }
        }
    }

    private static void ensureDirectiveLoaded() {
        if (directive != null) {
            return;
        }
        synchronized (JsonFastPrompt.class) {
            if (directive != null) {
                return;
            }
            try {
                directive = PromptLoader.load(DIRECTIVE_PATH).trim();
            } catch (IOException e) {
                throw new IllegalStateException("Missing " + DIRECTIVE_PATH, e);
            }
        }
    }
}
