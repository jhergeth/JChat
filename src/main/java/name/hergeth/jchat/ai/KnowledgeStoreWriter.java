package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.ai.search.OfficeFactFilter;
import jakarta.inject.Singleton;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class KnowledgeStoreWriter {

    private final KnowledgeStore knowledgeStore;

    public KnowledgeStoreWriter(KnowledgeStore knowledgeStore) {
        this.knowledgeStore = knowledgeStore;
    }

    public void merge(String conversationId, List<Statement> extracted, int maxStatements) {
        if (extracted.isEmpty()) {
            return;
        }
        List<Statement> merged = merge(knowledgeStore.all(conversationId), extracted);
        List<Statement> toStore = merged.stream().limit(maxStatements).toList();
        knowledgeStore.replaceAll(conversationId, toStore);
    }

    public void mergeSearchResults(String conversationId, List<Statement> extracted, int maxStatements) {
        if (extracted.isEmpty()) {
            return;
        }
        List<Statement> existing = knowledgeStore.all(conversationId);
        boolean replacesOfficeFacts = extracted.stream().anyMatch(OfficeFactFilter::touchesOfficeHolder);
        List<Statement> base = replacesOfficeFacts
                ? existing.stream().filter(s -> !OfficeFactFilter.isOfficeHolderFact(s)).toList()
                : existing;
        List<Statement> merged = merge(base, extracted);
        List<Statement> toStore = merged.stream().limit(maxStatements).toList();
        knowledgeStore.replaceAll(conversationId, toStore);
    }

    static List<Statement> merge(List<Statement> existing, List<Statement> extracted) {
        Map<String, Statement> byKey = new LinkedHashMap<>();
        for (Statement statement : existing) {
            byKey.put(StatementTextNormalizer.factKey(statement.subject(), statement.predicate()), statement);
        }
        for (Statement statement : extracted) {
            byKey.put(StatementTextNormalizer.factKey(statement.subject(), statement.predicate()), statement);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(Statement::createdAt).reversed())
                .toList();
    }
}
