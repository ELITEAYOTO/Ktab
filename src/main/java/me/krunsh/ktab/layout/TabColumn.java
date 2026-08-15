package me.krunsh.ktab.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Définition immuable d'une colonne du layout virtuel.
 */
public final class TabColumn {

    private final String id;
    private final String title;
    private final List<String> lines;

    public TabColumn(
            String id,
            String title,
            List<String> lines) {

        this.id =
            id == null ? "" : id;

        this.title =
            title == null ? "" : title;

        List<String> copy =
            lines == null
                ? new ArrayList<String>()
                : new ArrayList<String>(lines);

        this.lines =
            Collections.unmodifiableList(copy);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getLines() {
        return lines;
    }
}
