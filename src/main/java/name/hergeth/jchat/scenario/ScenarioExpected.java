package name.hergeth.jchat.scenario;

import java.util.List;

public record ScenarioExpected(
        List<TripleExpectation> mustContain,
        Integer minStatements
) {
    public ScenarioExpected {
        if (mustContain == null) {
            mustContain = List.of();
        }
    }
}
