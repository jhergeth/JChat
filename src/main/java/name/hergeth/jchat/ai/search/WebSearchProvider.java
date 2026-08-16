package name.hergeth.jchat.ai.search;

import java.util.List;

public interface WebSearchProvider {
    List<SearchSnippet> search(String query, int maxResults);
    boolean isConfigured();
}
