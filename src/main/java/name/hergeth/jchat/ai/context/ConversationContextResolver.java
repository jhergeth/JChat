package name.hergeth.jchat.ai.context;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import name.hergeth.jchat.ai.KnowledgeStore;
import name.hergeth.jchat.ai.EntityIndex;
import name.hergeth.jchat.ai.model.Statement;
import name.hergeth.jchat.openai.dto.ChatMessage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Singleton
public class ConversationContextResolver {

    private static final Pattern MASCULINE_POSSESSIVE = Pattern.compile(
            "\\b(sein|seine|seinem|seinen|seiner)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern FEMININE_POSSESSIVE = Pattern.compile(
            "\\b(ihr|ihre|ihrem|ihren|ihrer)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DEMONSTRATIVE = Pattern.compile(
            "\\b(diese[rsnm]?|dieser|dieses|jene[rsnm]?|jenes|jenen|jener|dortige[rsnm]?)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Set<String> RELATION_TERMS = Set.of(
            "frau", "ehefrau", "ehemann", "mann", "partner", "ehepartner",
            "pressesprecher", "pressesprecherin", "tochter", "sohn", "kind",
            "vater", "mutter", "bruder", "schwester", "gatte", "gattin");

    private final KnowledgeStore knowledgeStore;
    private final int recentTurns;

    public ConversationContextResolver(
            KnowledgeStore knowledgeStore,
            @Value("${app.context.recent-turns:3}") int recentTurns) {
        this.knowledgeStore = knowledgeStore;
        this.recentTurns = Math.max(1, recentTurns);
    }

    public ResolvedContext resolve(String conversationId, List<ChatMessage> messages, String userMessage) {
        String question = userMessage == null ? "" : userMessage.trim();
        if (question.isBlank()) {
            return ResolvedContext.plain(question);
        }

        List<Statement> all = knowledgeStore.all(conversationId);
        EntityIndex index = EntityIndex.from(all);
        if (index.isEmpty()) {
            return ResolvedContext.plain(question);
        }

        List<String> priorDialog = priorDialogTexts(messages, question);
        List<String> salientKeys = findSalientEntities(priorDialog, index);
        List<String> currentKeys = index.entityKeysMentionedIn(question);

        LinkedHashSet<String> focusKeys = new LinkedHashSet<>(currentKeys);
        String resolvedQuery = question;
        String notes = "";

        if (hasPossessivePronoun(question) || hasDemonstrative(question)) {
            Optional<String> antecedent = resolveAntecedent(question, priorDialog, salientKeys, index);
            if (antecedent.isPresent()) {
                focusKeys.add(antecedent.get());
                String label = index.labelFor(antecedent.get());
                resolvedQuery = buildResolvedQuery(question, label);
                notes = "pronoun->" + label;
            }
        } else if (focusKeys.isEmpty() && !salientKeys.isEmpty()) {
            focusKeys.add(salientKeys.get(0));
        }

        List<String> focusEntityKeys = List.copyOf(focusKeys);
        List<String> focusLabels = focusEntityKeys.stream().map(index::labelFor).toList();
        return new ResolvedContext(question, resolvedQuery, focusEntityKeys, focusLabels, notes);
    }

    private Optional<String> resolveAntecedent(
            String question,
            List<String> priorDialog,
            List<String> salientKeys,
            EntityIndex index) {
        for (String text : priorDialog) {
            Optional<String> roleKey = index.entityKeyForRoleMention(text);
            if (roleKey.isPresent()) {
                return roleKey;
            }
        }
        for (String text : priorDialog) {
            List<String> mentioned = index.entityKeysMentionedIn(text);
            if (!mentioned.isEmpty()) {
                return Optional.of(mentioned.get(0));
            }
        }
        if (!salientKeys.isEmpty()) {
            return Optional.of(salientKeys.get(0));
        }
        if (FEMININE_POSSESSIVE.matcher(question).find()) {
            return salientKeys.stream().findFirst();
        }
        return Optional.empty();
    }

    private static List<String> findSalientEntities(List<String> dialogTexts, EntityIndex index) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String text : dialogTexts) {
            keys.addAll(index.entityKeysMentionedIn(text));
        }
        return List.copyOf(keys);
    }

    private static String buildResolvedQuery(String question, String entityLabel) {
        String lower = question.toLowerCase(Locale.ROOT);
        for (String relation : RELATION_TERMS) {
            if (TermContains.matchesWord(lower, relation)) {
                return relation + " " + entityLabel;
            }
        }
        return question + " " + entityLabel;
    }

    private static boolean hasPossessivePronoun(String text) {
        return MASCULINE_POSSESSIVE.matcher(text).find()
                || FEMININE_POSSESSIVE.matcher(text).find();
    }

    private static boolean hasDemonstrative(String text) {
        return DEMONSTRATIVE.matcher(text).find();
    }

    private List<String> priorDialogTexts(List<ChatMessage> messages, String currentQuestion) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<String> texts = new ArrayList<>();
        int collected = 0;
        for (int i = messages.size() - 1; i >= 0 && collected < recentTurns * 2; i--) {
            ChatMessage message = messages.get(i);
            if (message.content() == null || message.content().isBlank()) {
                continue;
            }
            if ("user".equals(message.role()) && message.content().trim().equals(currentQuestion)) {
                continue;
            }
            if ("user".equals(message.role()) || "assistant".equals(message.role())) {
                texts.add(message.content().trim());
                collected++;
            }
        }
        return texts;
    }

    private static final class TermContains {
        private TermContains() {}

        static boolean matchesWord(String lowerText, String term) {
            if (lowerText.contains(" " + term + " ")
                    || lowerText.startsWith(term + " ")
                    || lowerText.endsWith(" " + term)
                    || lowerText.equals(term)) {
                return true;
            }
            return lowerText.contains(term);
        }
    }
}
