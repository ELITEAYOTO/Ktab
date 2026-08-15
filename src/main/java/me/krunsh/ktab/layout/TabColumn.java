package me.krunsh.ktab.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Définition immuable d'une colonne du layout virtuel.
 */
public final class TabColumn {

    private final String id;

    private final String defaultSkinId;

    private final TabCell title;
    private final List<TabCell> lines;

    public TabColumn(
            String id,
            String defaultSkinId,
            TabCell title,
            List<TabCell> lines) {

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
}
