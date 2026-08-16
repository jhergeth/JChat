package name.hergeth.jchat.ai.search;

public record SearchDecision(boolean search, String query) {

    public static SearchDecision skip() {
        return new SearchDecision(false, "");
    }

    public static SearchDecision go(String query) {
        return new SearchDecision(true, query == null ? "" : query.trim());
    }
}
