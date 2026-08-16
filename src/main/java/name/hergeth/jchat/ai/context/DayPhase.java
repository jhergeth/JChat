package name.hergeth.jchat.ai.context;

enum DayPhase {
    MORNING("Morgen"),
    DAY("Tag"),
    EVENING("Abend"),
    NIGHT("Nacht");

    private final String labelDe;

    DayPhase(String labelDe) {
        this.labelDe = labelDe;
    }

    String labelDe() {
        return labelDe;
    }

    static DayPhase fromHour(int hour) {
        if (hour >= 5 && hour < 11) {
            return MORNING;
        }
        if (hour >= 11 && hour < 17) {
            return DAY;
        }
        if (hour >= 17 && hour < 22) {
            return EVENING;
        }
        return NIGHT;
    }
}
