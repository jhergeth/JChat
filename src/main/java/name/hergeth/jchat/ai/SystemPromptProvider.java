package name.hergeth.jchat.ai;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class SystemPromptProvider {

    private final String systemPrompt;

    public SystemPromptProvider(@Value("${app.system-prompt-path}") String path) throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(path)) {
            if (resource != null) {
                this.systemPrompt = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
                return;
            }
        }
        Path filePath = Path.of(path);
        if (Files.exists(filePath)) {
            this.systemPrompt = Files.readString(filePath);
            return;
        }
        throw new IOException("System prompt not found: " + path);
    }

    public String get() {
        return systemPrompt;
    }
}
