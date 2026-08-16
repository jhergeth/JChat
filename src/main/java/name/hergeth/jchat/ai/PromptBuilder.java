package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.context.AmbientContext;
import name.hergeth.jchat.ai.context.AmbientContextFormatter;
import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.openai.dto.ChatMessage;

import java.util.List;

public interface PromptBuilder {
    List<ChatMessage> build(List<ChatMessage> history, String systemPrompt, List<Statement> retrievedStatements);

    default List<ChatMessage> build(
            List<ChatMessage> history,
            String systemPrompt,
            List<Statement> retrievedStatements,
            String searchContext) {
        return build(history, systemPrompt, retrievedStatements, searchContext, null);
    }

    default List<ChatMessage> build(
            List<ChatMessage> history,
            String systemPrompt,
            List<Statement> retrievedStatements,
            String searchContext,
            AmbientContext ambientContext) {
        String enrichedPrompt = systemPrompt;
        if (ambientContext != null) {
            enrichedPrompt = enrichedPrompt + AmbientContextFormatter.format(ambientContext);
        }
        if (searchContext != null && !searchContext.isBlank()) {
            enrichedPrompt = enrichedPrompt + searchContext;
        }
        return build(history, enrichedPrompt, retrievedStatements);
    }
}
