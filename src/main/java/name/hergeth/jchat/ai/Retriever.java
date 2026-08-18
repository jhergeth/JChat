package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.context.ResolvedContext;
import name.hergeth.jchat.ai.model.Statement;

import java.util.List;

public interface Retriever {

    List<Statement> retrieve(String conversationId, ResolvedContext context);

    default List<Statement> retrieve(String conversationId, String query) {
        return retrieve(conversationId, ResolvedContext.plain(query));
    }
}
