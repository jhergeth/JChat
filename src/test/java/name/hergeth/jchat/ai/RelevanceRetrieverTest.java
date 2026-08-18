package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelevanceRetrieverTest {

    @Test
    void retrievePrefersFactsFromRecentTurns() {
        Instant old = Instant.parse("2020-01-01T00:00:00Z");
        Instant recent = Instant.parse("2026-08-18T12:00:00Z");
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        store.add(new Statement("Canberra", "ist_hauptstadt_von", "Australien", "conv", "turn-recent", recent));
        store.add(new Statement("Alt", "info", "wert", "conv", "turn-old", old));

        RelevanceRetriever retriever = new RelevanceRetriever(store, new KnowledgeLimits(500, 5, 1, 1, 5, 6));
        List<Statement> result = retriever.retrieve("conv", "Hauptstadt Australien");

        assertTrue(result.stream().anyMatch(s -> "Canberra".equals(s.subject())));
        assertEquals(1, result.size());
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
