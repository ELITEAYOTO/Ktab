package me.krunsh.ktab.visibility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.integration.ServerNpcHook;
import me.krunsh.ktab.packet.TabVisibilityPacketSender;

/**
 * Contrôle les entrées non-Ktab présentes dans la liste client.
 *
 * Il ne masque PAS les entités dans le monde :
 * seul PacketPlayOutPlayerInfo REMOVE_PLAYER est envoyé.
 */
public final class TabVisibilityController {

    private final KtabPlugin plugin;
    private final KtabConfig config;

    private final ServerNpcHook serverNpcHook;
    private final TabVisibilityPacketSender packetSender;

    private int lastHiddenRealPlayers;
    private int lastHiddenNpcs;

    public TabVisibilityController(
            KtabPlugin plugin,
            KtabConfig config) {

        if (plugin == null
                || config == null) {

            throw new IllegalArgumentException(
                "Dépendance TabVisibilityController manquante."
            );
        }

        this.plugin = plugin;
        this.config = config;

        serverNpcHook =
            new ServerNpcHook(
                plugin
            );

        packetSender =
            new TabVisibilityPacketSender();
    }

    public void refreshHooks() {
        serverNpcHook.refresh();
    }

    public void apply(
            Player viewer) {

        if (viewer == null
                || !viewer.isOnline()
                || !config.isEnabled()
                || !config.isVirtualLayoutEnabled()) {

            return;
        }

        Map<UUID, TabProfile> profiles =
            new LinkedHashMap<UUID, TabProfile>();

        int realPlayers =
            0;

        int npcs =
            0;

        if (config.isHideRealPlayers()) {

            for (Player player
                    : Bukkit.getOnlinePlayers()) {

                if (player == null) {
                    continue;
                }

                profiles.put(
                    player.getUniqueId(),
                    new TabProfile(
                        player.getUniqueId(),
                        player.getName()
                    )
                );

                realPlayers++;
            }
        }

        if (config.isHideServerNpcs()
                && serverNpcHook.isAvailable()) {

            List<TabProfile> npcProfiles =
                serverNpcHook
                    .getNpcProfiles();

            for (TabProfile profile
                    : npcProfiles) {

                if (profile == null) {
                    continue;
                }

                profiles.put(
                    profile.getUuid(),
                    profile
                );

                npcs++;
            }
        }

        if (!profiles.isEmpty()) {

            try {

                packetSender.removeProfiles(
                    viewer,
                    profiles.values()
                );

            } catch (RuntimeException failure) {

                plugin.getLogger()
                    .warning(
                        failure.getMessage()
                    );
            }
        }

        lastHiddenRealPlayers =
            realPlayers;

        lastHiddenNpcs =
            npcs;
    }

    public void applyAll() {

        for (Player viewer
                : Bukkit.getOnlinePlayers()) {

            apply(viewer);
        }
    }

    /**
     * Réaffiche uniquement les vrais joueurs.
     *
     * ServerNPC reste responsable de ses propres NPC.
     */
    public void restoreRealPlayers() {

        Collection<? extends Player> players =
            Bukkit.getOnlinePlayers();

        if (players.isEmpty()) {
            return;
        }

        for (Player viewer : players) {

            if (viewer == null
                    || !viewer.isOnline()) {

                continue;
            }

            try {

                packetSender.addRealPlayers(
                    viewer,
                    players
                );

            } catch (RuntimeException failure) {

                plugin.getLogger()
                    .warning(
                        failure.getMessage()
                    );
            }
        }
    }

    public boolean isServerNpcAvailable() {
        return serverNpcHook.isAvailable();
    }

    public int getLastHiddenRealPlayers() {
        return lastHiddenRealPlayers;
    }

    public int getLastHiddenNpcs() {
        return lastHiddenNpcs;
    }

    public String getNmsVersion() {
        return packetSender.getNmsVersion();
    }
}
