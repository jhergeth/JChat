package name.hergeth.jchat.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TripleMatcherTest {

    @Test
    void matchesQueryHubWithSpaceAndPostgreSqlSpelling() {
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("Query Hub", "nutzt", "Postgre SQL"),
                new TripleExpectation("QueryHub", "nutzt", "PostgreSQL")));
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("Query Hub", "laeuft_auf", "srv-prod-01"),
                new TripleExpectation("QueryHub", "laeuft_auf", "srv-prod")));
    }

    @Test
    void matchesFahrtWithUmlaut() {
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("Maria Schmidt", "fährt", "Toyota RAV4"),
                new TripleExpectation("Maria", "faehrt", "RAV4")));
    }

    @Test
    void matchesSubjectCompactForm() {
        assertTrue(TripleMatcher.matchesSubject("Query Hub", "QueryHub"));
        assertTrue(TripleMatcher.matchesSubject("mariaschmidt", "Maria Schmidt"));
    }

    @Test
    void matchesMariaSchmidtProfileFacts() {
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("Maria Schmidt", "wohnt_in", "Augsburg"),
                new TripleExpectation("Maria", "wohnt_in", "Augsburg")));
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("Maria Schmidt", "arbeitet_bei", "Tech Line AG"),
                new TripleExpectation("Maria", "arbeitet_bei", "TechLine")));
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("Maria Schmidt", "hat_hobby", "Klettern"),
                new TripleExpectation("Maria", "hobby", "Klettern")));
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("Maria Schmidt", "magessen", "Ramen"),
                new TripleExpectation("Maria", "lieblingsessen", "Ramen")));
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("Maria Schmidt", "fahrt_auto", "Toyota RAV4"),
                new TripleExpectation("Maria", "faehrt", "RAV4")));
    }

    @Test
    void matchesDevTeamStackFacts() {
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("Lukas Mueller", "wohnt_in", "Berlin"),
                new TripleExpectation("Lukas", "wohnt_in", "Berlin")));
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("Lukas Mueller", "arbeitet_bei", "DataForge GmbH"),
                new TripleExpectation("Lukas", "arbeitet_bei", "DataForge")));
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("QueryHub", "nutzt", "PostgreSQL"),
                new TripleExpectation("QueryHub", "nutzt", "PostgreSQL")));
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("QueryHub", "laeuft_auf", "srv-prod-01"),
                new TripleExpectation("QueryHub", "laeuft_auf", "srv-prod")));
        assertTrue(TripleMatcher.matches(
                new StatementSnapshot("Sara", "arbeitet_bei", "DataForge GmbH"),
                new TripleExpectation("Sara", "arbeitet_bei", "DataForge")));
    }

    @Test
    void matchesSubjectPrefixRequiresMinLength() {
        assertFalse(TripleMatcher.matchesSubject("AM5", "AM"));
        assertTrue(TripleMatcher.matchesSubject("AM5", "AM5"));
    }

    @Test
    void matchesObjectIgnoresSpacesAndCase() {
        assertTrue(TripleMatcher.matchesObject("Tech Line AG", "TechLine"));
        assertTrue(TripleMatcher.matchesObject("München", "Muenchen"));
    }

    @Test
    void rejectsUnrelatedTriples() {
        assertFalse(TripleMatcher.matches(
                new StatementSnapshot("Anna", "wohnt_in", "Hamburg"),
                new TripleExpectation("Maria", "wohnt_in", "Augsburg")));
    }
}
