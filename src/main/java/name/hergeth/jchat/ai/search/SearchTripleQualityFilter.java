package name.hergeth.jchat.ai.search;

import name.hergeth.jchat.ai.model.Statement;

import java.util.Locale;
import java.util.regex.Pattern;

final class SearchTripleQualityFilter {

    private static final Pattern INSTITUTIONAL_SUBJECT = Pattern.compile(
            "\\b(verfassung|nachfolge|liste der|repräsentantenhaus|reprasentantenhaus|"
                    + "gewählter|gewahlter|staatsverschuldung|wahlleutekollegium|"
                    + "electoral college|ministerium|parlament|senat)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private SearchTripleQualityFilter() {}

    static boolean isLowQuality(Statement statement) {
        String subject = statement.subject().toLowerCase(Locale.ROOT);
        String predicate = statement.predicate().toLowerCase(Locale.ROOT);
        String object = statement.object().toLowerCase(Locale.ROOT);

        if (isOfficeRolePredicate(predicate)
                && !WikiOfficeHolderExtractor.isPlausiblePersonName(statement.subject())) {
            return true;
        }
        if (isOfficeRolePredicate(predicate) && isRoleTermAsSubject(subject)) {
            return true;
        }
        if (isInstitutionalSubject(subject)) {
            return true;
        }
        if (isMetaPredicate(predicate)) {
            return true;
        }
        if (isVagueObject(statement.object())) {
            return true;
        }
        if (isOfficeRolePredicate(predicate)
                && !predicate.contains("_von")
                && isBareCountryObject(object)) {
            return true;
        }
        if (subject.equals(object)) {
            return true;
        }
        return false;
    }

    static boolean isOfficeRolePredicate(String predicate) {
        if (predicate == null || predicate.isBlank()) {
            return false;
        }
        String lower = predicate.toLowerCase(Locale.ROOT);
        return lower.contains("amtsinhaber")
                || lower.contains("bundeskanzler")
                || lower.contains("kanzler")
                || lower.contains("präsident")
                || lower.contains("prasident")
                || lower.contains("president")
                || lower.contains("ministerpräsident")
                || lower.contains("ministerpraesident")
                || lower.contains("premierminister")
                || lower.contains("regierungschef")
                || lower.contains("bekleidet")
                || lower.contains("amt_innehaber")
                || lower.contains("amtinhaber");
    }

    static boolean isRoleTermAsSubject(String subject) {
        return subject.contains("bundeskanzler")
                || subject.contains("präsident der")
                || subject.contains("prasident der")
                || subject.contains("ministerpräsident")
                || subject.contains("ministerpraesident")
                || subject.contains("premierminister");
    }

    private static boolean isInstitutionalSubject(String subject) {
        return INSTITUTIONAL_SUBJECT.matcher(subject).find()
                || subject.startsWith("vereinigte staaten")
                || subject.startsWith("vereinigten staaten");
    }

    private static boolean isMetaPredicate(String predicate) {
        return predicate.contains("regeltdienachfolge")
                || predicate.contains("wirdnachfolge")
                || predicate.contains("habenprasidentenamt")
                || predicate.contains("habenkanzleramt")
                || predicate.contains("istdesignierter")
                || predicate.contains("istvizeprasident")
                || predicate.contains("istvizepräsident")
                || predicate.contains("hatsprecher")
                || predicate.contains("regierendurch")
                || predicate.contains("istvize")
                || predicate.contains("istdesigniert");
    }

    private static boolean isVagueObject(String object) {
        String lower = object.toLowerCase(Locale.ROOT).trim();
        return lower.equals("keine angabe")
                || lower.equals("(unbekannt)")
                || lower.equals("unbekannt")
                || lower.equals("n/a")
                || lower.contains("keinefesteninhaber")
                || lower.contains("keine festen inhaber");
    }

    private static boolean isBareCountryObject(String object) {
        String lower = object.toLowerCase(Locale.ROOT).trim();
        return lower.equals("deutschland")
                || lower.equals("österreich")
                || lower.equals("oesterreich")
                || lower.equals("vereinigte staaten")
                || lower.equals("usa")
                || lower.equals("amerika")
                || lower.equals("frankreich")
                || lower.equals("uk");
    }
}
