package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityFactExpanderTest {

    @Test
    void expandsAllFactsForFocusedEntityAndRelatedPerson() {
        List<Statement> all = List.of(
                stmt("Friedrich Merz", "hat_position", "Bundeskanzler"),
                stmt("Friedrich Merz", "hat_ehepartner", "Uschi Merz"),
                stmt("Uschi Merz", "ist", "Ehefrau von Friedrich Merz"),
                stmt("Canberra", "ist_hauptstadt_von", "Australien"));

        EntityIndex index = EntityIndex.from(all);
        String merzKey = index.entityKeysMentionedIn("Friedrich Merz").get(0);
        List<Statement> bundle = EntityFactExpander.expand(index, List.of(merzKey), 10);

        assertTrue(bundle.stream().anyMatch(s -> "Friedrich Merz".equals(s.subject())
                && "hat_ehepartner".equals(s.predicate())));
        assertTrue(bundle.stream().anyMatch(s -> "Uschi Merz".equals(s.subject())
                || "Uschi Merz".equals(s.object())));
        assertFalse(bundle.stream().anyMatch(s -> "Canberra".equals(s.subject())));
    }

    private static Statement stmt(String subject, String predicate, String object) {
        return new Statement(subject, predicate, object, "conv", "turn", Instant.now());
    }
}
