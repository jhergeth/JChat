package name.hergeth.jchat.debug;

import name.hergeth.jchat.openai.RequestClassifier;

public final class DebugRequestClassifier {

    private DebugRequestClassifier() {}

    public static String classify(String userInput) {
        return RequestClassifier.classify(userInput);
    }

    public static boolean isChat(String requestType) {
        return RequestClassifier.isChat(requestType);
    }
}
