package name.hergeth.jchat.ai.context;

import java.util.Map;
import java.util.Optional;

public record SessionContextHints(
        Optional<String> acceptLanguage,
        Optional<String> timezone,
        Optional<String> locale,
        Map<String, String> metadata) {

    public SessionContextHints {
        acceptLanguage = acceptLanguage == null ? Optional.empty() : acceptLanguage;
        timezone = timezone == null ? Optional.empty() : timezone;
        locale = locale == null ? Optional.empty() : locale;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static SessionContextHints empty() {
        return new SessionContextHints(Optional.empty(), Optional.empty(), Optional.empty(), Map.of());
    }

    public static SessionContextHints fromHeadersAndMetadata(
            String acceptLanguage,
            String timezoneHeader,
            Map<String, String> metadata) {
        Optional<String> metaLocale = optionalMetadata(metadata, "locale", "language");
        Optional<String> metaTimezone = optionalMetadata(metadata, "timezone", "time_zone", "tz");
        return new SessionContextHints(
                optionalBlank(acceptLanguage),
                firstPresent(optionalBlank(timezoneHeader), metaTimezone),
                metaLocale,
                metadata == null ? Map.of() : Map.copyOf(metadata));
    }

    private static Optional<String> optionalBlank(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private static Optional<String> firstPresent(Optional<String> primary, Optional<String> secondary) {
        return primary.isPresent() ? primary : secondary;
    }

    private static Optional<String> optionalMetadata(Map<String, String> metadata, String... keys) {
        if (metadata == null || metadata.isEmpty()) {
            return Optional.empty();
        }
        for (String key : keys) {
            String value = metadata.get(key);
            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }

    public Optional<String> resolvedLocaleTag() {
        if (locale.isPresent()) {
            return locale;
        }
        return acceptLanguage.flatMap(SessionContextHints::firstLanguageTag);
    }

    private static Optional<String> firstLanguageTag(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return Optional.empty();
        }
        String first = acceptLanguage.split(",")[0].trim();
        if (first.isBlank()) {
            return Optional.empty();
        }
        int qIndex = first.indexOf(';');
        if (qIndex >= 0) {
            first = first.substring(0, qIndex).trim();
        }
        return first.isBlank() ? Optional.empty() : Optional.of(first.replace('_', '-'));
    }
}
