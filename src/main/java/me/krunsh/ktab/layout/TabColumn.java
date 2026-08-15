package me.krunsh.ktab.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.krunsh.ktab.condition.TabConditionGroup;

/**
 * Définition immuable d'une colonne du layout virtuel.
 */
public final class TabColumn {

    private final String id;
    private final String defaultSkinId;

    private final TabCell title;
    private final List<TabCell> lines;

    private final TabConditionGroup conditions;

    public TabColumn(
            String id,
            String defaultSkinId,
            TabCell title,
            List<TabCell> lines) {

        this(
            id,
            defaultSkinId,
            title,
            lines,
            TabConditionGroup.ALWAYS
        );
    }

    public TabColumn(
            String id,
            String defaultSkinId,
            TabCell title,
            List<TabCell> lines,
            TabConditionGroup conditions) {

        this.id =
            id == null
                ? ""
                : id;

        this.defaultSkinId =
            defaultSkinId == null
                ? ""
                : defaultSkinId;

        this.title =
            title == null
                ? new TabCell(
                    "",
                    this.defaultSkinId
                )
                : title;

        List<TabCell> copy =
            lines == null
                ? new ArrayList<TabCell>()
                : new ArrayList<TabCell>(
                    lines
                );

        this.lines =
            Collections.unmodifiableList(
                copy
            );

        this.conditions =
            conditions == null
                ? TabConditionGroup.ALWAYS
                : conditions;
    }

    public String getId() {
        return id;
    }

    public String getDefaultSkinId() {
        return defaultSkinId;
    }

    public TabCell getTitle() {
        return title;
    }

    public List<TabCell> getLines() {
        return lines;
    }

    public TabConditionGroup getConditions() {
        return conditions;
    }

    public boolean hasConditions() {
        return !conditions.isEmpty();
    }
}
