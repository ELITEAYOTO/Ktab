package me.krunsh.ktab.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.render.PlaceholderRenderer;

/**
 * Transforme la configuration du layout en liste d'entrées virtuelles.
 *
 * L'ordre est volontairement column-major :
 *
 * colonne 0 / ligne 0..N
 * colonne 1 / ligne 0..N
 * ...
 *
 * C'est le comportement utilisé par le prototype historique et celui qui
 * correspond le mieux au remplissage visuel du TAB 1.8.
 */
public final class VirtualLayoutRenderer {

    private final KtabConfig config;
    private final PlaceholderRenderer renderer;

    public VirtualLayoutRenderer(
            KtabConfig config,
            PlaceholderRenderer renderer) {

        if (config == null
                || renderer == null) {

            throw new IllegalArgumentException(
                "Configuration ou renderer manquant."
            );
        }

        this.config = config;
        this.renderer = renderer;
    }

    public List<String> render(
            Player viewer,
            int onlinePlayers) {

        if (viewer == null
                || !config.isVirtualLayoutEnabled()) {

            return Collections.emptyList();
        }

        List<TabColumn> columns =
            config.getVirtualColumns();

        int columnCount =
            Math.min(
                config.getVirtualColumnsCount(),
                columns.size()
            );

        if (columnCount <= 0) {
            return Collections.emptyList();
        }

        List<List<String>> renderedColumns =
            new ArrayList<List<String>>();

        int rowCount =
            config.getVirtualRows();

        for (int i = 0;
                i < columnCount;
                i++) {

            List<String> lines =
                renderColumn(
                    viewer,
                    columns.get(i)
                );

            renderedColumns.add(lines);

            rowCount =
                Math.max(
                    rowCount,
                    lines.size()
                );
        }

        rowCount =
            Math.min(
                20,
                Math.max(
                    1,
                    rowCount
                )
            );

        int entryLimit =
            PackedTabSizing.fakeEntryLimit(
                config.isVirtualForceClientRows(),
                columnCount,
                rowCount,
                config.getVirtualMaxEntries(),
                config.getVirtualReservedRealEntries(),
                onlinePlayers
            );

        if (entryLimit <= 0) {
            return Collections.emptyList();
        }

        List<String> entries =
            new ArrayList<String>();

        for (int column = 0;
                column < columnCount;
                column++) {

            List<String> lines =
                renderedColumns.get(column);

            for (int row = 0;
                    row < rowCount
                        && entries.size() < entryLimit;
                    row++) {

                String value =
                    row < lines.size()
                        ? lines.get(row)
                        : config.getVirtualBlankText();

                if (value == null
                        || value.trim().isEmpty()) {

                    value =
                        config.getVirtualBlankText();
                }

                entries.add(
                    config.getVirtualCellPrefix()
                        + value
                        + config.getVirtualCellSuffix()
                );
            }
        }

        return entries;
    }

    private List<String> renderColumn(
            Player viewer,
            TabColumn column) {

        List<String> result =
            new ArrayList<String>();

        if (column.getTitle() != null
                && !column.getTitle()
                    .trim()
                    .isEmpty()) {

            addSplit(
                result,
                renderer.render(
                    viewer,
                    column.getTitle(),
                    config.isPlaceholderApiEnabled()
                )
            );
        }

        for (String rawLine
                : column.getLines()) {

            addSplit(
                result,
                renderer.render(
                    viewer,
                    rawLine,
                    config.isPlaceholderApiEnabled()
                )
            );
        }

        return result;
    }

    private static void addSplit(
            List<String> target,
            String value) {

        if (value == null) {
            target.add("");
            return;
        }

        String[] split =
            value.split(
                "\\n",
                -1
            );

        for (String line : split) {
            target.add(line);
        }
    }
}
