package name.hergeth.jchat.ai;

public final class ConversationIds {

    public static final String DEFAULT = "default";

    private ConversationIds() {}

    public static String resolve(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return DEFAULT;
        }
        return conversationId;
    }
}
