package com.example.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import jakarta.inject.Singleton;

@Singleton
public interface ChatAssistant {

    @SystemMessage("""
        Du bist ein hilfreicher Assistent.
        Antworte präzise und auf Deutsch, sofern nicht anders verlangt.
        """)
    String chat(@UserMessage String userMessage);
}
