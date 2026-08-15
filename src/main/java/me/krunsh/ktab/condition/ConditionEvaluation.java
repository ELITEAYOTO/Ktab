package me.krunsh.ktab.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Résultat détaillé d'un groupe de conditions.
 */
public final class ConditionEvaluation {

    private final boolean matched;
    private final List<String> details;

    public ConditionEvaluation(
            boolean matched,
            List<String> details) {

        this.matched =
            matched;

        List<String> copy =
            details == null
                ? new ArrayList<String>()
                : new ArrayList<String>(
                    details
                );

        this.details =
            Collections.unmodifiableList(
                copy
            );
    }

    public boolean isMatched() {
        return matched;
    }

    public List<String> getDetails() {
        return details;
    }

    public String summarize() {

        if (details.isEmpty()) {
            return matched
                ? "toujours visible"
                : "masqué";
        }

        StringBuilder builder =
            new StringBuilder();

        for (String detail : details) {

            if (builder.length() > 0) {
                builder.append(" | ");
            }

            builder.append(detail);
        }

        return builder.toString();
    }
}
