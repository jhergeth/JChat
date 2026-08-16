package name.hergeth.jchat.ai.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchIntentHeuristicTest {

    @Test
    void triggersOnBundeskanzlerCorrection() {
        assertTrue(SearchIntentHeuristic.shouldSearch(
                "Olaf scholz ist NICHT der heutige Bundeskanzler.", List.of()));
    }

    @Test
    void triggersOnWerIstHeuteBundeskanzler() {
        assertTrue(SearchIntentHeuristic.shouldSearch("wer ist heute bundeskanzler?", List.of()));
    }

    @Test
    void triggersOnTypoBundeskanzler() {
        assertTrue(SearchIntentHeuristic.shouldSearch("wer ist heute bu8ndeskanzler?", List.of()));
    }

    @Test
    void triggersOnCountryFollowUp() {
        assertTrue(SearchIntentHeuristic.shouldSearch(
                "und in den usa?",
                List.of("wer ist aktuell bundeskanzler?", "und in den usa?")));
    }

    @Test
    void followUpFallbackUsesCurrentMessage() {
        String query = SearchIntentHeuristic.fallbackQuery(
                "nd in österreich?",
                List.of("wer ist aktuell bundeskanzlerin?", "nd in österreich?"));
        assertTrue(query.toLowerCase().contains("österreich"));
    }

    @Test
    void researchAgainUsesPriorQuestion() {
        String query = SearchIntentHeuristic.fallbackQuery(
                "schaue nochmal nach",
                List.of(
                        "Olaf scholz ist NICHT der heutige Bundeskanzler.",
                        "wer ist heute bundeskanzler?",
                        "schaue nochmal nach"));
        assertTrue(query.toLowerCase().contains("bundeskanzler"));
    }

    @Test
    void ignoresSmalltalk() {
        assertFalse(SearchIntentHeuristic.shouldSearch("danke!", List.of("wer ist bundeskanzler?")));
    }
}
