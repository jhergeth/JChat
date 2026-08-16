package name.hergeth.jchat.ai.llm;

import name.hergeth.jchat.openai.dto.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFastPromptTest {

    @Test
    void prependsDirectiveToSystemPromptAndSuffixesUserMessage() {
        List<ChatMessage> wrapped = JsonFastPrompt.wrap(List.of(
                new ChatMessage("system", "Task prompt"),
                new ChatMessage("user", "Input text")));

        assertTrue(wrapped.get(0).content().startsWith("Direktmodus — json-fast"));
        assertTrue(wrapped.get(0).content().contains("Task prompt"));
        assertEquals("Input text /no_think", wrapped.get(1).content());
    }

    @Test
    void insertsDirectiveWhenSystemPromptMissing() {
        List<ChatMessage> wrapped = JsonFastPrompt.wrap(List.of(
                new ChatMessage("user", "Only user")));

        assertEquals("system", wrapped.get(0).role());
        assertTrue(wrapped.get(0).content().startsWith("Direktmodus — json-fast"));
        assertEquals("Only user /no_think", wrapped.get(1).content());
    }
}
