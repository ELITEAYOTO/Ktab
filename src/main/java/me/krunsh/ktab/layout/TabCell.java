package me.krunsh.ktab.layout;

/**
 * Cellule configurée avant rendu PlaceholderAPI.
 */
public final class TabCell {

    private final String text;
    private final String skinId;

    public TabCell(
            String text,
            String skinId) {

        this.text =
            text == null
                ? ""
                : text;

        this.skinId =
            skinId == null
                ? ""
                : skinId;
    }

    public String getText() {
        return text;
    }

    public String getSkinId() {
        return skinId;
    }
}
