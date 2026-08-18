package name.hergeth.jchat.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import name.hergeth.jchat.openai.dto.ChatMessage;

import java.io.IOException;
import java.util.List;

/**
 * Second validation stage: asks JChat (meta/check LLM) whether an expected fact is present in the store.
 */
public final class SemanticTripleValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CONVERSATION_ID = "__scenario-semantic-validate__";

    private static final String INSTRUCTIONS = """
            Du prüfst, ob ein erwarteter Fakt im Knowledge Store semantisch enthalten ist.
            Antworte ausschließlich mit JSON in einer Zeile, ohne Markdown:
            {"match":true|false,"matchedTriple":"subject | predicate | object oder leer","reason":"kurz"}
            match=true nur wenn subject, predicate und object inhaltlich zum erwarteten Fakt passen.
            Kleine Formulierungsunterschiede, Vollständigkeit von Namen und Synonyme bei Predikaten sind erlaubt.
            Leerzeichen in Namen (QueryHub vs Query Hub) und Schreibvarianten (PostgreSQL vs Postgre SQL) gelten als gleich.
            """;

    private final JChatHttpClient client;

    public SemanticTripleValidator(JChatHttpClient client) {
        this.client = client;
    }

    public boolean matches(List<StatementSnapshot> store, TripleExpectation expected)
            throws IOException, InterruptedException {
        String storeJson = MAPPER.writeValueAsString(store);
        // Meta-requests strip system messages (MetaRequestMessages.passthrough) — instructions belong in user text.
        String userPrompt = """
                ### Task: scenario_triple_match

                %s

                Erwarteter Fakt:
                subject: %s
                predicate: %s
                object: %s

                Knowledge Store (JSON):
                %s
                """.formatted(
                INSTRUCTIONS,
                expected.subject(),
                expected.predicate(),
                expected.object(),
                storeJson);

        String response = client.chat(
                CONVERSATION_ID,
                List.of(new ChatMessage("user", userPrompt)));

        boolean matched = parseMatch(response);
        if (!matched) {
            System.err.printf("  semantic no-match: %s (LLM: %s)%n",
                    formatTriple(expected), truncate(response, 120));
        }
        return matched;
    }

    static boolean parseMatch(String response) {
        if (response == null || response.isBlank()) {
            return false;
        }
        try {
            String json = extractJson(response);
            JsonNode node = MAPPER.readTree(json);
            JsonNode match = node.get("match");
            return match != null && match.asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    private static String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    static String formatTriple(TripleExpectation triple) {
        return triple.subject() + " | " + triple.predicate() + " | " + triple.object();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').trim();
        return oneLine.length() <= maxLength ? oneLine : oneLine.substring(0, maxLength) + "...";
    }
}
