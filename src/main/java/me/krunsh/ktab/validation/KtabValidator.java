package me.krunsh.ktab.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.layout.TabCell;
import me.krunsh.ktab.layout.TabColumn;
import me.krunsh.ktab.validation.ValidationIssue.Severity;

/**
 * Validation statique de la configuration Ktab.
 *
 * Le validator n'envoie aucun packet et ne dépend d'aucun joueur connecté.
 */
public final class KtabValidator {

    private final KtabConfig config;

    public KtabValidator(
            KtabConfig config) {

        if (config == null) {
            throw new IllegalArgumentException(
                "KtabConfig manquante."
            );
        }

        this.config = config;
    }

    public List<ValidationIssue> validate() {

        List<ValidationIssue> issues =
            new ArrayList<ValidationIssue>();

        if (!config.isVirtualLayoutEnabled()) {
            return issues;
        }

        List<TabColumn> columns =
            config.getVirtualColumns();

        if (columns.isEmpty()) {

            error(
                issues,
                "virtual_layout.columns",
                "Aucune colonne active."
            );

            return issues;
        }

        if (columns.size()
                < config.getVirtualColumnsCount()) {

            warning(
                issues,
                "virtual_layout.columns_count",
                "columns_count="
                    + config.getVirtualColumnsCount()
                    + " mais seulement "
                    + columns.size()
                    + " colonne(s) active(s)."
            );
        }

        if (columns.size()
                > config.getVirtualColumnsCount()) {

            warning(
                issues,
                "virtual_layout.columns",
                (columns.size()
                    - config.getVirtualColumnsCount())
                    + " colonne(s) active(s) seront ignorées car columns_count="
                    + config.getVirtualColumnsCount()
                    + "."
            );
        }

        int renderedColumns =
            Math.min(
                columns.size(),
                config.getVirtualColumnsCount()
            );

        int totalCells =
            renderedColumns
                * config.getVirtualRows();

        if (config.getVirtualMaxEntries()
                < totalCells) {

            warning(
                issues,
                "virtual_layout.max_entries",
                "max_entries="
                    + config.getVirtualMaxEntries()
                    + " tronquera la grille "
                    + renderedColumns
                    + "x"
                    + config.getVirtualRows()
                    + " ("
                    + totalCells
                    + " cellules)."
            );
        }

        validateSkin(
            issues,
            "virtual_layout.default_skin",
            config.getVirtualDefaultSkinId()
        );

        validateSkin(
            issues,
            "virtual_layout.render.blank_skin",
            config.getVirtualBlankSkinId()
        );

        for (int columnIndex = 0;
                columnIndex < renderedColumns;
                columnIndex++) {

            validateColumn(
                issues,
                columns.get(columnIndex),
                columnIndex
            );
        }

        return issues;
    }

    private void validateColumn(
            List<ValidationIssue> issues,
            TabColumn column,
            int columnIndex) {

        String base =
            "virtual_layout.columns."
                + column.getId();

        validateSkin(
            issues,
            base + ".skin",
            column.getDefaultSkinId()
        );

        Map<Integer, String> explicitRows =
            new HashMap<Integer, String>();

        TabCell title =
            column.getTitle();

        if (title != null
                && !title.getText()
                    .trim()
                    .isEmpty()) {

            validateCell(
                issues,
                title,
                base + ".title",
                explicitRows
            );
        }

        int automaticCells = 0;

        int lineIndex = 0;

        for (TabCell cell
                : column.getLines()) {

            String path =
                base
                    + ".lines["
                    + lineIndex
                    + "]";

            validateCell(
                issues,
                cell,
                path,
                explicitRows
            );

            if (cell != null
                    && !cell.hasExplicitRow()) {

                automaticCells++;
            }

            lineIndex++;
        }

        int reservedExplicit =
            explicitRows.size();

        int freeRows =
            Math.max(
                0,
                config.getVirtualRows()
                    - reservedExplicit
            );

        if (automaticCells > freeRows) {

            warning(
                issues,
                base + ".lines",
                automaticCells
                    + " cellule(s) automatiques pour seulement "
                    + freeRows
                    + " ligne(s) libre(s). Les dernières seront ignorées."
            );
        }
    }

    private void validateCell(
            List<ValidationIssue> issues,
            TabCell cell,
            String path,
            Map<Integer, String> explicitRows) {

        if (cell == null) {
            return;
        }

        validateSkin(
            issues,
            path + ".skin",
            cell.getSkinId()
        );

        if (!cell.hasExplicitRow()) {
            return;
        }

        int row =
            cell.getConfiguredRow();

        if (row < 1
                || row > config.getVirtualRows()) {

            error(
                issues,
                path + ".row",
                "row="
                    + row
                    + " hors limites. Valeur autorisée: 1.."
                    + config.getVirtualRows()
                    + "."
            );

            return;
        }

        String previous =
            explicitRows.put(
                Integer.valueOf(row),
                path
            );

        if (previous != null) {

            error(
                issues,
                path + ".row",
                "Collision sur la ligne "
                    + row
                    + " avec "
                    + previous
                    + "."
            );
        }
    }

    private void validateSkin(
            List<ValidationIssue> issues,
            String path,
            String rawSkinId) {

        String skinId =
            normalize(
                rawSkinId
            );

        if (skinId.isEmpty()
                || "none".equals(skinId)
                || "viewer".equals(skinId)
                || "player".equals(skinId)
                || "default_minecraft".equals(skinId)
                || skinId.startsWith(
                    "player:")) {

            return;
        }

        if (config.getSkinDefinition(
                skinId) == null) {

            warning(
                issues,
                path,
                "Skin inconnue '"
                    + rawSkinId
                    + "'. La tête Minecraft par défaut sera utilisée."
            );
        }
    }

    private static String normalize(
            String value) {

        return value == null
            ? ""
            : value.trim()
                .toLowerCase(
                    Locale.ROOT
                );
    }

    private static void error(
            List<ValidationIssue> issues,
            String path,
            String message) {

        issues.add(
            new ValidationIssue(
                Severity.ERROR,
                path,
                message
            )
        );
    }

    private static void warning(
            List<ValidationIssue> issues,
            String path,
            String message) {

        issues.add(
            new ValidationIssue(
                Severity.WARNING,
                path,
                message
            )
        );
    }
}
