package name.hergeth.jchat.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SemanticTripleValidatorTest {

    @Test
    void parseMatchAcceptsJsonResponse() {
        assertTrue(SemanticTripleValidator.parseMatch("""
                {"match":true,"matchedTriple":"Maria Schmidt | wohnt_in | Augsburg","reason":"ok"}
                """));
    }

    @Test
    void parseMatchAcceptsMarkdownWrappedJson() {
        assertTrue(SemanticTripleValidator.parseMatch("""
                ```json
                {"match": true, "matchedTriple": "", "reason": "same fact"}
                ```
                """));
    }

    @Test
    void parseMatchRejectsMissingMatch() {
        assertFalse(SemanticTripleValidator.parseMatch("""
                {"match":false,"matchedTriple":"","reason":"not found"}
                """));
        assertFalse(SemanticTripleValidator.parseMatch("not json"));
        assertFalse(SemanticTripleValidator.parseMatch(""));
    }
}
