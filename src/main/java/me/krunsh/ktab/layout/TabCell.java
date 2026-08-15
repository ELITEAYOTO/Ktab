package me.krunsh.ktab.layout;

/**
 * Cellule configurée avant rendu PlaceholderAPI.
 *
 * configuredRow utilise une numérotation utilisateur 1..20.
 * 0 signifie "placement automatique".
 */
public final class TabCell {

    private final String text;
    private final String skinId;
    private final int configuredRow;

    public TabCell(
            String text,
            String skinId) {

        this(
            text,
            skinId,
            0
        );
    }

    public TabCell(
            String text,
            String skinId,
            int configuredRow) {

        this.text =
            text == null
                ? ""
                : text;

        this.skinId =
            skinId == null
                ? ""
                : skinId;

        this.configuredRow =
            configuredRow;
    }

    public String getText() {
        return text;
    }

    public String getSkinId() {
        return skinId;
    }

    public int getConfiguredRow() {
        return configuredRow;
    }

    public boolean hasExplicitRow() {
        return configuredRow != 0;
    }
}
