package me.krunsh.ktab.layout;

/**
 * Cellule finale après résolution du texte.
 *
 * La skin reste référencée par ID jusqu'à VirtualTabService afin de pouvoir
 * résoudre les skins dynamiques comme "viewer".
 */
public final class RenderedVirtualCell {

    private final String text;
    private final String skinId;

    public RenderedVirtualCell(
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
