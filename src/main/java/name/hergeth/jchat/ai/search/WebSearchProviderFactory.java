package name.hergeth.jchat.ai.search;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class WebSearchProviderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(WebSearchProviderFactory.class);

    private final CompositeWebSearchProvider composite;

    @Inject
    public WebSearchProviderFactory(CompositeWebSearchProvider composite) {
        this.composite = composite;
    }

    public Optional<WebSearchProvider> activeProvider() {
        if (!composite.isConfigured()) {
            LOG.warn("No search provider configured (Wikipedia disabled and xnsearch URL missing)");
            return Optional.empty();
        }
        return Optional.of(composite);
    }
}
