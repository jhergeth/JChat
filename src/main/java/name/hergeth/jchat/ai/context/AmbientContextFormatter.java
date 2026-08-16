package name.hergeth.jchat.ai.context;

import java.util.Locale;

public final class AmbientContextFormatter {

    private static final Locale DISPLAY = Locale.GERMAN;

    private AmbientContextFormatter() {}

    public static String format(AmbientContext context) {
        if (context == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nAktueller Kontext:\n");
        sb.append("- Es ist ")
                .append(context.formattedLocalDateTime(DISPLAY))
                .append(" (")
                .append(context.timezoneId())
                .append("), ")
                .append(context.dayPhaseLabel())
                .append(".\n");
        if (!context.languageTag().isBlank()) {
            sb.append("- Nutzer-Locale: ").append(context.languageTag());
            if (context.country().isPresent()) {
                sb.append(" → vermutetes Land: ").append(context.country().get());
            }
            sb.append(".\n");
        } else if (context.country().isPresent()) {
            sb.append("- Vermutetes Land: ").append(context.country().get()).append(".\n");
        }
        return sb.toString().trim();
    }

    public static String formatForPlanner(AmbientContext context) {
        if (context == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Kontext: ");
        sb.append(context.localDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")));
        sb.append(" (").append(context.timezoneId()).append(")");
        if (context.country().isPresent()) {
            sb.append(", vermutetes Land ").append(context.country().get());
        }
        if (!context.languageTag().isBlank()) {
            sb.append(" (").append(context.languageTag()).append(")");
        }
        sb.append('.');
        return sb.toString();
    }
}
