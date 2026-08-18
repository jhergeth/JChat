package name.hergeth.jchat.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeLimitsTest {

    @Test
    void enforcesMinimumOfOneForStoreAndMaxPrompt() {
        KnowledgeLimits limits = new KnowledgeLimits(0, 0, 0, 0, 0, 0);
        assertEquals(1, limits.maxStoreStatements());
        assertEquals(1, limits.maxPromptStatements());
        assertEquals(1, limits.maxKnowledgeInContext());
        assertEquals(1, limits.maxSearchInContext());
    }

    @Test
    void capsMinPromptStatementsAtMaxPromptStatements() {
        KnowledgeLimits limits = new KnowledgeLimits(100, 5, 20, 2, 12, 6);
        assertEquals(5, limits.minPromptStatements());
    }
}
