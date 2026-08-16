package name.hergeth.jchat.ai.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SnippetRelevanceRankerTest {

    @Test
    void prefersSentenceWithCurrentOfficeHolder() {
        String text = "Der Präsident der Vereinigten Staaten von Amerika ist Staatsoberhaupt. "
                + "Aktueller Amtsinhaber ist seit dem 20. Januar 2025 Donald Trump. "
                + "Die Amtszeit beträgt vier Jahre.";
        String best = SnippetRelevanceRanker.bestSentences(text, "Präsident der Vereinigten Staaten aktuell", 2);
        assertTrue(best.contains("Donald Trump"));
        assertTrue(best.contains("Amtsinhaber"));
    }

    @Test
    void ranksOfficePageBeforeConstitutionPage() {
        SearchSnippet office = new SearchSnippet(
                "Präsident der Vereinigten Staaten",
                "https://example.org/office",
                "Der Präsident ist Staatsoberhaupt. Aktueller Amtsinhaber ist seit dem 20. Januar 2025 Donald Trump.");
        SearchSnippet constitution = new SearchSnippet(
                "Verfassung der Vereinigten Staaten",
                "https://example.org/constitution",
                "Die Verfassung der Vereinigten Staaten regelt die Nachfolge des Präsidenten.");
        List<SearchSnippet> ranked = SnippetRelevanceRanker.rank(
                List.of(constitution, office),
                "Präsident der Vereinigten Staaten aktuell");
        assertTrue(ranked.get(0).title().contains("Präsident der Vereinigten Staaten"));
    }
}
