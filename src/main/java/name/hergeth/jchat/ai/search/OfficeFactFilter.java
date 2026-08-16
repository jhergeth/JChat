package name.hergeth.jchat.ai.search;

import name.hergeth.jchat.ai.model.Statement;

import java.util.Locale;

public final class OfficeFactFilter {

    private OfficeFactFilter() {}

    public static boolean isOfficeHolderFact(Statement statement) {
        if (statement == null) {
            return false;
        }
        String subject = statement.subject().toLowerCase(Locale.ROOT);
        String predicate = statement.predicate().toLowerCase(Locale.ROOT);
        if (SearchTripleQualityFilter.isRoleTermAsSubject(subject)) {
            return false;
        }
        if (!SearchTripleQualityFilter.isOfficeRolePredicate(predicate)) {
            return false;
        }
        return WikiOfficeHolderExtractor.isPlausiblePersonName(statement.subject());
    }

    public static boolean touchesOfficeHolder(Statement statement) {
        return isOfficeHolderFact(statement);
    }
}
