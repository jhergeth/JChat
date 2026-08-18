package name.hergeth.jchat.ai;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes predicate strings for comparison and storage.
 */
public final class PredicateNormalizer {

    private static final Map<String, String> PREDICATE_ALIASES = Map.ofEntries(
            Map.entry("arbeite_bei", "arbeitet_bei"),
            Map.entry("arbeitetbei", "arbeitet_bei"),
            Map.entry("arbeitet_im", "arbeitet_bei"),
            Map.entry("arbeitet_im_unternehmen", "arbeitet_bei"),
            Map.entry("wohntin", "wohnt_in"),
            Map.entry("lebtin", "wohnt_in"),
            Map.entry("lieblingsfarbe_ist", "lieblingsfarbe"),
            Map.entry("lieblingsfarbe_verwendet", "lieblingsfarbe"),
            Map.entry("nutzt_plattform", "nutzt"),
            Map.entry("nutzt_system", "nutzt"),
            Map.entry("laeuftauf", "laeuft_auf"),
            Map.entry("runson", "laeuft_auf"),
            Map.entry("runs_on", "laeuft_auf"),
            Map.entry("uses", "nutzt"),
            Map.entry("use", "nutzt"),
            Map.entry("hasgpus", "hat_gpu"),
            Map.entry("has_gpus", "hat_gpu"),
            Map.entry("has_gpu", "hat_gpu"),
            Map.entry("hatgpu", "hat_gpu"),
            Map.entry("nutztgpu", "nutzt_gpu"),
            Map.entry("usedfor", "nutzt_gpu"),
            Map.entry("used_for", "nutzt_gpu"),
            Map.entry("verwendet_auf_am5", "laeuft_auf"),
            Map.entry("verwendet_auf", "laeuft_auf"),
            Map.entry("projektname", "name"),
            Map.entry("hat_neuer_name", "name"),
            Map.entry("ist_name", "name"),
            Map.entry("heisst", "name"));

    private static final Pattern CAMEL_CASE = Pattern.compile("([a-z])([A-Z])");
    private static final Pattern GLUED_PREP = Pattern.compile(
            "^([a-zäöüß]+?)(in|bei|auf|mit|an|von|fuer|fur|im|am|zum|zur)$");

    private PredicateNormalizer() {}

    public static String normalize(String value) {
        String cleaned = clean(value);
        cleaned = CAMEL_CASE.matcher(cleaned).replaceAll("$1_$2");
        cleaned = cleaned.toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replaceAll("_+", "_")
                .replaceAll("[^a-z0-9_äöüß]", "");
        cleaned = splitGluedPreposition(cleaned);
        return PREDICATE_ALIASES.getOrDefault(cleaned, cleaned);
    }

    private static String splitGluedPreposition(String predicate) {
        Matcher matcher = GLUED_PREP.matcher(predicate);
        if (matcher.matches() && matcher.group(1).length() >= 3) {
            return matcher.group(1) + "_" + matcher.group(2);
        }
        return predicate;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
