package me.krunsh.ktab.visibility;

import java.util.UUID;

/**
 * Profil minimal présent dans le PlayerInfo client.
 *
 * Ktab n'a besoin que de l'UUID et du nom technique pour retirer une entrée
 * du TAB sans toucher à l'entité visible dans le monde.
 */
public final class TabProfile {

    private final UUID uuid;
    private final String name;

    public TabProfile(
            UUID uuid,
            String name) {

        if (uuid == null) {
            throw new IllegalArgumentException(
                "UUID TabProfile manquant."
            );
        }

        this.uuid = uuid;

        String safeName =
            name == null
                ? ""
                : name;

        if (safeName.length() > 16) {
            safeName =
                safeName.substring(
                    0,
                    16
                );
        }

        this.name = safeName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }
}
