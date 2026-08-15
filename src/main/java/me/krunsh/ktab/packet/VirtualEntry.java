package me.krunsh.ktab.packet;

import java.util.UUID;

import me.krunsh.ktab.skin.ResolvedTabSkin;

/**
 * Identité stable d'une entrée fake du TAB.
 */
public final class VirtualEntry {

    private final int index;
    private final UUID uuid;
    private final String technicalName;

    private String displayName;
    private final ResolvedTabSkin skin;

    public VirtualEntry(
            int index,
            UUID uuid,
            String technicalName,
            String displayName,
            ResolvedTabSkin skin) {

        this.index = index;
        this.uuid = uuid;

        this.technicalName =
            technicalName == null
                ? ""
                : technicalName;

        this.displayName =
            displayName == null
                ? ""
                : displayName;

        this.skin =
            skin == null
                ? ResolvedTabSkin.NONE
                : skin;
    }

    public int getIndex() {
        return index;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getTechnicalName() {
        return technicalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ResolvedTabSkin getSkin() {
        return skin;
    }

    public void setDisplayName(
            String displayName) {

        this.displayName =
            displayName == null
                ? ""
                : displayName;
    }
}
