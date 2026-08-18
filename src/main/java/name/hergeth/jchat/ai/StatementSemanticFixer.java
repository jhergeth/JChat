package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class StatementSemanticFixer {

    private static final Set<String> BOOLEAN_OBJECTS = Set.of("true", "false", "ja", "nein", "yes", "no");
    private static final Pattern ORG_MARKER = Pattern.compile(
            "\\b(GmbH|AG|Inc|Ltd|Corp|SE|e\\.V\\.|KG|OHG)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAMEL_CASE = Pattern.compile("([a-zäöüß])([A-Z])");
    private static final Set<String> SOFTWARE_NAMES = Set.of(
            "ollama", "jchat", "docker", "kubernetes");

    private StatementSemanticFixer() {}

    static Statement fix(Statement statement) {
        Statement swapped = swapInvertedArbeitetBei(statement);
        swapped = swapInvertedNutztGpu(swapped);
        swapped = swapInvertedHatGpu(swapped);
        swapped = swapInvertedLaeuftAuf(swapped);
        return canonicalizeEntities(swapped);
    }

    static boolean isInvalid(Statement statement) {
        if (BOOLEAN_OBJECTS.contains(statement.object().toLowerCase(Locale.ROOT).trim())) {
            return true;
        }
        if (statement.object().toLowerCase(Locale.ROOT).contains("amazon alexa")) {
            return true;
        }
        String predicate = statement.predicate();
        if ("arbeitet_bei".equals(predicate) && isSoftwareName(statement.subject()) && !looksLikeOrg(statement.object())) {
            return true;
        }
        if ("arbeitet_bei".equals(predicate) && looksLikeOrg(statement.subject()) && !looksLikeOrg(statement.object())) {
            return true;
        }
        if (("hat_gpu".equals(predicate) || "nutzt_gpu".equals(predicate)) && looksLikeOrg(statement.subject())) {
            return true;
        }
        if (("hat_gpu".equals(predicate) || "nutzt_gpu".equals(predicate)) && looksLikeCpu(statement.object())) {
            return true;
        }
        if ("laeuft_auf".equals(predicate) && looksLikeHardware(statement.subject())) {
            return true;
        }
        return false;
    }

    private static Statement swapInvertedArbeitetBei(Statement statement) {
        if (!"arbeitet_bei".equals(statement.predicate())) {
            return statement;
        }
        if (looksLikeOrg(statement.subject()) && !looksLikeOrg(statement.object())) {
            return swap(statement);
        }
        return statement;
    }

    private static Statement swapInvertedNutztGpu(Statement statement) {
        if (!"nutzt_gpu".equals(statement.predicate())) {
            return statement;
        }
        if (looksLikeHardware(statement.subject()) && !looksLikeHardware(statement.object())) {
            return swap(statement);
        }
        return statement;
    }

    private static Statement swapInvertedHatGpu(Statement statement) {
        if (!"hat_gpu".equals(statement.predicate())) {
            return statement;
        }
        if (looksLikeHardware(statement.subject()) && looksLikeServer(statement.object())) {
            return swap(statement);
        }
        return statement;
    }

    private static Statement swapInvertedLaeuftAuf(Statement statement) {
        if (!"laeuft_auf".equals(statement.predicate())) {
            return statement;
        }
        if (looksLikeServer(statement.subject()) && isSoftwareName(statement.object())) {
            return swap(statement);
        }
        return statement;
    }

    private static Statement swap(Statement statement) {
        return new Statement(
                statement.object(),
                statement.predicate(),
                statement.subject(),
                statement.conversationId(),
                statement.turnId(),
                statement.createdAt(),
                statement.source());
    }

    private static Statement canonicalizeEntities(Statement statement) {
        return new Statement(
                canonicalName(statement.subject()),
                statement.predicate(),
                canonicalName(statement.object()),
                statement.conversationId(),
                statement.turnId(),
                statement.createdAt(),
                statement.source());
    }

    private static String canonicalName(String value) {
        String trimmed = stripRolePrefix(value).trim();
        if ("am5".equalsIgnoreCase(trimmed)) {
            return "AM5";
        }
        String compact = trimmed.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        if ("nvidiap100".equals(compact)) {
            return "Nvidia P100";
        }
        if ("lokalerserver".equals(compact)) {
            return "lokaler Server";
        }
        if (CAMEL_CASE.matcher(trimmed).find()) {
            trimmed = CAMEL_CASE.matcher(trimmed).replaceAll("$1 $2");
        }
        return trimmed;
    }

    private static String stripRolePrefix(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("user:")) {
            return value.substring(5).trim();
        }
        if (lower.startsWith("assistant:")) {
            return value.substring(10).trim();
        }
        return value;
    }

    private static boolean looksLikeOrg(String value) {
        return ORG_MARKER.matcher(value).find();
    }

    private static boolean looksLikeServer(String value) {
        return "AM5".equalsIgnoreCase(value.trim())
                || value.toLowerCase(Locale.ROOT).contains("server");
    }

    private static boolean isSoftwareName(String name) {
        return SOFTWARE_NAMES.contains(name.toLowerCase(Locale.ROOT).trim());
    }

    private static boolean looksLikeHardware(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("nvidia") || lower.contains("p100") || lower.contains("gpu")
                || lower.contains("geforce") || lower.contains("rtx") || lower.contains("gtx");
    }

    private static boolean looksLikeCpu(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("ryzen") || lower.contains("intel core") || lower.contains("xeon")
                || lower.contains("threadripper") || lower.contains("epyc");
    }
}
