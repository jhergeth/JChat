package name.hergeth.jchat.ai.search;

import name.hergeth.jchat.ai.model.FactSource;
import name.hergeth.jchat.ai.model.Statement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WikiOfficeHolderExtractor {

    private static final Pattern CURRENT_OFFICE_HOLDER = Pattern.compile(
            "Aktueller Amtsinhaber ist seit dem .+? ([A-ZÄÖÜ][\\p{L}\\-]+(?:\\s+[A-ZÄÖÜ][\\p{L}\\-]+)+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LEADERSHIP = Pattern.compile(
            "unter der Führung des ([\\p{L}\\s\\-]+?) ([A-ZÄÖÜ][\\p{L}\\-]+(?:\\s+[A-ZÄÖÜ][\\p{L}\\-]+)+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LEADERSHIP_FIRST_NAME = Pattern.compile(
            "unter der Führung des ([\\p{L}\\s\\-]+?) ([A-ZÄÖÜ][\\p{L}\\-]+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CURRENT_SINCE = Pattern.compile(
            "ist seit dem .+? der (?:\\d+\\. )?(?:\\w+\\s+)?(.+?)(?:\\.|,|\\s+Er\\s)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern WIKI_PERSON_INTRO = Pattern.compile(
            "([A-ZÄÖÜ][\\p{L}\\-]+\\s+[A-ZÄÖÜ][\\p{L}\\-]+)\\s*\\(\\*");
    private static final Pattern KABINETT_TITLE = Pattern.compile("^Kabinett\\s+(.+)$", Pattern.UNICODE_CASE);

    private WikiOfficeHolderExtractor() {}

    static List<Statement> extractOfficeFacts(List<SearchSnippet> snippets, String conversationId, String turnId) {
        List<Statement> facts = new ArrayList<>();
        Instant now = Instant.now();

        for (SearchSnippet snippet : snippets) {
            extractOfficeHolder(snippet).ifPresent(holder -> {
                if (facts.stream().noneMatch(f -> f.subject().equalsIgnoreCase(holder.person()))) {
                    facts.add(new Statement(
                            holder.person(),
                            "ist_amtsinhaber_von",
                            holder.jurisdiction().isBlank() ? holder.office() : holder.jurisdiction(),
                            conversationId,
                            turnId,
                            now,
                            FactSource.WEB_SEARCH));
                }
            });
        }
        return facts;
    }

    static Optional<OfficeHolderFact> extractOfficeHolder(SearchSnippet snippet) {
        String text = snippet.snippet();
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher officeHolder = CURRENT_OFFICE_HOLDER.matcher(text);
        if (officeHolder.find()) {
            return toOfficeHolderFact(
                    officeHolder.group(1).trim(),
                    officeFromTitle(snippet.title()).orElse("Amtsinhaber"),
                    jurisdictionFromText(text));
        }

        Matcher leadership = LEADERSHIP.matcher(text);
        if (leadership.find()) {
            return toOfficeHolderFact(
                    leadership.group(2).trim(),
                    normalizeOfficeRole(leadership.group(1).trim()),
                    jurisdictionFromText(text));
        }

        Optional<OfficeHolderFact> fromKabinettTitle = fromKabinettTitle(snippet, text);
        if (fromKabinettTitle.isPresent()) {
            return fromKabinettTitle;
        }

        Matcher since = CURRENT_SINCE.matcher(text);
        if (since.find()) {
            OfficePhrase phrase = parseOfficePhrase(since.group(1).trim());
            String jurisdiction = jurisdictionFromText(text);
            if (jurisdiction.isBlank()) {
                jurisdiction = phrase.jurisdiction();
            }
            String finalJurisdiction = jurisdiction;
            return nameFromSnippet(snippet, text)
                    .flatMap(name -> toOfficeHolderFact(name, phrase.office(), finalJurisdiction));
        }
        return Optional.empty();
    }

    private static Optional<OfficeHolderFact> fromKabinettTitle(SearchSnippet snippet, String text) {
        if (snippet.title() == null || snippet.title().isBlank()) {
            return Optional.empty();
        }
        Matcher title = KABINETT_TITLE.matcher(snippet.title().trim());
        if (!title.matches()) {
            return Optional.empty();
        }
        String surname = title.group(1).trim();
        Matcher firstName = LEADERSHIP_FIRST_NAME.matcher(text);
        if (!firstName.find()) {
            return Optional.empty();
        }
        String role = normalizeOfficeRole(firstName.group(1).trim());
        String name = firstName.group(2).trim() + " " + surname;
        return toOfficeHolderFact(name, role, jurisdictionFromText(text));
    }

    private static Optional<OfficeHolderFact> toOfficeHolderFact(String person, String office, String jurisdiction) {
        String cleanedPerson = person.trim().replaceAll("\\s+", " ");
        if (!isPlausiblePersonName(cleanedPerson)) {
            return Optional.empty();
        }
        return Optional.of(new OfficeHolderFact(
                cleanedPerson,
                office == null ? "" : office.trim(),
                jurisdiction == null ? "" : jurisdiction.trim()));
    }

    private static Optional<String> nameFromSnippet(SearchSnippet snippet, String text) {
        if (snippet.title() != null && !snippet.title().isBlank() && looksLikePersonTitle(snippet.title())) {
            return Optional.of(snippet.title().trim());
        }
        Matcher intro = WIKI_PERSON_INTRO.matcher(text);
        if (intro.find()) {
            return Optional.of(intro.group(1).trim());
        }
        return Optional.empty();
    }

    static boolean isPlausiblePersonName(String name) {
        return name != null && name.contains(" ") && name.length() >= 4;
    }

    private static Optional<String> officeFromTitle(String title) {
        if (title == null || title.isBlank() || looksLikePersonTitle(title)) {
            return Optional.empty();
        }
        return Optional.of(title.trim());
    }

    private static String normalizeOfficeRole(String role) {
        String cleaned = role.trim().replaceAll("\\s+", " ");
        if (cleaned.endsWith("s") && cleaned.length() > 4) {
            return cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    private static OfficePhrase parseOfficePhrase(String phrase) {
        String cleaned = phrase.trim().replaceAll("\\s+", " ");
        int vonIndex = cleaned.lastIndexOf(" des ");
        if (vonIndex > 0) {
            return new OfficePhrase(
                    cleaned.substring(0, vonIndex).trim(),
                    cleaned.substring(vonIndex + 5).trim());
        }
        int derIndex = cleaned.lastIndexOf(" der ");
        if (derIndex > 0) {
            return new OfficePhrase(
                    cleaned.substring(0, derIndex).trim(),
                    cleaned.substring(derIndex + 5).trim());
        }
        int vonSpaceIndex = cleaned.lastIndexOf(" von ");
        if (vonSpaceIndex > 0) {
            return new OfficePhrase(
                    cleaned.substring(0, vonSpaceIndex).trim(),
                    cleaned.substring(vonSpaceIndex + 5).trim());
        }
        return new OfficePhrase(cleaned, "");
    }

    private static String jurisdictionFromText(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("bundesrepublik deutschland")) {
            return "Bundesrepublik Deutschland";
        }
        if (lower.contains("vereinigten staaten von amerika") || lower.contains("vereinigten staaten")) {
            return "Vereinigte Staaten von Amerika";
        }
        if (lower.contains("österreich")) {
            return "Österreich";
        }
        if (lower.contains("vereinigten königreich") || lower.contains("vereinigten koenigreich")
                || lower.contains("großbritannien") || lower.contains("grossbritannien")
                || lower.contains(" united kingdom")) {
            return "Vereinigtes Königreich";
        }
        if (lower.contains("frankreich")) {
            return "Frankreich";
        }
        if (lower.contains("deutschland")) {
            return "Deutschland";
        }
        return "";
    }

    private static boolean looksLikePersonTitle(String title) {
        String lower = title.toLowerCase(Locale.ROOT);
        return !lower.contains("kabinett")
                && !lower.contains("liste ")
                && !lower.contains("verfassung")
                && !lower.contains("nachfolge")
                && title.contains(" ");
    }

    record OfficeHolderFact(String person, String office, String jurisdiction) {}

    private record OfficePhrase(String office, String jurisdiction) {}
}
