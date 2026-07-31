package name.hergeth.jchat.scenario;

import java.util.List;

public record ScenarioTurnResult(
        int index,
        String userMessage,
        String assistantResponse,
        List<StatementSnapshot> knowledgeStore
) {}
