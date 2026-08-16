package name.hergeth.jchat.ai.context;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionContextResolverTest {

    private final SessionContextResolver resolver = new SessionContextResolver("Europe/Berlin", "de-DE");

    @Test
    void mapsLocaleWithCountryToGermanCountryName() {
        Optional<String> country = SessionContextResolver.countryFromLocale("de-AT");
        assertTrue(country.isPresent());
        assertEquals("Österreich", country.get());
    }

    @Test
    void resolvesTimezoneFromHeader() {
        SessionContextHints hints = SessionContextHints.fromHeadersAndMetadata(
                "de-AT", "Europe/Vienna", java.util.Map.of());
        AmbientContext context = resolver.resolve(hints);

        assertEquals("Europe/Vienna", context.timezoneId());
        assertEquals("de-AT", context.languageTag());
        assertEquals("Österreich", context.country().orElse(""));
    }

    @Test
    void fallsBackToDefaultsWhenHeadersMissing() {
        AmbientContext context = resolver.resolve(SessionContextHints.empty());

        assertEquals("Europe/Berlin", context.timezoneId());
        assertEquals("de-DE", context.languageTag());
        assertEquals("Deutschland", context.country().orElse(""));
    }

    @Test
    void usesMetadataTimezoneAndLocale() {
        SessionContextHints hints = SessionContextHints.fromHeadersAndMetadata(
                "", "",
                java.util.Map.of("timezone", "Europe/Vienna", "locale", "de-AT"));
        AmbientContext context = resolver.resolve(hints);

        assertEquals("Europe/Vienna", context.timezoneId());
        assertEquals("de-AT", context.languageTag());
    }
}

class AmbientContextFormatterTest {

    @Test
    void formatsSystemPromptBlock() {
        AmbientContext context = new AmbientContext(
                ZonedDateTime.of(2026, 7, 31, 16, 12, 0, 0, ZoneId.of("Europe/Berlin")),
                ZoneId.of("Europe/Berlin"),
                "de-AT",
                Optional.of("Österreich"),
                DayPhase.DAY);

        String formatted = AmbientContextFormatter.format(context);

        assertTrue(formatted.contains("Aktueller Kontext:"));
        assertTrue(formatted.contains("Europe/Berlin"));
        assertTrue(formatted.contains("de-AT"));
        assertTrue(formatted.contains("Österreich"));
        assertTrue(formatted.contains("Tag"));
    }

    @Test
    void formatsPlannerContextLine() {
        AmbientContext context = new AmbientContext(
                ZonedDateTime.of(2026, 7, 31, 16, 12, 0, 0, ZoneId.of("Europe/Berlin")),
                ZoneId.of("Europe/Berlin"),
                "de-AT",
                Optional.of("Österreich"),
                DayPhase.EVENING);

        String formatted = AmbientContextFormatter.formatForPlanner(context);

        assertTrue(formatted.startsWith("Kontext:"));
        assertTrue(formatted.contains("31.07.2026"));
        assertTrue(formatted.contains("Österreich"));
    }
}

class DayPhaseTest {

    @Test
    void classifiesDayPhases() {
        assertEquals(DayPhase.MORNING, DayPhase.fromHour(8));
        assertEquals(DayPhase.DAY, DayPhase.fromHour(14));
        assertEquals(DayPhase.EVENING, DayPhase.fromHour(19));
        assertEquals(DayPhase.NIGHT, DayPhase.fromHour(2));
    }
}
