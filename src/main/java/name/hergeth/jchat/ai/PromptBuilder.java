package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.openai.dto.ChatMessage;

import java.util.List;

public interface PromptBuilder {
    List<ChatMessage> build(List<ChatMessage> history, String systemPrompt, List<Statement> retrievedStatements);
}
