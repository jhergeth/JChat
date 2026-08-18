package name.hergeth.jchat.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import name.hergeth.jchat.ai.llm.LlmResponseException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AgentLoopTest {

    @Test
    void returnsTextWhenModelAnswersWithoutTools() {
        ChatLanguageModel model = new ChatLanguageModel() {
            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                return Response.from(AiMessage.from("Berlin"));
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
                return Response.from(AiMessage.from("Berlin"));
            }
        };
        AgentLoop loop = new AgentLoop(name -> model, new ToolRegistry(List.of(new StubWebSearchTool())), 3, 3, 1);

        AgentChatResult result = loop.run(
                "test",
                List.of(new name.hergeth.jchat.openai.dto.ChatMessage("user", "Hauptstadt?")),
                null);

        assertEquals("Berlin", result.text());
        assertTrue(result.toolCalls().isEmpty());
        assertEquals(1, result.stepsUsed());
    }

    @Test
    void executesToolThenReturnsFinalAnswer() {
        AtomicInteger modelCalls = new AtomicInteger();
        ChatLanguageModel model = new ChatLanguageModel() {
            private int step;

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
                modelCalls.incrementAndGet();
                if (step++ == 0) {
                    ToolExecutionRequest request = ToolExecutionRequest.builder()
                            .id("1")
                            .name("web_search")
                            .arguments("{\"query\":\"test\"}")
                            .build();
                    return Response.from(AiMessage.from(request));
                }
                return Response.from(AiMessage.from("Antwort nach Suche"));
            }
        };

        AgentLoop loop = new AgentLoop(name -> model, new ToolRegistry(List.of(new StubWebSearchTool())), 5, 3, 1);

        AgentChatResult result = loop.run(
                "test",
                List.of(new name.hergeth.jchat.openai.dto.ChatMessage("user", "Frage")),
                null);

        assertEquals("Antwort nach Suche", result.text());
        assertEquals(1, result.toolCalls().size());
        assertEquals("web_search", result.toolCalls().get(0).toolName());
        assertFalse(result.toolCalls().get(0).error());
        assertEquals(2, result.stepsUsed());
        assertEquals(2, modelCalls.get());
    }

    @Test
    void returnsFallbackWhenMaxStepsExceededWithToolResults() {
        ChatLanguageModel model = new ChatLanguageModel() {
            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
                ToolExecutionRequest request = ToolExecutionRequest.builder()
                        .id("1")
                        .name("web_search")
                        .arguments("{\"query\":\"loop\"}")
                        .build();
                return Response.from(AiMessage.from(request));
            }
        };
        AgentLoop loop = new AgentLoop(name -> model, new ToolRegistry(List.of(new StubWebSearchTool())), 2, 3, 1);

        AgentChatResult result = loop.run(
                "test", List.of(new name.hergeth.jchat.openai.dto.ChatMessage("user", "x")), null);

        assertTrue(result.text().startsWith("Laut Recherche:"));
        assertEquals(2, result.stepsUsed());
    }

    @Test
    void usesFallbackWhenLlmTimesOutAfterToolCalls() {
        ChatLanguageModel model = new ChatLanguageModel() {
            private int step;

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
                if (step++ == 0) {
                    ToolExecutionRequest request = ToolExecutionRequest.builder()
                            .id("1")
                            .name("web_search")
                            .arguments("{\"query\":\"test\"}")
                            .build();
                    return Response.from(AiMessage.from(request));
                }
                throw new RuntimeException("request timed out after 60 seconds");
            }
        };
        AgentLoop loop = new AgentLoop(name -> model, new ToolRegistry(List.of(new StubWebSearchTool())), 5, 3, 1);

        AgentChatResult result = loop.run(
                "test",
                List.of(new name.hergeth.jchat.openai.dto.ChatMessage("user", "Frage")),
                null);

        assertTrue(result.text().startsWith("Laut Recherche:"));
        assertEquals(1, result.toolCalls().size());
    }

    @Test
    void skipsParallelToolCallsBeyondPerStepLimit() {
        ChatLanguageModel model = new ChatLanguageModel() {
            private int step;

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
                if (step++ == 0) {
                    ToolExecutionRequest first = ToolExecutionRequest.builder()
                            .id("1")
                            .name("web_search")
                            .arguments("{\"query\":\"a\"}")
                            .build();
                    ToolExecutionRequest second = ToolExecutionRequest.builder()
                            .id("2")
                            .name("web_search")
                            .arguments("{\"query\":\"b\"}")
                            .build();
                    return Response.from(AiMessage.from(first, second));
                }
                return Response.from(AiMessage.from("Fertig"));
            }
        };
        AgentLoop loop = new AgentLoop(name -> model, new ToolRegistry(List.of(new StubWebSearchTool())), 5, 3, 1);

        AgentChatResult result = loop.run(
                "test",
                List.of(new name.hergeth.jchat.openai.dto.ChatMessage("user", "Frage")),
                null);

        assertEquals("Fertig", result.text());
        assertEquals(2, result.toolCalls().size());
        assertFalse(result.toolCalls().get(0).error());
        assertTrue(result.toolCalls().get(1).error());
        assertTrue(result.toolCalls().get(1).result().contains("Parallele"));
    }

    private static final class StubWebSearchTool implements JChatTool {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        @Override
        public String name() {
            return "web_search";
        }

        @Override
        public String description() {
            return "stub";
        }

        @Override
        public com.fasterxml.jackson.databind.JsonNode parameterSchema() {
            return MAPPER.createObjectNode();
        }

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public ToolExecutionResult execute(com.fasterxml.jackson.databind.JsonNode arguments, ToolContext context) {
            return ToolExecutionResult.ok("Suchergebnis");
        }
    }
}
