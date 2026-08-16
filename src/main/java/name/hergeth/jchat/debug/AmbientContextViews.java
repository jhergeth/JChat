package name.hergeth.jchat.debug;

import name.hergeth.jchat.ai.context.AmbientContext;

public final class AmbientContextViews {

    private AmbientContextViews() {}

    public static AmbientContextView from(AmbientContext context) {
        if (context == null) {
            return null;
        }
        return new AmbientContextView(
                context.localDateTime().toString(),
                context.timezoneId(),
                context.languageTag(),
                context.countryOrUnknown(),
                context.dayPhaseLabel());
    }
}
