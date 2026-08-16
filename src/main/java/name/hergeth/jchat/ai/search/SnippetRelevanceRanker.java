package name.hergeth.jchat.ai.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class SnippetRelevanceRanker {

    private static final Set<String> STOP_WORDS = Set.of(
            "ich", "der", "die", "das", "ein", "und", "ist", "wer", "was", "wie", "wo",
            "von", "fuer", "fur", "bei", "mit", "auf", "den", "dem", "des", "the", "and", "for");

    private static final Pattern PERSON_NAME = Pattern.compile(
            "[A-ZÄÖÜ][\\p{L}\\-]+\\s+[A-ZÄÖÜ][\\p{L}\\-]+");
    private static final Set<String> META_TERMS = Set.of(
            "verfassung", "nachfolge", "liste der", "staatsverschuldung",
            "gewählter", "gewahlter", "präsidentschaftswahl", "prasidentschaftswahl",
            "nachfolge des", "wahlleutekollegium", "electoral college");
    private static final Set<String> TEMPORAL_TERMS = Set.of(
            "aktueller amtsinhaber", "amtsinhaber", "amtierend", "seit dem",
            "ist derzeit", "ist seit", "unter der führung", "unter der fuhrung");

    private SnippetRelevanceRanker() {}

    static List<SearchSnippet> rank(List<SearchSnippet> snippets, String query) {
        if (snippets == null || snippets.size() <= 1) {
            return snippets == null ? List.of() : snippets;
        }
        Set<String> terms = termsFrom(query);
        return snippets.stream()
                .sorted(Comparator.comparingInt((SearchSnippet snippet) -> scoreSnippet(snippet, terms)).reversed())
                .toList();
    }

    static String bestSentences(String text, String query, int maxSentences) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Set<String> terms = termsFrom(query);
        List<ScoredSentence> scored = new ArrayList<>();
        for (String sentence : splitSentences(text)) {
            if (sentence.isBlank()) {
                continue;
            }
            scored.add(new ScoredSentence(sentence.trim(), scoreSentence(sentence, terms)));
        }
        scored.sort(Comparator.comparingInt(ScoredSentence::score).reversed());
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (ScoredSentence sentence : scored) {
            if (sentence.score() <= 0) {
                continue;
            }
            if (count > 0) {
                sb.append(' ');
            }
            sb.append(sentence.text());
            count++;
            if (count >= maxSentences) {
                break;
            }
        }
        if (sb.length() > 0) {
            return sb.toString().trim();
        }
        return text.length() <= 400 ? text.trim() : text.substring(0, 400).trim() + "...";
    }

    private static int scoreSnippet(SearchSnippet snippet, Set<String> terms) {
        int score = scoreText(snippet.snippet(), terms);
        String title = snippet.title() == null ? "" : snippet.title().toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (title.contains(term)) {
                score += 2;
            }
        }
        if (WikiOfficeHolderExtractor.extractOfficeHolder(snippet).isPresent()) {
            score += 15;
        }
        return score;
    }

    private static int scoreSentence(String sentence, Set<String> terms) {
        return scoreText(sentence, terms);
    }

    private static int scoreText(String text, Set<String> terms) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String temporal : TEMPORAL_TERMS) {
            if (lower.contains(temporal)) {
                score += 12;
            }
        }
        for (String term : terms) {
            if (lower.contains(term)) {
                score += 3;
            }
        }
        if (PERSON_NAME.matcher(text).find()) {
            score += 4;
        }
        for (String meta : META_TERMS) {
            if (lower.contains(meta)) {
                score -= 8;
            }
        }
        return score;
    }

    private static List<String> splitSentences(String text) {
        return List.of(text.split("(?<!\\d)\\.(?=\\s+[A-ZÄÖÜ])"));
    }

    private record ScoredSentence(String text, int score) {}

    private static Set<String> termsFrom(String query) {
        if (query == null || query.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^a-z0-9äöüß]+"))
                .filter(term -> term.length() >= 2)
                .filter(term -> !STOP_WORDS.contains(term))
                .collect(Collectors.toSet());
    }
}
