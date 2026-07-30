package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;

import java.util.List;

public interface StatementNormalizer {
    List<Statement> normalize(List<Statement> statements);
}
