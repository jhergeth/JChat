package name.hergeth.jchat.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class PromptLoader {

    private PromptLoader() {}

    public static String load(String path) throws IOException {
        try (InputStream resource = PromptLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (resource != null) {
                return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        throw new IOException("Prompt not found: " + path);
    }
}
