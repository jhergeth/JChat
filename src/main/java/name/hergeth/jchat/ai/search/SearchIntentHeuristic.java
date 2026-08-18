package name.hergeth.jchat.ai.search;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class SearchIntentHeuristic {

    private static final Pattern NOT_PATTERN = Pattern.compile(
            "\\b(nicht|nicht mehr|ist falsch|stimmt nicht|unrichtig|fehlt)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CURRENT_PATTERN = Pattern.compile(
            "\\b(aktuell|heute|derzeit|jetzt|momentan|202[4-9])\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern RESEARCH_AGAIN_PATTERN = Pattern.compile(
            "\\b(nochmal|noch\\s+mal|nachschauen|nachschlagen|nachprüfen|nachpruefen|"
                    + "recherchier|schaue\\s+nach|schau\\s+nach|prüf|pruef|guck\\s+nach)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern PUBLIC_FACT_PATTERN = Pattern.compile(
            "\\b(bundeskanzler|kanzler|präsident|president|minister|bürgermeister|"
                    + "hauptstadt|ceo|vorstand|regierung|amt|bekleidet|wetter|preis)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern FOLLOW_UP_START = Pattern.compile(
            "^(und|nd|was\\s+ist\\s+(mit|in)|in\\s+(den|der|die)?)\\s+",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern COUNTRY_HINT = Pattern.compile(
            "\\b(usa|amerika|deutschland|österreich|oesterreich|frankreich|schweiz|"
                    + "vereinigten staaten|vereinigte staaten|uk|england)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern POSSESSIVE_PRONOUN = Pattern.compile(
            "\\b(sein|seine|seinem|seinen|seiner|ihr|ihre|ihrem|ihren|ihrer)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern RELATION_HINT = Pattern.compile(
            "\\b(frau|ehefrau|ehemann|mann|pressesprecher|pressesprecherin|partner|"
                    + "ehepartner|tochter|sohn|kind|vater|mutter)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private SearchIntentHeuristic() {}

    static boolean shouldSearch(String userMessage, List<String> recentUserMessages) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        String lower = normalizeForMatching(userMessage);
        if (matchesPublicFactQuestion(lower)) {
            return true;
        }
        if (looksLikeCountryFollowUp(lower, recentUserMessages)) {
            return true;
        }
        if (looksLikePronounFollowUp(lower, recentUserMessages)) {
            return true;
        }
        if (RESEARCH_AGAIN_PATTERN.matcher(lower).find() && hasPriorPublicFactQuestion(recentUserMessages)) {
            return true;
        }
        return false;
    }

    static String fallbackQuery(String userMessage, List<String> recentUserMessages) {
        if (userMessage == null || userMessage.isBlank()) {
            return "";
        }
        if (RESEARCH_AGAIN_PATTERN.matcher(userMessage.toLowerCase(Locale.ROOT)).find()) {
            for (int i = recentUserMessages.size() - 2; i >= 0; i--) {
                String prior = recentUserMessages.get(i);
                if (matchesPublicFactQuestion(normalizeForMatching(prior))) {
                    return sanitizeQuery(prior);
                }
            }
        }
        if (looksLikeCountryFollowUp(userMessage.toLowerCase(Locale.ROOT), recentUserMessages)) {
            return sanitizeQuery(userMessage);
        }
        return sanitizeQuery(userMessage);
    }

    private static boolean matchesPublicFactQuestion(String lower) {
        if (NOT_PATTERN.matcher(lower).find() && PUBLIC_FACT_PATTERN.matcher(lower).find()) {
            return true;
        }
        if (CURRENT_PATTERN.matcher(lower).find() && PUBLIC_FACT_PATTERN.matcher(lower).find()) {
            return true;
        }
        return lower.contains("wer ist") && PUBLIC_FACT_PATTERN.matcher(lower).find();
    }

    private static boolean looksLikeCountryFollowUp(String lower, List<String> recentUserMessages) {
        if (!COUNTRY_HINT.matcher(lower).find()) {
            return false;
        }
        if (FOLLOW_UP_START.matcher(lower.trim()).find()) {
            return hasPriorPublicFactQuestion(recentUserMessages);
        }
        return lower.length() <= 40 && hasPriorPublicFactQuestion(recentUserMessages);
    }

    private static boolean looksLikePronounFollowUp(String lower, List<String> recentUserMessages) {
        if (!POSSESSIVE_PRONOUN.matcher(lower).find()) {
            return false;
        }
        if (!RELATION_HINT.matcher(lower).find() && !lower.contains("wie heisst") && !lower.contains("wie heißt")) {
            return false;
        }
        return recentUserMessages != null && recentUserMessages.size() > 1;
    }

    private static boolean hasPriorPublicFactQuestion(List<String> recentUserMessages) {
        if (recentUserMessages == null || recentUserMessages.size() <= 1) {
            return false;
        }
        for (int i = recentUserMessages.size() - 2; i >= 0; i--) {
            if (matchesPublicFactQuestion(normalizeForMatching(recentUserMessages.get(i)))) {
                return true;
            }
        }
        String combined = String.join(" ", recentUserMessages.subList(0, recentUserMessages.size() - 1))
                .toLowerCase(Locale.ROOT);
        return PUBLIC_FACT_PATTERN.matcher(combined).find();
    }

    private static String normalizeForMatching(String text) {
        return text.replaceAll("(?i)(?<=[a-zäöüß])\\d+(?=[a-zäöüß])", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String sanitizeQuery(String original) {
        String cleaned = original
                .replaceAll("(?i)\\b(korrektur|stimmt nicht|ist nicht|nicht der|nicht die)\\b", " ")
                .replaceAll("\\?", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() > 80) {
            cleaned = cleaned.substring(0, 80);
        }
        return cleaned.isBlank() ? original.trim() : cleaned;
    }
}
