package name.hergeth.jchat.ai.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchQueryNormalizerTest {

    @Test
    void trimsAndCollapsesWhitespace() {
        assertEquals(
                "Präsident der Vereinigten Staaten aktuell",
                SearchQueryNormalizer.sanitize("  Präsident   der Vereinigten Staaten aktuell  "));
    }

    @Test
    void truncatesLongQueries() {
        String longQuery = "a".repeat(100);
        assertEquals(80, SearchQueryNormalizer.sanitize(longQuery).length());
    }

    @Test
    void keepsUnrelatedQueries() {
        assertEquals("Hauptstadt von Australien", SearchQueryNormalizer.sanitize("Hauptstadt von Australien"));
    }

    @Test
    void blankReturnsEmpty() {
        assertEquals("", SearchQueryNormalizer.sanitize("   "));
    }
}
