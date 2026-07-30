package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.ai.model.Turn;

import java.util.List;

public interface StatementExtractor {
    List<Statement> extract(Turn turn);
}
