package me.krunsh.ktab.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Groupe de conditions ALL ou ANY.
 */
public final class TabConditionGroup {

    public enum Mode {
        ALL,
        ANY
    }

    public static final TabConditionGroup ALWAYS =
        new TabConditionGroup(
            Mode.ALL,
            Collections.<TabCondition>emptyList()
        );

    private final Mode mode;
    private final List<TabCondition> conditions;

    public TabConditionGroup(
            Mode mode,
            List<TabCondition> conditions) {

        this.mode =
            mode == null
                ? Mode.ALL
                : mode;

        List<TabCondition> copy =
            conditions == null
                ? new ArrayList<TabCondition>()
                : new ArrayList<TabCondition>(
                    conditions
                );

        this.conditions =
            Collections.unmodifiableList(
                copy
            );
    }

    public Mode getMode() {
        return mode;
    }

    public List<TabCondition> getConditions() {
        return conditions;
    }

    public boolean isEmpty() {
        return conditions.isEmpty();
    }
}
