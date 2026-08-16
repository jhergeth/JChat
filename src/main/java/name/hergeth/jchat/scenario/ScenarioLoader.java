package name.hergeth.jchat.scenario;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public final class ScenarioLoader {

    private static final Yaml YAML = new Yaml();

    private ScenarioLoader() {}

    public static List<ScenarioDefinition> loadAll(Path scenariosDir) throws IOException {
        if (!Files.isDirectory(scenariosDir)) {
            throw new IOException("Scenarios directory not found: " + scenariosDir);
        }
        List<ScenarioDefinition> scenarios = new ArrayList<>();
        try (Stream<Path> paths = Files.list(scenariosDir)) {
            paths.filter(p -> p.getFileName().toString().endsWith(".yaml"))
                    .filter(p -> !p.getFileName().toString().endsWith(".expected.yaml"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(p -> {
                        try {
                            scenarios.add(loadScenario(p));
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to load " + p, e);
                        }
                    });
        }
        return scenarios;
    }

    public static ScenarioDefinition loadScenario(Path file) throws IOException {
        Map<String, Object> root = loadMap(file);
        String name = requiredString(root, "name", file);
        String conversationId = optionalString(root, "conversationId");
        String description = optionalString(root, "description");
        List<String> turns = requiredStringList(root, "turns", file);
        return new ScenarioDefinition(name, conversationId, description, turns);
    }

    public static Optional<ScenarioExpected> loadExpected(Path scenarioFile) throws IOException {
        Path expectedFile = Path.of(
                scenarioFile.toString().replace(".yaml", ".expected.yaml"));
        if (!Files.exists(expectedFile)) {
            return Optional.empty();
        }
        Map<String, Object> root = loadMap(expectedFile);
        List<TripleExpectation> mustContain = parseTripleList(root.get("mustContain"));
        Integer minStatements = optionalInteger(root, "minStatements");
        return Optional.of(new ScenarioExpected(mustContain, minStatements));
    }

    private static Map<String, Object> loadMap(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            Object loaded = YAML.load(in);
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new IOException("Expected YAML map in " + file);
            }
            return (Map<String, Object>) map;
        }
    }

    private static List<TripleExpectation> parseTripleList(Object raw) {
        List<TripleExpectation> triples = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return triples;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                triples.add(new TripleExpectation(
                        string(map.get("subject")),
                        string(map.get("predicate")),
                        string(map.get("object"))));
            }
        }
        return triples;
    }

    private static String requiredString(Map<String, Object> root, String key, Path file) throws IOException {
        String value = optionalString(root, key);
        if (value == null || value.isBlank()) {
            throw new IOException("Missing required field '" + key + "' in " + file);
        }
        return value;
    }

    private static List<String> requiredStringList(Map<String, Object> root, String key, Path file)
            throws IOException {
        Object raw = root.get(key);
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw new IOException("Missing or empty list '" + key + "' in " + file);
        }
        List<String> turns = new ArrayList<>();
        for (Object item : list) {
            if (item == null) {
                throw new IOException("Null entry in '" + key + "' in " + file);
            }
            turns.add(item.toString());
        }
        return turns;
    }

    private static String optionalString(Map<String, Object> root, String key) {
        Object value = root.get(key);
        return value == null ? null : value.toString();
    }

    private static Integer optionalInteger(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString();
    }
}
