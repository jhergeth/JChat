package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.List;

public record KnowledgeStoreMatch(
        String query,
        List<Statement> statements,
        String promptContext,
        int topScore
) {}
