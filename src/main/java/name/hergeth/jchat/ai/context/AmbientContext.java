package name.hergeth.jchat.ai.context;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;

public record AmbientContext(
        ZonedDateTime localDateTime,
        ZoneId timezone,
        String localeTag,
        Optional<String> country,
        DayPhase dayPhase) {

    public AmbientContext {
        localeTag = localeTag == null ? "" : localeTag.trim();
        country = country == null ? Optional.empty() : country;
    }

    public String timezoneId() {
        return timezone.getId();
    }

    public String languageTag() {
        return localeTag;
    }

    public String countryOrUnknown() {
        return country.orElse("unbekannt");
    }

    public String dayPhaseLabel() {
        return dayPhase.labelDe();
    }

    public String formattedLocalDateTime(Locale displayLocale) {
        return localDateTime.format(java.time.format.DateTimeFormatter
                .ofPattern("EEEE, d. MMMM yyyy, HH:mm", displayLocale));
    }
}
