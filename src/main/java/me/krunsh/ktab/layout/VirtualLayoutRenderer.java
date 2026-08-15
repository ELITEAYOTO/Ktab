package me.krunsh.ktab.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.krunsh.ktab.cache.RenderedCellCache;
import me.krunsh.ktab.cache.RenderedCellCache.CachedCell;
import me.krunsh.ktab.condition.ConditionEvaluation;
import me.krunsh.ktab.condition.ConditionEvaluator;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.dependency.DependencyAnalyzer;
import me.krunsh.ktab.dependency.DependencySet;
import me.krunsh.ktab.performance.SelectiveRenderMetrics;
import me.krunsh.ktab.render.PlaceholderRenderer;

/**
 * Renderer de grille fixe Ktab.
 *
 * V9.4 :
 * - cache final par cellule/viewer ;
 * - dépendances PlaceholderAPI / global / permission analysées une fois ;
 * - une cellule valide est réutilisée sans réévaluer conditions/PAPI ;
 * - revision globale invalide uniquement les cellules qui en dépendent ;
 * - rows fixes et colonnes conditionnelles gardent exactement la géométrie.
 */
public final class VirtualLayoutRenderer {

    private final KtabConfig config;
    private final PlaceholderRenderer renderer;
    private final ConditionEvaluator conditionEvaluator;

    private final DependencyAnalyzer dependencyAnalyzer;
    private final RenderedCellCache renderedCellCache =
        new RenderedCellCache();

    private final SelectiveRenderMetrics metrics =
        new SelectiveRenderMetrics();

    public VirtualLayoutRenderer(
            KtabConfig config,
            PlaceholderRenderer renderer) {

        if (config == null
                || renderer == null) {

            throw new IllegalArgumentException(
                "Configuration ou renderer manquant."
            );
        }

        this.config =
            config;

        this.renderer =
            renderer;

        this.conditionEvaluator =
            new ConditionEvaluator(
                config,
                renderer
            );

        this.dependencyAnalyzer =
            new DependencyAnalyzer(
                config,
                renderer
            );

        applyConfig();
    }

    public void applyConfig() {

        renderedCellCache
            .setMaxCellsPerPlayer(
                config
                    .getPerformanceRenderMaxCellsPerPlayer()
            );
    }

    public void clearCaches() {

        renderedCellCache.clear();
        dependencyAnalyzer.clear();
        applyConfig();
    }

    public void invalidateViewer(
            UUID viewerId) {

        renderedCellCache.invalidate(
            viewerId
        );
    }

    public void resetMetrics() {
        metrics.reset();
    }

    public SelectiveRenderMetrics getMetrics() {
        return metrics;
    }

    public int getCachedViewerCount() {
        return renderedCellCache
            .getViewerCount();
    }

    public int getCachedCellCount() {
        return renderedCellCache
            .getCellCount();
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

            return emptyResult();
        }

        List<TabColumn> columns =
            config.getVirtualColumns();

        int columnCount =
            Math.min(
                config.getVirtualColumnsCount(),
                columns.size()
            );

        if (columnCount <= 0) {
            return emptyResult();
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
            return emptyResult();
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
                rowCount
            );

        String columnPath =
            "column."
                + column.getId();

        ConditionResult columnCondition =
            resolveCondition(
                viewer,
                columnPath + ".__condition",
                column.getConditions(),
                dependencyAnalyzer
                    .analyzeColumn(
                        column
                    )
            );

        if (column.hasConditions()) {

            decisions.add(
                new LayoutDecision(
                    columnPath,
                    columnCondition.visible,
                    columnCondition.reason
                )
            );
        }

        if (!columnCondition.visible) {

            fillBlanks(
                rows,
                column,
                columnIndex,
                blankText
            );

            return rows;
        }

        List<ConfiguredCell> configuredCells =
            collectConfiguredCells(
                column
            );

        /*
         * PASS 1 : rows explicites.
         * Une cellule masquée réserve toujours son espace.
         */
        for (ConfiguredCell configured
                : configuredCells) {

            TabCell cell =
                configured.cell;

            if (cell == null
                    || !cell.hasExplicitRow()) {

                continue;
            }

            ResolvedConfiguredCell resolved =
                resolveCell(
                    viewer,
                    configured
                );

            if (cell.hasConditions()) {

                decisions.add(
                    new LayoutDecision(
                        configured.path,
                        resolved.visible,
                        resolved.reason
                    )
                );
            }

            if (resolved.visible) {

                placeExplicitVisible(
                    rows,
                    column,
                    columnIndex,
                    cell,
                    resolved.renderedText
                );

            } else {

                reserveExplicitHidden(
                    rows,
                    column,
                    columnIndex,
                    cell,
                    resolved.renderedText,
                    blankText
                );
            }
        }

        /*
         * PASS 2 : rows automatiques.
         * Une cellule masquée ne consomme aucune position.
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

            ResolvedConfiguredCell resolved =
                resolveCell(
                    viewer,
                    configured
                );

            if (cell.hasConditions()) {

                decisions.add(
                    new LayoutDecision(
                        configured.path,
                        resolved.visible,
                        resolved.reason
                    )
                );
            }

            if (!resolved.visible) {
                continue;
            }

            nextAutoRow =
                placeAutomatic(
                    rows,
                    column,
                    columnIndex,
                    cell,
                    resolved.renderedText,
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

    private ResolvedConfiguredCell resolveCell(
            Player viewer,
            ConfiguredCell configured) {

        TabCell cell =
            configured.cell;

        DependencySet dependencies =
            dependencyAnalyzer
                .analyzeCell(
                    cell
                );

        long now =
            System.currentTimeMillis();

        long globalRevision =
            renderer
                .getGlobalSnapshot()
                .getRevision();

        boolean cacheEnabled =
            config
                .isPerformanceRenderCacheEnabled();

        CachedCell cached =
            cacheEnabled
                ? renderedCellCache.get(
                    viewer.getUniqueId(),
                    configured.path,
                    now,
                    globalRevision,
                    dependencies
                )
                : null;

        if (cached != null) {

            metrics.recordCell(
                true
            );

            if (cell.hasConditions()) {
                metrics.recordCondition(
                    true
                );
            }

            return new ResolvedConfiguredCell(
                cached.isVisible(),
                cached.getRenderedText(),
                "cache"
            );
        }

        metrics.recordCell(
            false
        );

        ConditionEvaluation evaluation =
            conditionEvaluator.evaluate(
                viewer,
                cell.getConditions()
            );

        if (cell.hasConditions()) {
            metrics.recordCondition(
                false
            );
        }

        /*
         * Même masqué, on rend le texte une fois afin de conserver le nombre
         * exact de lignes si un placeholder génère un \n.
         */
        String rendered =
            renderer.render(
                viewer,
                cell.getText(),
                config.isPlaceholderApiEnabled()
            );

        long ttlTicks =
            resolveRenderTtlTicks(
                dependencies
            );

        if (cacheEnabled
                && ttlTicks > 0L) {

            renderedCellCache.put(
                viewer.getUniqueId(),
                configured.path,
                evaluation.isMatched(),
                rendered,
                ticksToMillis(
                    ttlTicks
                ),
                globalRevision
            );
        }

        return new ResolvedConfiguredCell(
            evaluation.isMatched(),
            rendered,
            evaluation.summarize()
        );
    }

    private ConditionResult resolveCondition(
            Player viewer,
            String path,
            me.krunsh.ktab.condition.TabConditionGroup group,
            DependencySet dependencies) {

        if (group == null
                || group.isEmpty()) {

            return new ConditionResult(
                true,
                "toujours visible"
            );
        }

        long now =
            System.currentTimeMillis();

        long globalRevision =
            renderer
                .getGlobalSnapshot()
                .getRevision();

        boolean cacheEnabled =
            config
                .isPerformanceRenderCacheEnabled();

        CachedCell cached =
            cacheEnabled
                ? renderedCellCache.get(
                    viewer.getUniqueId(),
                    path,
                    now,
                    globalRevision,
                    dependencies
                )
                : null;

        if (cached != null) {

            metrics.recordCondition(
                true
            );

            return new ConditionResult(
                cached.isVisible(),
                "cache"
            );
        }

        metrics.recordCondition(
            false
        );

        ConditionEvaluation evaluation =
            conditionEvaluator.evaluate(
                viewer,
                group
            );

        long ttlTicks =
            resolveRenderTtlTicks(
                dependencies
            );

        if (cacheEnabled
                && ttlTicks > 0L) {

            renderedCellCache.put(
                viewer.getUniqueId(),
                path,
                evaluation.isMatched(),
                "",
                ticksToMillis(
                    ttlTicks
                ),
                globalRevision
            );
        }

        return new ConditionResult(
            evaluation.isMatched(),
            evaluation.summarize()
        );
    }

    private long resolveRenderTtlTicks(
            DependencySet dependencies) {

        if (dependencies == null) {

            return config
                .getPerformanceRenderDefaultTtlTicks();
        }

        if (!dependencies.isDynamic()
                && !dependencies.isPermission()) {

            return config
                .getPerformanceRenderStaticTtlTicks();
        }

        long fallback =
            dependencies.isPermission()
                ? config
                    .getPerformanceRenderPermissionTtlTicks()
                : config
                    .getPerformanceRenderDefaultTtlTicks();

        return dependencies
            .getTtlTicks(
                fallback
            );
    }

    private List<ConfiguredCell> collectConfiguredCells(
            TabColumn column) {

        List<ConfiguredCell> result =
            new ArrayList<ConfiguredCell>();

        TabCell title =
            column.getTitle();

        if (title != null
                && title.getText() != null
                && !title.getText()
                    .trim()
                    .isEmpty()) {

            result.add(
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

            result.add(
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

        return result;
    }

    private void placeExplicitVisible(
            List<RenderedVirtualCell> rows,
            TabColumn column,
            int columnIndex,
            TabCell configured,
            String rendered) {

        int startRow =
            configured.getConfiguredRow()
                - 1;

        if (startRow < 0
                || startRow >= rows.size()) {

            return;
        }

        String[] split =
            split(
                rendered
            );

        for (int offset = 0;
                offset < split.length;
                offset++) {

            int row =
                startRow + offset;

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
            TabColumn column,
            int columnIndex,
            TabCell configured,
            String rendered,
            String blankText) {

        int startRow =
            configured.getConfiguredRow()
                - 1;

        if (startRow < 0
                || startRow >= rows.size()) {

            return;
        }

        String[] split =
            split(
                rendered
            );

        for (int offset = 0;
                offset < split.length;
                offset++) {

            int row =
                startRow + offset;

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
            TabColumn column,
            int columnIndex,
            TabCell configured,
            String rendered,
            int startSearchRow) {

        String[] split =
            split(
                rendered
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

    private static String[] split(
            String rendered) {

        return rendered == null
            ? new String[] {""}
            : rendered.split(
                "\\n",
                -1
            );
    }

    private static List<RenderedVirtualCell> emptyRows(
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

    private static long ticksToMillis(
            long ticks) {

        return Math.max(
            1L,
            ticks
        ) * 50L;
    }

    private static LayoutRenderResult emptyResult() {

        return new LayoutRenderResult(
            Collections.<RenderedVirtualCell>emptyList(),
            Collections.<LayoutDecision>emptyList()
        );
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

    private static final class ResolvedConfiguredCell {

        private final boolean visible;
        private final String renderedText;
        private final String reason;

        private ResolvedConfiguredCell(
                boolean visible,
                String renderedText,
                String reason) {

            this.visible =
                visible;

            this.renderedText =
                renderedText == null
                    ? ""
                    : renderedText;

            this.reason =
                reason == null
                    ? ""
                    : reason;
        }
    }

    private static final class ConditionResult {

        private final boolean visible;
        private final String reason;

        private ConditionResult(
                boolean visible,
                String reason) {

            this.visible =
                visible;

            this.reason =
                reason == null
                    ? ""
                    : reason;
        }
    }
}
