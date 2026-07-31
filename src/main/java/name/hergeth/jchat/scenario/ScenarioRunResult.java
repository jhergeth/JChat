package name.hergeth.jchat.scenario;

import java.time.Instant;
import java.util.List;

public record ScenarioRunResult(
        String scenarioName,
        String conversationId,
        Instant runAt,
        List<ScenarioTurnResult> turns,
        List<StatementSnapshot> finalKnowledgeStore,
        ScenarioValidationResult validation
) {}
