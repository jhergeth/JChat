package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.openai.dto.ChatMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationTurnsTest {

    @Test
    void keepsLastTwoTurns() {
        List<ChatMessage> history = List.of(
                new ChatMessage("user", "eins"),
                new ChatMessage("assistant", "zwei"),
                new ChatMessage("user", "drei"),
                new ChatMessage("assistant", "vier"),
                new ChatMessage("user", "fuenf"));

        List<ChatMessage> recent = ConversationTurns.lastTurns(history, 2);

        assertEquals(3, recent.size());
        assertEquals("drei", recent.get(0).content());
        assertEquals("vier", recent.get(1).content());
        assertEquals("fuenf", recent.get(2).content());
    }
}

class StatementRelevanceScorerTest {

    @Test
    void ranksMatchingStatementsHigher() {
        Instant now = Instant.now();
        List<Statement> statements = List.of(
                stmt("Anna", "wohnt_in", "Hamburg", now),
                stmt("JChat", "nutzt_gpu", "Nvidia P100", now));

        List<Statement> ranked = StatementRelevanceScorer.rank(statements, "Mit welcher Karte arbeitet JChat?", 5);

        assertEquals("JChat", ranked.get(0).subject());
    }

    private static Statement stmt(String s, String p, String o, Instant t) {
        return new Statement(s, p, o, "default", "turn", t);
    }
}
