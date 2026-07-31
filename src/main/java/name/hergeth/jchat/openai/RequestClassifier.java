package name.hergeth.jchat.openai;

public final class RequestClassifier {

    private RequestClassifier() {}

    public static String classify(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return "chat";
        }
        if (!userInput.contains("### Task:")) {
            return "chat";
        }
        String lower = userInput.toLowerCase();
        if (lower.contains("follow_up") || lower.contains("follow-up")) {
            return "follow_up";
        }
        return "meta";
    }

    public static boolean isChat(String requestType) {
        return "chat".equals(requestType);
    }

    public static boolean isMeta(String requestType) {
        return "follow_up".equals(requestType) || "meta".equals(requestType);
    }
}
