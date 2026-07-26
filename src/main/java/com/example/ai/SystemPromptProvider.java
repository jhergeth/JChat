package com.example.ai;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class SystemPromptProvider {

    private final String systemPrompt;

    public SystemPromptProvider(@Value("${app.system-prompt-path}") String path) throws IOException {
        this.systemPrompt = Files.readString(Path.of(path));
    }

    public String get() {
        return systemPrompt;
    }
}
