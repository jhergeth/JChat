package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RetrieverStatementSelectorTest {

    @Test
    void includesAllFactsFromRecentTurns() {
        Instant t1 = Instant.parse("2026-08-18T10:00:00Z");
        Instant t2 = Instant.parse("2026-08-18T11:00:00Z");
        Instant t3 = Instant.parse("2026-08-18T12:00:00Z");

        List<Statement> all = List.of(
                stmt("A", "p", "1", "turn-1", t1),
                stmt("B", "p", "2", "turn-2", t2),
                stmt("C", "p", "3", "turn-2", t2),
                stmt("D", "p", "4", "turn-3", t3));

        List<Statement> selected = RetrieverStatementSelector.select(all, 2, 0, 12);

        assertEquals(3, selected.size());
        assertTrue(selected.stream().anyMatch(s -> "B".equals(s.subject())));
        assertTrue(selected.stream().anyMatch(s -> "C".equals(s.subject())));
        assertTrue(selected.stream().anyMatch(s -> "D".equals(s.subject())));
        assertFalse(selected.stream().anyMatch(s -> "A".equals(s.subject())));
    }

    @Test
    void fillsUpToMinStatementsFromOlderFacts() {
        Instant old = Instant.parse("2020-01-01T00:00:00Z");
        Instant recent = Instant.parse("2026-08-18T12:00:00Z");

        List<Statement> all = List.of(
                stmt("Only", "recent", "x", "turn-new", recent),
                stmt("Old1", "p", "a", "turn-old-1", old),
                stmt("Old2", "p", "b", "turn-old-2", old),
                stmt("Old3", "p", "c", "turn-old-3", old));

        List<Statement> selected = RetrieverStatementSelector.select(all, 1, 3, 12);

        assertEquals(3, selected.size());
        assertTrue(selected.stream().anyMatch(s -> "Only".equals(s.subject())));
    }

    @Test
    void respectsMaxStatements() {
        Instant t = Instant.parse("2026-08-18T12:00:00Z");
        List<Statement> all = List.of(
                stmt("A", "p", "1", "turn", t),
                stmt("B", "p", "2", "turn", t),
                stmt("C", "p", "3", "turn", t));

        List<Statement> selected = RetrieverStatementSelector.select(all, 1, 1, 2);

        assertEquals(2, selected.size());
    }

    private static Statement stmt(String subject, String predicate, String object, String turnId, Instant createdAt) {
        return new Statement(subject, predicate, object, "conv", turnId, createdAt);
    }
}
