package name.hergeth.jchat.ai.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class WikipediaWebSearchProvider implements WebSearchProvider {

    private static final Logger LOG = LoggerFactory.getLogger(WikipediaWebSearchProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int EXTRACT_CHARS = 2500;
    private static final int ENRICH_TOP_PAGES = 2;

    private final HttpClient httpClient;
    private final String apiBase;
    private final boolean enabled;

    public WikipediaWebSearchProvider(
            @Value("${app.search.wikipedia.lang:de}") String lang,
            @Value("${app.search.wikipedia.enabled:true}") boolean enabled) {
        this.enabled = enabled;
        this.apiBase = "https://" + lang + ".wikipedia.org/w/api.php";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return enabled;
    }

    @Override
    public List<SearchSnippet> search(String query, int maxResults) {
        if (!enabled) {
            return List.of();
        }
        try {
            int limit = Math.min(Math.max(maxResults, 1), 10);
            List<SearchSnippet> snippets = searchTitles(query, limit);
            if (snippets.isEmpty()) {
                return snippets;
            }
            int enrichPages = looksLikeOfficeHolderQuery(query) ? 3 : ENRICH_TOP_PAGES;
            List<SearchSnippet> enriched = enrichWithExtracts(snippets, enrichPages);
            LOG.info("Wikipedia search for '{}' returned {} snippets ({} enriched)",
                    query, snippets.size(), enriched.size());
            return enriched;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Wikipedia search interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Wikipedia search failed", e);
        }
    }

    private List<SearchSnippet> searchTitles(String query, int limit) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = apiBase
                + "?action=query&list=search&format=json&origin=*"
                + "&utf8=1&srsearch=" + encodedQuery
                + "&srlimit=" + limit;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "JChat/0.1 (local research bot)")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Wikipedia HTTP " + response.statusCode());
        }
        return parseSearchResponse(response.body());
    }

    private List<SearchSnippet> enrichWithExtracts(List<SearchSnippet> snippets, int maxPages)
            throws IOException, InterruptedException {
        int count = Math.min(maxPages, snippets.size());
        List<String> titles = snippets.stream().limit(count).map(SearchSnippet::title).toList();
        Map<String, String> extracts = fetchExtracts(titles);

        List<SearchSnippet> enriched = new ArrayList<>();
        for (int i = 0; i < snippets.size(); i++) {
            SearchSnippet snippet = snippets.get(i);
            String extract = extracts.get(snippet.title());
            if (i < count && extract != null && !extract.isBlank()) {
                enriched.add(new SearchSnippet(snippet.title(), snippet.url(), extract));
            } else {
                enriched.add(snippet);
            }
        }
        return enriched;
    }

    private Map<String, String> fetchExtracts(List<String> titles) throws IOException, InterruptedException {
        if (titles.isEmpty()) {
            return Map.of();
        }
        String joinedTitles = titles.stream()
                .map(title -> URLEncoder.encode(title, StandardCharsets.UTF_8))
                .collect(Collectors.joining("%7C"));
        String url = apiBase
                + "?action=query&prop=extracts&explaintext=1&exchars=" + EXTRACT_CHARS
                + "&format=json&origin=*&titles=" + joinedTitles;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "JChat/0.1 (local research bot)")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Wikipedia extract HTTP " + response.statusCode());
        }

        JsonNode pages = MAPPER.readTree(response.body()).path("query").path("pages");
        Map<String, String> extracts = new LinkedHashMap<>();
        pages.fields().forEachRemaining(entry -> {
            JsonNode page = entry.getValue();
            String title = page.path("title").asText("");
            String extract = page.path("extract").asText("");
            if (!title.isBlank() && !extract.isBlank()) {
                extracts.put(title, extract);
            }
        });
        return extracts;
    }

    private List<SearchSnippet> parseSearchResponse(String body) throws IOException {
        JsonNode search = MAPPER.readTree(body).path("query").path("search");
        List<SearchSnippet> snippets = new ArrayList<>();
        if (!search.isArray()) {
            return snippets;
        }
        for (JsonNode item : search) {
            String title = item.path("title").asText("");
            String snippet = stripHtml(item.path("snippet").asText(""));
            String pageUrl = wikiUrl(title);
            snippets.add(new SearchSnippet(title, pageUrl, snippet));
        }
        return snippets;
    }

    private String wikiUrl(String title) {
        String wikiHost = apiBase.replace("https://", "").replace("/w/api.php", "");
        return "https://" + wikiHost + "/wiki/" + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8);
    }

    private static String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", "").trim();
    }

    private static boolean looksLikeOfficeHolderQuery(String query) {
        if (query == null) {
            return false;
        }
        String lower = query.toLowerCase(Locale.ROOT);
        return lower.contains("amtier")
                || lower.contains("kanzler")
                || lower.contains("präsident")
                || lower.contains("prasident")
                || lower.contains("president")
                || lower.contains("regierung")
                || lower.contains("minister")
                || lower.contains("bürgermeister")
                || lower.contains("buergermeister")
                || lower.contains("ceo")
                || lower.contains("vorstand");
    }
}
