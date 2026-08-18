package name.hergeth.jchat.scenario;

import java.nio.file.Path;
import java.util.List;

public final class ScenarioRunnerMain {

    private ScenarioRunnerMain() {}

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        JChatHttpClient client = new JChatHttpClient(config.baseUrl());
        ScenarioRunner runner = new ScenarioRunner(
                client,
                config.scenariosDir(),
                config.outputDir(),
                config.validate(),
                config.semanticValidate());

        List<ScenarioRunResult> results = runner.runAll();

        int passed = 0;
        int failed = 0;
        for (ScenarioRunResult result : results) {
            boolean ok = result.validation().passed();
            if (ok) {
                passed++;
            } else {
                failed++;
            }
            System.out.printf("%s %s -> %d turns, %d statements%n",
                    ok ? "OK" : "FAIL",
                    result.scenarioName(),
                    result.turns().size(),
                    result.finalKnowledgeStore().size());
            if (!ok) {
                for (String failure : result.validation().failures()) {
                    System.out.println("  - " + failure);
                }
            }
        }

        System.out.printf("%nResults written to %s%n", config.outputDir());
        if (failed > 0) {
            System.exit(1);
        }
    }

    private record Config(
            String baseUrl,
            Path scenariosDir,
            Path outputDir,
            boolean validate,
            boolean semanticValidate) {

        static Config parse(String[] args) {
            String baseUrl = "http://localhost:8080";
            Path scenariosDir = Path.of("scenarios");
            Path outputDir = Path.of("build/scenario-runs");
            boolean validate = false;
            boolean semanticValidate = false;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--base-url" -> baseUrl = requireValue(args, ++i, "--base-url");
                    case "--scenarios" -> scenariosDir = Path.of(requireValue(args, ++i, "--scenarios"));
                    case "--output" -> outputDir = Path.of(requireValue(args, ++i, "--output"));
                    case "--validate" -> validate = true;
                    case "--semantic-validate" -> semanticValidate = true;
                    case "--help" -> {
                        printHelp();
                        System.exit(0);
                    }
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }
            if (semanticValidate && !validate) {
                throw new IllegalArgumentException("--semantic-validate requires --validate");
            }
            return new Config(baseUrl, scenariosDir, outputDir, validate, semanticValidate);
        }

        private static String requireValue(String[] args, int index, String flag) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + flag);
            }
            return args[index];
        }

        private static void printHelp() {
            System.out.println("""
                    Usage: runScenarios [options]

                      --base-url URL          JChat base URL (default: http://localhost:8080)
                      --scenarios DIR         Scenario YAML directory (default: scenarios)
                      --output DIR            Output directory (default: build/scenario-runs)
                      --validate              Check *.expected.yaml files
                      --semantic-validate     LLM fallback for unmatched triples (requires --validate)
                      --help                  Show this help
                    """);
        }
    }
}
