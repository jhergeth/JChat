package name.hergeth.jchat.ai.context;

import name.hergeth.jchat.ai.KnowledgeStore;
import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.openai.dto.ChatMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConversationContextResolverTest {

    @Test
    void resolvesMasculinePronounToBundeskanzlerEntity() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        store.add(stmt("Friedrich Merz", "hat_position", "Bundeskanzler"));
        store.add(stmt("Friedrich Merz", "hat_ehepartner", "Uschi Merz"));

        ConversationContextResolver resolver = new ConversationContextResolver(store, 3);
        List<ChatMessage> messages = List.of(
                new ChatMessage("user", "Wer ist aktuell der Bundeskanzler von Deutschland?"),
                new ChatMessage("assistant", "Der Bundeskanzler ist Friedrich Merz."),
                new ChatMessage("user", "wie heisst seine Frau?"));

        ResolvedContext context = resolver.resolve("conv", messages, "wie heisst seine Frau?");

        assertTrue(context.hasFocusEntities());
        assertTrue(context.hasPronounResolution());
        assertTrue(context.resolvedQuery().toLowerCase().contains("friedrich merz"));
        assertTrue(context.focusEntityLabels().stream().anyMatch(label -> label.contains("Merz")));
    }

    @Test
    void resolvesPressesprecherQuestionToTrumpEntity() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        store.add(stmt("Donald Trump", "hat_position", "Präsident der USA"));
        store.add(stmt("Donald Trump", "hat_pressesprecherin", "Karoline Leavitt"));

        ConversationContextResolver resolver = new ConversationContextResolver(store, 3);
        List<ChatMessage> messages = List.of(
                new ChatMessage("user", "Wer ist der aktuelle Präsident der USA?"),
                new ChatMessage("assistant", "Donald Trump ist Präsident."),
                new ChatMessage("user", "wie heisst die Person, die sein Pressesprecher ist?"));

        ResolvedContext context = resolver.resolve("conv", messages,
                "wie heisst die Person, die sein Pressesprecher ist?");

        assertTrue(context.hasFocusEntities());
        assertTrue(context.focusEntityLabels().stream().anyMatch(label -> label.contains("Trump")));
        assertTrue(context.resolvedQuery().toLowerCase().contains("trump"));
    }

    private static Statement stmt(String subject, String predicate, String object) {
        return new Statement(subject, predicate, object, "conv", "turn", Instant.now());
    }

    private static final class InMemoryKnowledgeStore implements KnowledgeStore {
        private final java.util.List<Statement> statements = new java.util.ArrayList<>();

        @Override
        public void add(Statement statement) {
            statements.add(statement);
        }

        @Override
        public void replaceAll(String conversationId, List<Statement> newStatements) {
            statements.clear();
            statements.addAll(newStatements);
        }

        @Override
        public List<Statement> all(String conversationId) {
            return List.copyOf(statements);
        }
    }
}
