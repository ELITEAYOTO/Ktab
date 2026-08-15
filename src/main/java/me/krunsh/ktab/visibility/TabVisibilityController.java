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
 * V9.1 :
 * - masquage ciblé au join ;
 * - construction d'un batch de profils une seule fois par sweep ;
 * - aucun apply() appelé à chaque rendu du VirtualTabService.
 */
public final class TabVisibilityController {

    private final KtabPlugin plugin;
    private final KtabConfig config;

    private final ServerNpcHook serverNpcHook;
    private final TabVisibilityPacketSender packetSender;

    private int lastHiddenRealPlayers;
    private int lastHiddenNpcs;

    private long targetedHideOperations;
    private long fullSweeps;
    private long npcSweeps;
    private long packetsSent;
    private long profileEntriesSent;

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

    /**
     * Compatibilité avec les anciennes phases.
     */
    public void apply(
            Player viewer) {

        hideExistingFrom(
            viewer
        );
    }

    /**
     * Masque pour un nouveau viewer les vrais joueurs actuellement connectés
     * ainsi que les NPC ServerNPC.
     *
     * Cette opération est O(n) en construction de profils, mais elle n'est
     * exécutée qu'au join/reload/fallback et non plus à chaque refresh.
     */
    public void hideExistingFrom(
            Player viewer) {

        if (!canApply(viewer)) {
            return;
        }

        ProfileBatch batch =
            buildCombinedBatch();

        sendProfiles(
            viewer,
            batch.profiles
        );

        lastHiddenRealPlayers =
            batch.realPlayers;

        lastHiddenNpcs =
            batch.npcs;

        targetedHideOperations++;
    }

    /**
     * Quand un vrai joueur rejoint, les viewers déjà connectés n'ont besoin
     * de retirer QUE ce nouveau profil, pas les 699 autres.
     */
    public void hideRealPlayerFromOthers(
            Player joined) {

        if (joined == null
                || !joined.isOnline()
                || !config.isEnabled()
                || !config.isVirtualLayoutEnabled()
                || !config.isHideRealPlayers()) {

            return;
        }

        List<TabProfile> one =
            new ArrayList<TabProfile>(
                1
            );

        one.add(
            new TabProfile(
                joined.getUniqueId(),
                joined.getName()
            )
        );

        for (Player viewer
                : Bukkit.getOnlinePlayers()) {

            if (viewer == null
                    || !viewer.isOnline()
                    || viewer.getUniqueId()
                        .equals(
                            joined.getUniqueId()
                        )) {

                continue;
            }

            sendProfiles(
                viewer,
                one
            );
        }

        lastHiddenRealPlayers = 1;
        targetedHideOperations++;
    }

    /**
     * Sweep complet optimisé :
     * la liste des profils est construite une seule fois puis réutilisée pour
     * tous les viewers.
     */
    public void applyAllBatched() {

        if (!config.isEnabled()
                || !config.isVirtualLayoutEnabled()) {

            return;
        }

        ProfileBatch batch =
            buildCombinedBatch();

        if (batch.profiles.isEmpty()) {
            return;
        }

        for (Player viewer
                : Bukkit.getOnlinePlayers()) {

            if (viewer == null
                    || !viewer.isOnline()) {

                continue;
            }

            sendProfiles(
                viewer,
                batch.profiles
            );
        }

        lastHiddenRealPlayers =
            batch.realPlayers;

        lastHiddenNpcs =
            batch.npcs;

        fullSweeps++;
    }

    /**
     * Compatibilité avec les anciennes commandes.
     */
    public void applyAll() {
        applyAllBatched();
    }

    /**
     * Sweep léger réservé aux NPC.
     *
     * Utile car certains plugins NPC peuvent réinjecter une entrée PlayerInfo
     * après le spawn/chargement de skin.
     */
    public void hideServerNpcsAll() {

        if (!config.isEnabled()
                || !config.isVirtualLayoutEnabled()
                || !config.isHideServerNpcs()
                || !serverNpcHook.isAvailable()) {

            return;
        }

        List<TabProfile> npcProfiles =
            serverNpcHook.getNpcProfiles();

        if (npcProfiles.isEmpty()) {
            return;
        }

        for (Player viewer
                : Bukkit.getOnlinePlayers()) {

            if (viewer == null
                    || !viewer.isOnline()) {

                continue;
            }

            sendProfiles(
                viewer,
                npcProfiles
            );
        }

        lastHiddenNpcs =
            npcProfiles.size();

        npcSweeps++;
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

    public long getTargetedHideOperations() {
        return targetedHideOperations;
    }

    public long getFullSweeps() {
        return fullSweeps;
    }

    public long getNpcSweeps() {
        return npcSweeps;
    }

    public long getPacketsSent() {
        return packetsSent;
    }

    public long getProfileEntriesSent() {
        return profileEntriesSent;
    }

    public void resetPerformanceMetrics() {

        targetedHideOperations = 0L;
        fullSweeps = 0L;
        npcSweeps = 0L;
        packetsSent = 0L;
        profileEntriesSent = 0L;
    }

    public String getNmsVersion() {
        return packetSender.getNmsVersion();
    }

    private boolean canApply(
            Player viewer) {

        return viewer != null
            && viewer.isOnline()
            && config.isEnabled()
            && config.isVirtualLayoutEnabled();
    }

    private ProfileBatch buildCombinedBatch() {

        Map<UUID, TabProfile> profiles =
            new LinkedHashMap<UUID, TabProfile>();

        int realPlayers =
            0;

        int npcs =
            0;

        if (config.isHideRealPlayers()) {

            for (Player player
                    : Bukkit.getOnlinePlayers()) {

                if (player == null
                        || !player.isOnline()) {

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
                serverNpcHook.getNpcProfiles();

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

        return new ProfileBatch(
            new ArrayList<TabProfile>(
                profiles.values()
            ),
            realPlayers,
            npcs
        );
    }

    private void sendProfiles(
            Player viewer,
            Collection<TabProfile> profiles) {

        if (viewer == null
                || !viewer.isOnline()
                || profiles == null
                || profiles.isEmpty()) {

            return;
        }

        try {

            packetSender.removeProfiles(
                viewer,
                profiles
            );

            packetsSent++;
            profileEntriesSent +=
                profiles.size();

        } catch (RuntimeException failure) {

            plugin.getLogger()
                .warning(
                    failure.getMessage()
                );
        }
    }

    private static final class ProfileBatch {

        private final List<TabProfile> profiles;
        private final int realPlayers;
        private final int npcs;

        private ProfileBatch(
                List<TabProfile> profiles,
                int realPlayers,
                int npcs) {

            this.profiles =
                profiles;

            this.realPlayers =
                realPlayers;

            this.npcs =
                npcs;
        }
    }
}
