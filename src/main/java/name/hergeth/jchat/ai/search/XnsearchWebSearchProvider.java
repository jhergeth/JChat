package name.hergeth.jchat.ai.search;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class XnsearchWebSearchProvider implements WebSearchProvider {

    private static final Logger LOG = LoggerFactory.getLogger(XnsearchWebSearchProvider.class);
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private final HttpClient httpClient;
    private final String baseUrl;
    private final int maxPages;
    private final int pageSize;

    public XnsearchWebSearchProvider(
            @Value("${app.search.xnsearch.base-url:}") String baseUrl,
            @Value("${app.search.xnsearch.max-pages:5}") int maxPages,
            @Value("${app.search.xnsearch.page-size:10}") int pageSize) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.maxPages = Math.max(maxPages, 1);
        this.pageSize = Math.max(pageSize, 1);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank() && !baseUrl.startsWith("${");
    }

    @Override
    public List<SearchSnippet> search(String query, int maxResults) {
        if (!isConfigured()) {
            throw new IllegalStateException("xnsearch base-url not configured (XNSEARCH_URL)");
        }
        try {
            int target = Math.max(maxResults, 1);
            List<SearchSnippet> usable = new ArrayList<>();
            Set<String> seenUrls = new LinkedHashSet<>();

            for (int page = 1; page <= maxPages && usable.size() < target; page++) {
                List<SearchSnippet> pageResults = fetchPage(query, page);
                if (pageResults.isEmpty()) {
                    LOG.debug("xnsearch page {} for '{}' returned no results — stopping", page, query);
                    break;
                }

                int added = 0;
                for (SearchSnippet snippet : pageResults) {
                    if (!SearchSnippetQuality.isUsableLink(snippet)) {
                        continue;
                    }
                    String normalizedUrl = snippet.url().trim();
                    if (!seenUrls.add(normalizedUrl)) {
                        continue;
                    }
                    usable.add(snippet);
                    added++;
                    if (usable.size() >= target) {
                        break;
                    }
                }

                LOG.debug("xnsearch page {} for '{}': {} raw, {} usable (total {})",
                        page, query, pageResults.size(), added, usable.size());
            }

            LOG.info("xnsearch for '{}': {} usable links from up to {} pages", query, usable.size(), maxPages);
            return usable;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("xnsearch interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("xnsearch failed", e);
        }
    }

    private List<SearchSnippet> fetchPage(String query, int page) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        int offset = (page - 1) * pageSize;
        String url = baseUrl
                + "/search?q=" + encodedQuery
                + "&limit=" + pageSize
                + "&page=" + page
                + "&offset=" + offset;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("xnsearch HTTP " + response.statusCode() + ": " + response.body());
        }
        return parseResponse(response.body());
    }

    private List<SearchSnippet> parseResponse(String body) throws IOException {
        com.fasterxml.jackson.databind.JsonNode root = MAPPER.readTree(body);
        com.fasterxml.jackson.databind.JsonNode results = root.isArray() ? root : root.get("results");
        if (results == null) {
            results = root.get("items");
        }
        List<SearchSnippet> snippets = new ArrayList<>();
        if (results == null || !results.isArray()) {
            LOG.warn("xnsearch response has no results array");
            return snippets;
        }
        for (com.fasterxml.jackson.databind.JsonNode item : results) {
            snippets.add(new SearchSnippet(
                    firstText(item, "title", "name"),
                    firstText(item, "url", "link", "href"),
                    firstText(item, "snippet", "description", "text", "content")));
        }
        return snippets;
    }

    private static String firstText(com.fasterxml.jackson.databind.JsonNode node, String... fields) {
        for (String field : fields) {
            com.fasterxml.jackson.databind.JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return "";
    }
}
