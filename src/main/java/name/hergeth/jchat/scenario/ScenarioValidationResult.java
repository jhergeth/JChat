package name.hergeth.jchat.scenario;

import java.util.List;

public record ScenarioValidationResult(
        boolean passed,
        List<String> failures
) {}
