package name.hergeth.jchat.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TermMatcherTest {

    @Test
    void shortTermsRequireWordBoundary() {
        assertFalse(TermMatcher.matches("australien", "us"));
        assertFalse(TermMatcher.matches("Australian Capital Territory", "us"));
        assertTrue(TermMatcher.matches("US Regierung", "us"));
    }
}
