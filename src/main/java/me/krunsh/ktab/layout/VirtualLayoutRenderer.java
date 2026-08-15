package me.krunsh.ktab.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.render.PlaceholderRenderer;

/**
 * Transforme la configuration en cellules virtuelles finales.
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

        List<List<RenderedVirtualCell>> renderedColumns =
            new ArrayList<List<RenderedVirtualCell>>();

        int rowCount =
            config.getVirtualRows();

        for (int i = 0;
                i < columnCount;
                i++) {

            List<RenderedVirtualCell> lines =
                renderColumn(
                    viewer,
                    columns.get(i)
                );

            renderedColumns.add(
                lines
            );

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

        for (int column = 0;
                column < columnCount;
                column++) {

            List<RenderedVirtualCell> lines =
                renderedColumns.get(
                    column
                );

            for (int row = 0;
                    row < rowCount
                        && entries.size()
                            < entryLimit;
                    row++) {

                RenderedVirtualCell cell =
                    row < lines.size()
                        ? lines.get(row)
                        : new RenderedVirtualCell(
                            blankText,
                            config.getVirtualBlankSkinId()
                        );

                String value =
                    cell.getText();

                if (value == null
                        || value.trim()
                            .isEmpty()) {

                    value =
                        blankText;
                }

                entries.add(
                    new RenderedVirtualCell(
                        prefix
                            + value
                            + suffix,
                        cell.getSkinId()
                    )
                );
            }
        }

        return entries;
    }

    private List<RenderedVirtualCell> renderColumn(
            Player viewer,
            TabColumn column) {

        List<RenderedVirtualCell> result =
            new ArrayList<RenderedVirtualCell>();

        TabCell title =
            column.getTitle();

        if (title != null
                && title.getText() != null
                && !title.getText()
                    .trim()
                    .isEmpty()) {

            addSplit(
                result,
                renderer.render(
                    viewer,
                    title.getText(),
                    config.isPlaceholderApiEnabled()
                ),
                title.getSkinId()
            );
        }

        for (TabCell rawCell
                : column.getLines()) {

            if (rawCell == null) {
                continue;
            }

            addSplit(
                result,
                renderer.render(
                    viewer,
                    rawCell.getText(),
                    config.isPlaceholderApiEnabled()
                ),
                rawCell.getSkinId()
            );
        }

        return result;
    }

    private static void addSplit(
            List<RenderedVirtualCell> target,
            String value,
            String skinId) {

        if (value == null) {

            target.add(
                new RenderedVirtualCell(
                    "",
                    skinId
                )
            );

            return;
        }

        String[] split =
            value.split(
                "\\n",
                -1
            );

        for (String line : split) {

            target.add(
                new RenderedVirtualCell(
                    line,
                    skinId
                )
            );
        }
    }
}
