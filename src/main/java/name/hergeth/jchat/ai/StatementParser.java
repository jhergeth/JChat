package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.ai.model.Turn;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class StatementParser {

    public List<Statement> parse(String llmOutput, Turn turn) {
        List<Statement> statements = new ArrayList<>();
        for (String line : llmOutput.split("\n")) {
            Statement statement = parseLine(line, turn);
            if (statement != null) {
                statements.add(statement);
            }
        }
        return statements;
    }

    private Statement parseLine(String line, Turn turn) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        trimmed = trimmed.replaceFirst("^[-*\\d.]+\\s*", "");
        trimmed = trimmed.replaceFirst("(?i)^assistant:\\s*", "");
        String[] parts = trimmed.split("\\|", 3);
        if (parts.length < 3) {
            return null;
        }
        return new Statement(
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                turn.conversationId(),
                turn.turnId(),
                turn.timestamp());
    }
}
