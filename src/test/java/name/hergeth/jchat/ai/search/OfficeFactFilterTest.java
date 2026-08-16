package name.hergeth.jchat.ai.search;

import name.hergeth.jchat.ai.model.Statement;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficeFactFilterTest {

    @Test
    void recognizesGenericOfficeHolderFact() {
        Statement fact = new Statement(
                "Donald Trump", "ist_amtsinhaber_von", "Vereinigte Staaten von Amerika",
                "c1", "web-1", Instant.now());
        assertTrue(OfficeFactFilter.isOfficeHolderFact(fact));
        assertTrue(OfficeFactFilter.touchesOfficeHolder(fact));
    }

    @Test
    void recognizesUkPremierFact() {
        Statement fact = new Statement(
                "Keir Starmer", "ist_amtsinhaber_von", "Vereinigtes Königreich",
                "c1", "web-1", Instant.now());
        assertTrue(OfficeFactFilter.isOfficeHolderFact(fact));
    }

    @Test
    void rejectsInstitutionalSubject() {
        Statement bad = new Statement(
                "Verfassung der Vereinigten Staaten", "regeltdienachfolge", "Joe Biden",
                "c1", "web-1", Instant.now());
        assertFalse(OfficeFactFilter.isOfficeHolderFact(bad));
    }

    @Test
    void rejectsRoleAsSubject() {
        Statement bad = new Statement(
                "Bundeskanzler Deutschland", "istbundeskanzler_von", "Deutschland",
                "c1", "web-1", Instant.now());
        assertFalse(OfficeFactFilter.isOfficeHolderFact(bad));
    }
}
