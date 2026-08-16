package name.hergeth.jchat.ai.llm;

public class LlmResponseException extends RuntimeException {

    private final String provider;

    public LlmResponseException(String provider, String message) {
        super(message);
        this.provider = provider;
    }

    public LlmResponseException(String provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public String provider() {
        return provider;
    }
}
