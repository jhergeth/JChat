package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.List;

final class KnowledgeStorePromptFormatter {

    private KnowledgeStorePromptFormatter() {}

    static String format(List<Statement> statements) {
        if (statements == null || statements.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nAktuelle Recherche (aus gespeichertem Wissen — vor Trainingswissen vertrauen):\n");
        for (Statement statement : statements) {
            sb.append("- ").append(naturalize(statement)).append('\n');
        }
        return sb.toString().trim();
    }

    private static String naturalize(Statement statement) {
        String predicate = statement.predicate() == null ? "" : statement.predicate().replace('_', ' ');
        String subject = statement.subject() == null ? "" : statement.subject().trim();
        String object = statement.object() == null ? "" : statement.object().trim();

        if (predicate.contains("hauptstadt")) {
            return subject + " ist Hauptstadt von " + object + ".";
        }
        if (predicate.contains("amtsinhaber")
                || predicate.contains("bundeskanzler")
                || predicate.contains("präsident")
                || predicate.contains("prasident")
                || predicate.contains("president")
                || predicate.contains("position")) {
            if (object.isBlank()) {
                return subject + " ist " + predicate + ".";
            }
            return subject + " ist " + predicate + " (" + object + ").";
        }
        if (predicate.contains("pressesprecher")) {
            return subject + " hat als Pressesprecher(in) " + object + ".";
        }
        if (predicate.contains("ehepartner") || predicate.contains("frau") || predicate.contains("gatte")) {
            return "Partner(in) von " + subject + " ist " + object + ".";
        }
        if (object.isBlank()) {
            return subject + " " + predicate + ".";
        }
        return subject + " " + predicate + " " + object + ".";
    }
}
