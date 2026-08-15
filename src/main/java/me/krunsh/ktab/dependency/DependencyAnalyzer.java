package me.krunsh.ktab.dependency;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import me.krunsh.ktab.condition.TabCondition;
import me.krunsh.ktab.condition.TabConditionGroup;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.layout.TabCell;
import me.krunsh.ktab.layout.TabColumn;
import me.krunsh.ktab.render.PlaceholderRenderer;

/**
 * Analyse de dépendances mise en cache par objet de configuration.
 */
public final class DependencyAnalyzer {

    private final KtabConfig config;
    private final PlaceholderRenderer renderer;

    private final Map<TabCell, DependencySet> cellCache =
        new IdentityHashMap<TabCell, DependencySet>();

    private final Map<TabColumn, DependencySet> columnCache =
        new IdentityHashMap<TabColumn, DependencySet>();

    public DependencyAnalyzer(
            KtabConfig config,
            PlaceholderRenderer renderer) {

        this.config = config;
        this.renderer = renderer;
    }

    public void clear() {
        cellCache.clear();
        columnCache.clear();
    }

    public DependencySet analyzeCell(
            TabCell cell) {

        if (cell == null) {
            return new DependencySet();
        }

        DependencySet cached =
            cellCache.get(cell);

        if (cached != null) {
            return cached;
        }

        DependencySet result =
            analyzeText(
                cell.getText()
            );

        result.merge(
            analyzeConditions(
                cell.getConditions()
            )
        );

        cellCache.put(
            cell,
            result
        );

        return result;
    }

    public DependencySet analyzeColumn(
            TabColumn column) {

        if (column == null) {
            return new DependencySet();
        }

        DependencySet cached =
            columnCache.get(column);

        if (cached != null) {
            return cached;
        }

        DependencySet result =
            analyzeConditions(
                column.getConditions()
            );

        columnCache.put(
            column,
            result
        );

        return result;
    }

    private DependencySet analyzeConditions(
            TabConditionGroup group) {

        DependencySet result =
            new DependencySet();

        if (group == null
                || group.isEmpty()) {

            return result;
        }

        for (TabCondition condition
                : group.getConditions()) {

            if (condition == null) {
                continue;
            }

            String type =
                condition.getType();

            if ("permission".equals(type)
                    || "not_permission".equals(type)) {

                result.markPermission();
                result.markDynamic();
                result.includeTtl(
                    config
                        .getPerformanceRenderPermissionTtlTicks()
                );
            }

            if ("online_min".equals(type)
                    || "online_max".equals(type)) {

                result.markGlobal();
            }

            result.merge(
                analyzeText(
                    condition.getInput()
                )
            );

            result.merge(
                analyzeText(
                    condition.getValue()
                )
            );
        }

        return result;
    }

    private DependencySet analyzeText(
            String raw) {

        DependencySet result =
            new DependencySet();

        List<String> tokens =
            renderer.getPlaceholderTokens(
                raw
            );

        for (String token : tokens) {

            if ("%server_online%".equalsIgnoreCase(
                    token)
                    || "%server_max_players%"
                        .equalsIgnoreCase(
                            token)) {

                result.markGlobal();
                continue;
            }

            if ("%player_name%".equalsIgnoreCase(
                    token)) {

                continue;
            }

            result.markDynamic();

            result.includeTtl(
                renderer
                    .getConfiguredTtlTicks(
                        token
                    )
            );
        }

        return result;
    }
}
