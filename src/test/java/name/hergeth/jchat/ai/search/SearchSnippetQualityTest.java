package name.hergeth.jchat.ai.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchSnippetQualityTest {

    @Test
    void detectsSubstantiveSnippet() {
        SearchSnippet good = new SearchSnippet("T", "https://x", "x".repeat(50));
        assertTrue(SearchSnippetQuality.isSubstantive(good));
    }

    @Test
    void rejectsEmptyXnsearchStyleSnippet() {
        SearchSnippet empty = new SearchSnippet("", "", "");
        assertFalse(SearchSnippetQuality.isSubstantive(empty));
        assertFalse(SearchSnippetQuality.isUsableLink(empty));
    }

    @Test
    void acceptsUsableLinkWithTitleAndUrl() {
        SearchSnippet link = new SearchSnippet("Example", "https://example.org", "");
        assertTrue(SearchSnippetQuality.isUsableLink(link));
        assertFalse(SearchSnippetQuality.isSubstantive(link));
    }

    @Test
    void rejectsLinkWithoutTitle() {
        SearchSnippet link = new SearchSnippet("", "https://example.org", "some text");
        assertFalse(SearchSnippetQuality.isUsableLink(link));
    }
}
