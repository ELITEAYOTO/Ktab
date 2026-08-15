package me.krunsh.ktab.layout;

/**
 * Cellule finale après résolution du texte.
 *
 * columnIndex et rowIndex sont 0-based en interne.
 * Les getters display utilisent naturellement +1 dans les commandes.
 */
public final class RenderedVirtualCell {

    private final String text;
    private final String skinId;

    private final String columnId;
    private final int columnIndex;
    private final int rowIndex;

    public RenderedVirtualCell(
            String text,
            String skinId) {

        this(
            text,
            skinId,
            "",
            -1,
            -1
        );
    }

    public RenderedVirtualCell(
            String text,
            String skinId,
            String columnId,
            int columnIndex,
            int rowIndex) {

        this.text =
            text == null
                ? ""
                : text;

        this.skinId =
            skinId == null
                ? ""
                : skinId;

        this.columnId =
            columnId == null
                ? ""
                : columnId;

        this.columnIndex =
            columnIndex;

        this.rowIndex =
            rowIndex;
    }

    public String getText() {
        return text;
    }

    public String getSkinId() {
        return skinId;
    }

    public String getColumnId() {
        return columnId;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public int getDisplayColumn() {
        return columnIndex + 1;
    }

    public int getDisplayRow() {
        return rowIndex + 1;
    }
}
