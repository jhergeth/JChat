package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeStoreSearchServiceTest {

    @Test
    void skipsWebSearchWhenStrongMatchExists() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        store.add(stmt("Canberra", "ist_hauptstadt_von", "Australien"));
        KnowledgeStoreSearchService service = new KnowledgeStoreSearchService(store, new KnowledgeLimits(500, 12, 6, 3, 12, 6));

        Optional<KnowledgeStoreMatch> match = service.tryMatch(
                "default",
                "Was ist die Hauptstadt von Australien?",
                "Hauptstadt Australien");

        assertTrue(match.isPresent());
        assertEquals("Hauptstadt Australien", match.get().query());
        assertTrue(match.get().promptContext().contains("Canberra"));
        assertTrue(match.get().topScore() >= 4);
    }

    @Test
    void doesNotMatchUnrelatedStoredFacts() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        store.add(stmt("Donald Trump", "hat_position", "Präsident der USA"));
        KnowledgeStoreSearchService service = new KnowledgeStoreSearchService(store, new KnowledgeLimits(500, 12, 6, 3, 12, 6));

        Optional<KnowledgeStoreMatch> match = service.tryMatch(
                "default",
                "Was ist die Hauptstadt von Australien?",
                "Hauptstadt Australien");

        assertTrue(match.isEmpty());
    }

    @Test
    void requiresMultipleFactsForCabinetQuestions() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        store.add(stmt("Donald Trump", "hat_position", "Präsident der USA"));
        store.add(stmt("Pete Hegseth", "ist", "Verteidigungsminister"));
        KnowledgeStoreSearchService service = new KnowledgeStoreSearchService(store, new KnowledgeLimits(500, 12, 6, 3, 12, 6));

        Optional<KnowledgeStoreMatch> match = service.tryMatch(
                "default",
                "Welche Mitglieder hat heute die US-amerikanische Regierung (Köpfe der Ministerien)",
                "US Kabinett 2026");

        assertTrue(match.isEmpty());
    }

    @Test
    void rejectsCanberraNoiseForUsCabinetQuestion() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        store.add(stmt("Canberra", "ist_hauptstadt_von", "Australien"));
        store.add(stmt("Australien", "hauptstadt", "Canberra"));
        store.add(stmt("Karoline Leavitt", "arbeitet_bei", "Weiße Haus"));
        store.add(stmt("Weiße Haus", "hat_pressesprecher", "Karoline Leavitt"));
        store.add(stmt("Donald Trump", "ist_praesident_von", "Vereinigte Staaten"));
        KnowledgeStoreSearchService service = new KnowledgeStoreSearchService(store, new KnowledgeLimits(500, 12, 6, 3, 12, 6));

        Optional<KnowledgeStoreMatch> match = service.tryMatch(
                "default",
                "Welche Mitglieder hat heute die US-amerikanische Regierung (Köpfe der Ministerien)",
                "US Regierung 2026 Mitglieder");

        assertTrue(match.isEmpty());
    }

    private static Statement stmt(String subject, String predicate, String object) {
        return new Statement(subject, predicate, object, "default", "turn", Instant.now());
    }

    private static final class InMemoryKnowledgeStore implements KnowledgeStore {
        private final java.util.Map<String, java.util.List<Statement>> byConversation = new java.util.HashMap<>();

        @Override
        public void add(Statement statement) {
            byConversation.computeIfAbsent(statement.conversationId(), id -> new java.util.ArrayList<>())
                    .add(statement);
        }

        @Override
        public void replaceAll(String conversationId, List<Statement> statements) {
            byConversation.put(conversationId, new java.util.ArrayList<>(statements));
        }

        @Override
        public List<Statement> all(String conversationId) {
            return List.copyOf(byConversation.getOrDefault(conversationId, List.of()));
        }
    }
}
