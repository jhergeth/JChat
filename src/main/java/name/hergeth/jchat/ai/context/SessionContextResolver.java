package name.hergeth.jchat.ai.context;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;

@Singleton
public class SessionContextResolver {

    private final ZoneId defaultTimezone;
    private final String defaultLocaleTag;

    public SessionContextResolver(
            @Value("${app.context.default-timezone:Europe/Berlin}") String defaultTimezone,
            @Value("${app.context.default-locale:de-DE}") String defaultLocale) {
        this.defaultTimezone = ZoneId.of(defaultTimezone);
        this.defaultLocaleTag = defaultLocale == null ? "de-DE" : defaultLocale.trim();
    }

    public AmbientContext resolve(SessionContextHints hints) {
        SessionContextHints safeHints = hints == null ? SessionContextHints.empty() : hints;
        ZoneId timezone = resolveTimezone(safeHints);
        ZonedDateTime now = ZonedDateTime.now(timezone);
        String localeTag = safeHints.resolvedLocaleTag().orElse(defaultLocaleTag);
        Optional<String> country = countryFromLocale(localeTag);
        DayPhase dayPhase = DayPhase.fromHour(now.getHour());
        return new AmbientContext(now, timezone, localeTag, country, dayPhase);
    }

    private ZoneId resolveTimezone(SessionContextHints hints) {
        Optional<String> candidate = hints.timezone().filter(value -> !value.isBlank());
        if (candidate.isEmpty()) {
            return defaultTimezone;
        }
        try {
            return ZoneId.of(candidate.get().trim());
        } catch (Exception e) {
            return defaultTimezone;
        }
    }

    static Optional<String> countryFromLocale(String languageTag) {
        if (languageTag == null || languageTag.isBlank()) {
            return Optional.empty();
        }
        Locale locale = Locale.forLanguageTag(languageTag.trim().replace('_', '-'));
        if (!locale.getCountry().isBlank()) {
            return Optional.of(locale.getDisplayCountry(Locale.GERMAN));
        }
        return switch (locale.getLanguage().toLowerCase(Locale.ROOT)) {
            case "de" -> Optional.of("Deutschland");
            case "en" -> Optional.of("Vereinigte Staaten");
            case "fr" -> Optional.of("Frankreich");
            case "it" -> Optional.of("Italien");
            case "es" -> Optional.of("Spanien");
            default -> Optional.empty();
        };
    }
}
