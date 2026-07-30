package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.openai.dto.ChatMessage;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class FullHistoryPromptBuilder implements PromptBuilder {

    @Override
    public List<ChatMessage> build(List<ChatMessage> history, String systemPrompt, List<Statement> retrievedStatements) {
        List<ChatMessage> messages = new ArrayList<>();

        String fullSystemPrompt = systemPrompt;
        if (!retrievedStatements.isEmpty()) {
            String knowledge = retrievedStatements.stream()
                    .map(Statement::formatForPrompt)
                    .collect(Collectors.joining("\n"));
            fullSystemPrompt = systemPrompt + "\n\nRelevantes Wissen:\n" + knowledge;
        }
        messages.add(new ChatMessage("system", fullSystemPrompt));

        history.stream()
                .filter(m -> !"system".equals(m.role()))
                .forEach(messages::add);

        return messages;
    }
}
