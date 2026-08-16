package name.hergeth.jchat.debug;

public record AmbientContextView(
        String localDateTime,
        String timezone,
        String locale,
        String country,
        String dayPhase) {}
