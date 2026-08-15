package me.krunsh.ktab.layout;

import me.krunsh.ktab.condition.TabConditionGroup;

/**
 * Cellule configurée avant rendu PlaceholderAPI.
 *
 * configuredRow utilise une numérotation utilisateur 1..20.
 * 0 signifie "placement automatique".
 */
public final class TabCell {

    private final String text;
    private final String skinId;
    private final int configuredRow;
    private final TabConditionGroup conditions;

    public TabCell(
            String text,
            String skinId) {

        this(
            text,
            skinId,
            0,
            TabConditionGroup.ALWAYS
        );
    }

    public TabCell(
            String text,
            String skinId,
            int configuredRow) {

        this(
            text,
            skinId,
            configuredRow,
            TabConditionGroup.ALWAYS
        );
    }

    public TabCell(
            String text,
            String skinId,
            int configuredRow,
            TabConditionGroup conditions) {

        this.text =
            text == null
                ? ""
                : text;

        this.skinId =
            skinId == null
                ? ""
                : skinId;

        this.configuredRow =
            configuredRow;

        this.conditions =
            conditions == null
                ? TabConditionGroup.ALWAYS
                : conditions;
    }

    public String getText() {
        return text;
    }

    public String getSkinId() {
        return skinId;
    }

    public int getConfiguredRow() {
        return configuredRow;
    }

    public boolean hasExplicitRow() {
        return configuredRow != 0;
    }

    public TabConditionGroup getConditions() {
        return conditions;
    }

    public boolean hasConditions() {
        return !conditions.isEmpty();
    }
}
