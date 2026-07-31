package name.hergeth.jchat.ai;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Value;
import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.openai.dto.ChatMessage;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Replaces(FullHistoryPromptBuilder.class)
public class ContextWindowPromptBuilder implements PromptBuilder {

    private final int recentTurns;

    public ContextWindowPromptBuilder(@Value("${app.context.recent-turns:3}") int recentTurns) {
        this.recentTurns = recentTurns;
    }

    @Override
    public List<ChatMessage> build(List<ChatMessage> history, String systemPrompt, List<Statement> retrievedStatements) {
        List<ChatMessage> messages = new ArrayList<>();

        String fullSystemPrompt = systemPrompt;
        if (!retrievedStatements.isEmpty()) {
            String knowledge = retrievedStatements.stream()
                    .map(Statement::formatForPrompt)
                    .collect(Collectors.joining("\n"));
            fullSystemPrompt = systemPrompt + "\n\nBekannte Fakten (natürlich einbauen, Format nicht zitieren):\n" + knowledge;
        }
        messages.add(new ChatMessage("system", fullSystemPrompt));
        messages.addAll(ConversationTurns.lastTurns(history, recentTurns));
        return messages;
    }
}
