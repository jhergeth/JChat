package name.hergeth.jchat.tools;

final class ToolResultTruncator {

    private static final int MAX_LLM_CHARS = 900;

    private ToolResultTruncator() {}

    static String forLlm(String content) {
        if (content == null || content.isBlank()) {
            return content == null ? "" : content;
        }
        if (content.length() <= MAX_LLM_CHARS) {
            return content;
        }
        return content.substring(0, MAX_LLM_CHARS).trim() + "\n[gekürzt]";
    }
}
