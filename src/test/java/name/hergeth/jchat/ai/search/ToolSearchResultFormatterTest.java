package name.hergeth.jchat.ai.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolSearchResultFormatterTest {

    @Test
    void producesCompactOutputWithoutWikiSections() {
        SearchSnippet snippet = new SearchSnippet(
                "Pete Hegseth",
                "https://de.wikipedia.org/wiki/Pete_Hegseth",
                "Seit Januar 2025 ist er Verteidigungsminister im zweiten Kabinett von Donald Trump. "
                        + "\n\n== Herkunft, Studium und frühe Karriere ==\n"
                        + "Pete Hegseths Vorfahren stammten aus Norwegen und lebten sehr lange in Minnesota.");
        SearchTrace trace = new SearchTrace(
                true, "success", "OK", "US Kabinett 2026", 1, List.of(), List.of(), List.of(snippet), "");

        String formatted = ToolSearchResultFormatter.format(trace);

        assertTrue(formatted.contains("Pete Hegseth"));
        assertFalse(formatted.contains("Herkunft, Studium"));
        assertTrue(formatted.length() <= 1200);
    }
}
