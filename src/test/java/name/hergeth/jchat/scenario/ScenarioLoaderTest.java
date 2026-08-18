package name.hergeth.jchat.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioLoaderTest {

    @Test
    void testLoadScenarioWithoutModel(@TempDir Path tempDir) throws IOException {
        Path scenarioFile = tempDir.resolve("test.yaml");
        Files.writeString(scenarioFile, """
                name: test-scenario
                conversationId: custom-conv-id
                description: A test description
                turns:
                  - "Hello"
                  - "How are you?"
                """);

        ScenarioDefinition scenario = ScenarioLoader.loadScenario(scenarioFile);
        assertEquals("test-scenario", scenario.name());
        assertEquals("custom-conv-id", scenario.conversationId());
        assertEquals("A test description", scenario.description());
        assertEquals(List.of("Hello", "How are you?"), scenario.turns());
    }

    @Test
    void testLoadScenarioDefaultConversationId(@TempDir Path tempDir) throws IOException {
        Path scenarioFile = tempDir.resolve("simple.yaml");
        Files.writeString(scenarioFile, """
                name: simple
                turns:
                  - "Turn 1"
                """);

        ScenarioDefinition scenario = ScenarioLoader.loadScenario(scenarioFile);
        assertEquals("simple", scenario.name());
        assertEquals("scenario-simple", scenario.conversationId());
        assertNull(scenario.description());
        assertEquals(List.of("Turn 1"), scenario.turns());
    }

    @Test
    void loadByNameFindsScenarioFile(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("web-search.yaml"), """
                name: web-search
                turns:
                  - "Frage"
                """);

        ScenarioDefinition scenario = ScenarioLoader.loadByName(tempDir, "web-search");
        assertEquals("web-search", scenario.name());
        assertEquals(List.of("Frage"), scenario.turns());
    }
}
