package name.hergeth.jchat.ai.search;

import name.hergeth.jchat.ai.model.Statement;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchTripleQualityFilterTest {

    @Test
    void rejectsConstitutionalMetaTriple() {
        Statement bad = new Statement(
                "Verfassung der Vereinigten Staaten", "regeltdienachfolge", "Joe Biden",
                "c1", "web-1", Instant.now());
        assertTrue(SearchTripleQualityFilter.isLowQuality(bad));
    }

    @Test
    void rejectsNoFixedOfficeHolderTriple() {
        Statement bad = new Statement(
                "Vereinigte Staaten", "habenprasidentenamt", "keinefesteninhaber",
                "c1", "web-1", Instant.now());
        assertTrue(SearchTripleQualityFilter.isLowQuality(bad));
    }

    @Test
    void acceptsPersonAsUsPresident() {
        Statement good = new Statement(
                "Donald Trump", "ist_amtsinhaber_von", "Vereinigte Staaten von Amerika",
                "c1", "web-1", Instant.now());
        assertFalse(SearchTripleQualityFilter.isLowQuality(good));
    }

    @Test
    void rejectsBundeskanzlerTripleWithCountryObject() {
        Statement bad = new Statement(
                "Bundeskanzler Deutschland", "istbundeskanzler_von", "Deutschland",
                "c1", "web-1", Instant.now());
        assertTrue(SearchTripleQualityFilter.isLowQuality(bad));
    }

    @Test
    void rejectsSingleWordChancellorSubject() {
        Statement bad = new Statement(
                "Friedrich", "istbundeskanzler_von", "Deutschland",
                "c1", "web-1", Instant.now());
        assertTrue(SearchTripleQualityFilter.isLowQuality(bad));
    }

    @Test
    void acceptsPersonAsBundeskanzler() {
        Statement good = new Statement(
                "Friedrich Merz", "ist_amtsinhaber_von", "Bundesrepublik Deutschland",
                "c1", "web-1", Instant.now());
        assertFalse(SearchTripleQualityFilter.isLowQuality(good));
    }

    @Test
    void acceptsUkPremierTriple() {
        Statement good = new Statement(
                "Keir Starmer", "ist_amtsinhaber_von", "Vereinigtes Königreich",
                "c1", "web-1", Instant.now());
        assertFalse(SearchTripleQualityFilter.isLowQuality(good));
    }

    @Test
    void acceptsCapitalCityTriple() {
        Statement good = new Statement(
                "Canberra", "ist_hauptstadt_von", "Australien",
                "c1", "web-1", Instant.now());
        assertFalse(SearchTripleQualityFilter.isLowQuality(good));
    }

    @Test
    void rejectsCountryOnlyObjectForOfficePredicate() {
        Statement bad = new Statement(
                "Joe Biden", "istprasident", "Vereinigte Staaten",
                "c1", "web-1", Instant.now());
        assertTrue(SearchTripleQualityFilter.isLowQuality(bad));
    }

    @Test
    void rejectsPremierRoleAsSubject() {
        Statement bad = new Statement(
                "Premierminister Vereinigtes Königreich", "ist_amtsinhaber_von", "UK",
                "c1", "web-1", Instant.now());
        assertTrue(SearchTripleQualityFilter.isLowQuality(bad));
    }
}
