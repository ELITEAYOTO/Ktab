package me.krunsh.ktab.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.render.PlaceholderRenderer;

/**
 * Transforme la configuration en cellules virtuelles finales.
 *
 * V7 utilise une vraie grille fixe :
 * - 1 à 4 colonnes ;
 * - 1 à 20 lignes ;
 * - row optionnel en YAML pour un placement exact ;
 * - toutes les positions explicites sont réservées en premier ;
 * - les cellules sans row remplissent ensuite les premières lignes libres.
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

    public List<RenderedVirtualCell> render(
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

        int rowCount =
            Math.min(
                20,
                Math.max(
                    1,
                    config.getVirtualRows()
                )
            );

        int entryLimit =
            PackedTabSizing.fakeEntryLimit(
                config.isVirtualForceClientRows(),
                columnCount,
                rowCount,
                config.getVirtualMaxEntries(),
                config.getVirtualReservedRealEntries(),
                onlinePlayers,
                config.isHideRealPlayers()
            );

        if (entryLimit <= 0) {
            return Collections.emptyList();
        }

        String blankText =
            renderer.render(
                viewer,
                config.getVirtualBlankText(),
                false
            );

        String prefix =
            renderer.render(
                viewer,
                config.getVirtualCellPrefix(),
                false
            );

        String suffix =
            renderer.render(
                viewer,
                config.getVirtualCellSuffix(),
                false
            );

        List<RenderedVirtualCell> entries =
            new ArrayList<RenderedVirtualCell>();

        for (int columnIndex = 0;
                columnIndex < columnCount
                    && entries.size() < entryLimit;
                columnIndex++) {

            TabColumn column =
                columns.get(
                    columnIndex
                );

            List<RenderedVirtualCell> renderedColumn =
                renderColumn(
                    viewer,
                    column,
                    columnIndex,
                    rowCount,
                    blankText
                );

            for (int rowIndex = 0;
                    rowIndex < renderedColumn.size()
                        && entries.size() < entryLimit;
                    rowIndex++) {

                RenderedVirtualCell cell =
                    renderedColumn.get(
                        rowIndex
                    );

                entries.add(
                    new RenderedVirtualCell(
                        prefix
                            + cell.getText()
                            + suffix,
                        cell.getSkinId(),
                        cell.getColumnId(),
                        cell.getColumnIndex(),
                        cell.getRowIndex()
                    )
                );
            }
        }

        return entries;
    }

    private List<RenderedVirtualCell> renderColumn(
            Player viewer,
            TabColumn column,
            int columnIndex,
            int rowCount,
            String blankText) {

        List<RenderedVirtualCell> rows =
            new ArrayList<RenderedVirtualCell>(
                rowCount
            );

        for (int row = 0;
                row < rowCount;
                row++) {

            rows.add(null);
        }

        List<TabCell> cells =
            new ArrayList<TabCell>();

        TabCell title =
            column.getTitle();

        if (title != null
                && title.getText() != null
                && !title.getText()
                    .trim()
                    .isEmpty()) {

            cells.add(
                title
            );
        }

        cells.addAll(
            column.getLines()
        );

        /*
         * PASS 1 :
         * réserve toutes les positions explicites.
         */
        for (TabCell cell : cells) {

            if (cell == null
                    || !cell.hasExplicitRow()) {

                continue;
            }

            placeExplicit(
                rows,
                viewer,
                column,
                columnIndex,
                cell
            );
        }

        /*
         * PASS 2 :
         * les cellules automatiques prennent les premières places libres.
         */
        int nextAutoRow =
            0;

        for (TabCell cell : cells) {

            if (cell == null
                    || cell.hasExplicitRow()) {

                continue;
            }

            nextAutoRow =
                placeAutomatic(
                    rows,
                    viewer,
                    column,
                    columnIndex,
                    cell,
                    nextAutoRow
                );
        }

        for (int row = 0;
                row < rowCount;
                row++) {

            if (rows.get(row) == null) {

                rows.set(
                    row,
                    new RenderedVirtualCell(
                        blankText,
                        config.getVirtualBlankSkinId(),
                        column.getId(),
                        columnIndex,
                        row
                    )
                );
            }
        }

        return rows;
    }

    private void placeExplicit(
            List<RenderedVirtualCell> rows,
            Player viewer,
            TabColumn column,
            int columnIndex,
            TabCell configured) {

        int startRow =
            configured.getConfiguredRow()
                - 1;

        if (startRow < 0
                || startRow >= rows.size()) {

            return;
        }

        String[] split =
            renderSplit(
                viewer,
                configured
            );

        for (int offset = 0;
                offset < split.length;
                offset++) {

            int row =
                startRow
                    + offset;

            if (row >= rows.size()) {
                break;
            }

            /*
             * En cas de collision on conserve la première cellule.
             * /ktab validate signale précisément la configuration fautive.
             */
            if (rows.get(row) != null) {
                continue;
            }

            rows.set(
                row,
                new RenderedVirtualCell(
                    split[offset],
                    configured.getSkinId(),
                    column.getId(),
                    columnIndex,
                    row
                )
            );
        }
    }

    private int placeAutomatic(
            List<RenderedVirtualCell> rows,
            Player viewer,
            TabColumn column,
            int columnIndex,
            TabCell configured,
            int startSearchRow) {

        String[] split =
            renderSplit(
                viewer,
                configured
            );

        int searchRow =
            Math.max(
                0,
                startSearchRow
            );

        for (String line : split) {

            int row =
                findNextFree(
                    rows,
                    searchRow
                );

            if (row < 0) {
                return rows.size();
            }

            rows.set(
                row,
                new RenderedVirtualCell(
                    line,
                    configured.getSkinId(),
                    column.getId(),
                    columnIndex,
                    row
                )
            );

            searchRow =
                row + 1;
        }

        return searchRow;
    }

    private String[] renderSplit(
            Player viewer,
            TabCell configured) {

        String rendered =
            renderer.render(
                viewer,
                configured.getText(),
                config.isPlaceholderApiEnabled()
            );

        return rendered == null
            ? new String[] {""}
            : rendered.split(
                "\\n",
                -1
            );
    }

    private static int findNextFree(
            List<RenderedVirtualCell> rows,
            int start) {

        int safeStart =
            Math.max(
                0,
                start
            );

        for (int row = safeStart;
                row < rows.size();
                row++) {

            if (rows.get(row) == null) {
                return row;
            }
        }

        return -1;
    }
}
