package name.hergeth.jchat.ai.search;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WikiOfficeHolderExtractorTest {

    @Test
    void extractsFromKabinettMerzText() {
        SearchSnippet snippet = new SearchSnippet(
                "Kabinett Merz",
                "https://de.wikipedia.org/wiki/Kabinett_Merz",
                "Das Kabinett Merz ist die seit dem 6. Mai 2025 amtierende Bundesregierung der "
                        + "Bundesrepublik Deutschland unter der Führung des Bundeskanzlers Friedrich Merz.");
        Optional<WikiOfficeHolderExtractor.OfficeHolderFact> fact = WikiOfficeHolderExtractor.extractOfficeHolder(snippet);
        assertTrue(fact.isPresent());
        assertEquals("Friedrich Merz", fact.get().person());
        assertEquals("Bundesrepublik Deutschland", fact.get().jurisdiction());
    }

    @Test
    void extractsFromPersonPage() {
        SearchSnippet snippet = new SearchSnippet(
                "Friedrich Merz",
                "https://de.wikipedia.org/wiki/Friedrich_Merz",
                "Joachim-Friedrich Martin Josef Merz (* 11. November 1955 in Brilon) ist ein deutscher "
                        + "Politiker (CDU). Er ist seit dem 6. Mai 2025 der zehnte Bundeskanzler der "
                        + "Bundesrepublik Deutschland.");
        Optional<WikiOfficeHolderExtractor.OfficeHolderFact> fact = WikiOfficeHolderExtractor.extractOfficeHolder(snippet);
        assertTrue(fact.isPresent());
        assertEquals("Friedrich Merz", fact.get().person());
    }

    @Test
    void extractsFromTruncatedKabinettMerzSnippet() {
        String longIntro = "Das Kabinett Merz ist die seit dem 6. Mai 2025 amtierende Bundesregierung der "
                + "Bundesrepublik Deutschland unter der Führung des Bundeskanzlers Friedrich";
        SearchSnippet snippet = new SearchSnippet(
                "Kabinett Merz",
                "https://de.wikipedia.org/wiki/Kabinett_Merz",
                longIntro);
        Optional<WikiOfficeHolderExtractor.OfficeHolderFact> fact = WikiOfficeHolderExtractor.extractOfficeHolder(snippet);
        assertTrue(fact.isPresent());
        assertEquals("Friedrich Merz", fact.get().person());
    }

    @Test
    void extractsUsPresidentFromOfficePage() {
        SearchSnippet snippet = new SearchSnippet(
                "Präsident der Vereinigten Staaten",
                "https://de.wikipedia.org/wiki/Pr%C3%A4sident_der_Vereinigten_Staaten",
                "Der Präsident der Vereinigten Staaten von Amerika ist Staatsoberhaupt. "
                        + "Aktueller Amtsinhaber ist seit dem 20. Januar 2025 Donald Trump.");
        Optional<WikiOfficeHolderExtractor.OfficeHolderFact> fact = WikiOfficeHolderExtractor.extractOfficeHolder(snippet);
        assertTrue(fact.isPresent());
        assertEquals("Donald Trump", fact.get().person());
    }

    @Test
    void extractsUsPresidentFromPersonPage() {
        SearchSnippet snippet = new SearchSnippet(
                "Donald Trump",
                "https://de.wikipedia.org/wiki/Donald_Trump",
                "Donald John Trump ist ein US-amerikanischer Politiker. "
                        + "Er ist seit dem 20. Januar 2025 der 47. Präsident der Vereinigten Staaten.");
        Optional<WikiOfficeHolderExtractor.OfficeHolderFact> fact = WikiOfficeHolderExtractor.extractOfficeHolder(snippet);
        assertTrue(fact.isPresent());
        assertEquals("Donald Trump", fact.get().person());
    }

    @Test
    void extractsOfficeFactsForUsPresident() {
        SearchSnippet snippet = new SearchSnippet(
                "Präsident der Vereinigten Staaten",
                "https://example.org",
                "Aktueller Amtsinhaber ist seit dem 20. Januar 2025 Donald Trump.");
        List<?> facts = WikiOfficeHolderExtractor.extractOfficeFacts(List.of(snippet), "c1", "web-1");
        assertEquals(1, facts.size());
    }

    @Test
    void rejectsSingleWordName() {
        assertTrue(WikiOfficeHolderExtractor.extractOfficeFacts(
                List.of(new SearchSnippet("X", "https://x", "unter der Führung des Bundeskanzlers Friedrich.")),
                "c1", "web-1").isEmpty());
    }

    @Test
    void extractsOfficeFacts() {
        SearchSnippet snippet = new SearchSnippet(
                "Kabinett Merz",
                "https://example.org",
                "unter der Führung des Bundeskanzlers Friedrich Merz.");
        List<?> facts = WikiOfficeHolderExtractor.extractOfficeFacts(List.of(snippet), "c1", "web-1");
        assertEquals(1, facts.size());
    }

    @Test
    void extractsUkPremierFromPersonPage() {
        SearchSnippet snippet = new SearchSnippet(
                "Keir Starmer",
                "https://en.wikipedia.org/wiki/Keir_Starmer",
                "Sir Keir Rodney Starmer (* 2. September 1963) ist ein britischer Politiker. "
                        + "Er ist seit dem 5. Juli 2024 der Premierminister des Vereinigten Königreichs.");
        Optional<WikiOfficeHolderExtractor.OfficeHolderFact> fact = WikiOfficeHolderExtractor.extractOfficeHolder(snippet);
        assertTrue(fact.isPresent());
        assertEquals("Keir Starmer", fact.get().person());
        assertEquals("Vereinigtes Königreich", fact.get().jurisdiction());
    }
}
