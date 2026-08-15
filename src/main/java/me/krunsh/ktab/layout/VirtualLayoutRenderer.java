package me.krunsh.ktab.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import me.krunsh.ktab.condition.ConditionEvaluation;
import me.krunsh.ktab.condition.ConditionEvaluator;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.render.PlaceholderRenderer;

/**
 * Renderer de grille fixe Ktab.
 *
 * V8 :
 * - colonnes conditionnelles sans déplacement horizontal ;
 * - cellules conditionnelles ;
 * - une cellule fixe masquée réserve toujours sa row ;
 * - une cellule automatique masquée ne consomme aucune row.
 */
public final class VirtualLayoutRenderer {

    private final KtabConfig config;
    private final PlaceholderRenderer renderer;
    private final ConditionEvaluator conditionEvaluator;

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

        this.conditionEvaluator =
            new ConditionEvaluator(
                config,
                renderer
            );
    }

    public List<RenderedVirtualCell> render(
            Player viewer,
            int onlinePlayers) {

        return renderDetailed(
            viewer,
            onlinePlayers
        ).getCells();
    }

    public LayoutRenderResult renderDetailed(
            Player viewer,
            int onlinePlayers) {

        if (viewer == null
                || !config.isVirtualLayoutEnabled()) {

            return new LayoutRenderResult(
                Collections.<RenderedVirtualCell>emptyList(),
                Collections.<LayoutDecision>emptyList()
            );
        }

        List<TabColumn> columns =
            config.getVirtualColumns();

        int columnCount =
            Math.min(
                config.getVirtualColumnsCount(),
                columns.size()
            );

        if (columnCount <= 0) {

            return new LayoutRenderResult(
                Collections.<RenderedVirtualCell>emptyList(),
                Collections.<LayoutDecision>emptyList()
            );
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

            return new LayoutRenderResult(
                Collections.<RenderedVirtualCell>emptyList(),
                Collections.<LayoutDecision>emptyList()
            );
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

        List<LayoutDecision> decisions =
            new ArrayList<LayoutDecision>();

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
                    blankText,
                    decisions
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

        return new LayoutRenderResult(
            entries,
            decisions
        );
    }

    private List<RenderedVirtualCell> renderColumn(
            Player viewer,
            TabColumn column,
            int columnIndex,
            int rowCount,
            String blankText,
            List<LayoutDecision> decisions) {

        List<RenderedVirtualCell> rows =
            emptyRows(
                column,
                columnIndex,
                rowCount
            );

        ConditionEvaluation columnEvaluation =
            conditionEvaluator.evaluate(
                viewer,
                column.getConditions()
            );

        if (column.hasConditions()) {

            decisions.add(
                new LayoutDecision(
                    "column."
                        + column.getId(),
                    columnEvaluation.isMatched(),
                    columnEvaluation.summarize()
                )
            );
        }

        /*
         * Une colonne masquée garde exactement sa largeur/position.
         * On retourne donc une colonne entière de cellules blank.
         */
        if (!columnEvaluation.isMatched()) {

            fillBlanks(
                rows,
                column,
                columnIndex,
                blankText
            );

            return rows;
        }

        List<ConfiguredCell> configuredCells =
            new ArrayList<ConfiguredCell>();

        TabCell title =
            column.getTitle();

        if (title != null
                && title.getText() != null
                && !title.getText()
                    .trim()
                    .isEmpty()) {

            configuredCells.add(
                new ConfiguredCell(
                    title,
                    "column."
                        + column.getId()
                        + ".title"
                )
            );
        }

        int lineIndex =
            0;

        for (TabCell cell
                : column.getLines()) {

            configuredCells.add(
                new ConfiguredCell(
                    cell,
                    "column."
                        + column.getId()
                        + ".lines["
                        + lineIndex
                        + "]"
                )
            );

            lineIndex++;
        }

        /*
         * PASS 1 : cellules à row fixe.
         * Même masquées, leur row reste réservée.
         */
        for (ConfiguredCell configured
                : configuredCells) {

            TabCell cell =
                configured.cell;

            if (cell == null
                    || !cell.hasExplicitRow()) {

                continue;
            }

            ConditionEvaluation evaluation =
                evaluateCell(
                    viewer,
                    configured,
                    decisions
                );

            if (evaluation.isMatched()) {

                placeExplicitVisible(
                    rows,
                    viewer,
                    column,
                    columnIndex,
                    cell
                );

            } else {

                reserveExplicitHidden(
                    rows,
                    viewer,
                    column,
                    columnIndex,
                    cell,
                    blankText
                );
            }
        }

        /*
         * PASS 2 : cellules automatiques.
         * Si leur condition est fausse elles ne prennent aucune place.
         */
        int nextAutoRow =
            0;

        for (ConfiguredCell configured
                : configuredCells) {

            TabCell cell =
                configured.cell;

            if (cell == null
                    || cell.hasExplicitRow()) {

                continue;
            }

            ConditionEvaluation evaluation =
                evaluateCell(
                    viewer,
                    configured,
                    decisions
                );

            if (!evaluation.isMatched()) {
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

        fillBlanks(
            rows,
            column,
            columnIndex,
            blankText
        );

        return rows;
    }

    private ConditionEvaluation evaluateCell(
            Player viewer,
            ConfiguredCell configured,
            List<LayoutDecision> decisions) {

        TabCell cell =
            configured.cell;

        ConditionEvaluation evaluation =
            conditionEvaluator.evaluate(
                viewer,
                cell.getConditions()
            );

        if (cell.hasConditions()) {

            decisions.add(
                new LayoutDecision(
                    configured.path,
                    evaluation.isMatched(),
                    evaluation.summarize()
                )
            );
        }

        return evaluation;
    }

    private void placeExplicitVisible(
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

    private void reserveExplicitHidden(
            List<RenderedVirtualCell> rows,
            Player viewer,
            TabColumn column,
            int columnIndex,
            TabCell configured,
            String blankText) {

        int startRow =
            configured.getConfiguredRow()
                - 1;

        if (startRow < 0
                || startRow >= rows.size()) {

            return;
        }

        /*
         * Le texte est rendu uniquement pour connaître le nombre de lignes
         * occupées si la cellule contient des \n.
         */
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

            if (rows.get(row) != null) {
                continue;
            }

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

    private List<RenderedVirtualCell> emptyRows(
            TabColumn column,
            int columnIndex,
            int rowCount) {

        List<RenderedVirtualCell> rows =
            new ArrayList<RenderedVirtualCell>(
                rowCount
            );

        for (int row = 0;
                row < rowCount;
                row++) {

            rows.add(null);
        }

        return rows;
    }

    private void fillBlanks(
            List<RenderedVirtualCell> rows,
            TabColumn column,
            int columnIndex,
            String blankText) {

        for (int row = 0;
                row < rows.size();
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

    private static final class ConfiguredCell {

        private final TabCell cell;
        private final String path;

        private ConfiguredCell(
                TabCell cell,
                String path) {

            this.cell =
                cell;

            this.path =
                path;
        }
    }
}
