package name.hergeth.jchat.ai.search;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class CompositeWebSearchProvider implements WebSearchProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeWebSearchProvider.class);

    private final Optional<XnsearchWebSearchProvider> xnsearch;
    private final WikipediaWebSearchProvider wikipedia;

    public CompositeWebSearchProvider(
            Optional<XnsearchWebSearchProvider> xnsearch,
            WikipediaWebSearchProvider wikipedia) {
        this.xnsearch = xnsearch;
        this.wikipedia = wikipedia;
    }

    @Override
    public boolean isConfigured() {
        return wikipedia.isConfigured()
                || (xnsearch.isPresent() && xnsearch.get().isConfigured());
    }

    @Override
    public List<SearchSnippet> search(String query, int maxResults) {
        List<SearchSnippet> wikipediaResults = List.of();
        if (wikipedia.isConfigured()) {
            try {
                wikipediaResults = wikipedia.search(query, maxResults);
            } catch (Exception e) {
                LOG.warn("Wikipedia search failed for '{}': {}", query, e.getMessage());
            }
        }

        List<SearchSnippet> xnsearchResults = List.of();
        if (xnsearch.isPresent() && xnsearch.get().isConfigured()) {
            try {
                xnsearchResults = xnsearch.get().search(query, maxResults);
            } catch (Exception e) {
                LOG.warn("xnsearch failed for '{}': {}", query, e.getMessage());
            }
        }

        List<SearchSnippet> merged = merge(wikipediaResults, xnsearchResults);
        LOG.info("Search for '{}': {} Wikipedia + {} xnsearch → {} merged",
                query, wikipediaResults.size(), xnsearchResults.size(), merged.size());
        return limit(merged, maxResults);
    }

    private static List<SearchSnippet> merge(List<SearchSnippet> primary, List<SearchSnippet> secondary) {
        Map<String, SearchSnippet> byKey = new LinkedHashMap<>();
        for (SearchSnippet snippet : primary) {
            byKey.put(dedupeKey(snippet), snippet);
        }
        for (SearchSnippet snippet : secondary) {
            String key = dedupeKey(snippet);
            SearchSnippet existing = byKey.get(key);
            if (existing == null) {
                byKey.put(key, snippet);
                continue;
            }
            if (!SearchSnippetQuality.isSubstantive(existing) && SearchSnippetQuality.isSubstantive(snippet)) {
                byKey.put(key, snippet);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private static String dedupeKey(SearchSnippet snippet) {
        if (snippet.url() != null && !snippet.url().isBlank()) {
            return snippet.url().trim().toLowerCase();
        }
        return snippet.title() == null ? "" : snippet.title().toLowerCase();
    }

    private static List<SearchSnippet> limit(List<SearchSnippet> snippets, int maxResults) {
        return snippets.stream().limit(Math.max(maxResults, 1)).toList();
    }
}
