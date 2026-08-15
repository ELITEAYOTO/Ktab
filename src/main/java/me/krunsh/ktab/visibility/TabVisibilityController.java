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
import me.krunsh.ktab.logging.KtabConsole;
import me.krunsh.ktab.packet.TabVisibilityPacketSender;

/**
 * Contrôle les entrées non-Ktab présentes dans la liste client.
 *
 * V9.4 :
 * - masquage réel event-driven conservé ;
 * - snapshot ServerNPC mémorisé ;
 * - scans NPC en delta : aucun packet si aucun UUID nouveau ;
 * - force-rehide basse fréquence configurable comme filet de sécurité.
 */
public final class TabVisibilityController {

    private final KtabPlugin plugin;
    private final KtabConfig config;

    private final ServerNpcHook serverNpcHook;
    private final TabVisibilityPacketSender packetSender;

    private final Map<UUID, TabProfile> npcSnapshot =
        new LinkedHashMap<UUID, TabProfile>();

    private int lastHiddenRealPlayers;
    private int lastHiddenNpcs;

    private long targetedHideOperations;
    private long fullSweeps;
    private long npcSweeps;
    private long packetsSent;
    private long profileEntriesSent;

    private long npcScans;
    private long npcNoChangeScans;
    private long npcDeltaAdds;
    private long npcForceRehides;

    public TabVisibilityController(
            KtabPlugin plugin,
            KtabConfig config) {

        if (plugin == null
                || config == null) {

            throw new IllegalArgumentException(
                "Dépendance TabVisibilityController manquante."
            );
        }

        this.plugin =
            plugin;

        this.config =
            config;

        serverNpcHook =
            new ServerNpcHook(
                plugin
            );

        packetSender =
            new TabVisibilityPacketSender();
    }

    public void refreshHooks() {

        serverNpcHook.refresh();
        npcSnapshot.clear();
    }

    public void apply(
            Player viewer) {

        hideExistingFrom(
            viewer
        );
    }

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

        lastHiddenRealPlayers =
            1;

        targetedHideOperations++;
    }

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

    public void applyAll() {
        applyAllBatched();
    }

    /**
     * Compatibilité : ancien sweep NPC devient un force-rehide explicite.
     */
    public void hideServerNpcsAll() {
        auditServerNpcsDelta(
            true
        );
    }

    /**
     * Compare la liste actuelle ServerNPC au snapshot précédent.
     *
     * force=false :
     *   seulement les nouveaux UUID sont retirés des viewers.
     *
     * force=true :
     *   tous les NPC actuels sont retirés une fois, utile si ServerNPC a
     *   réinjecté un PlayerInfo sans changer son UUID.
     */
    public void auditServerNpcsDelta(
            boolean force) {

        if (!config.isEnabled()
                || !config.isVirtualLayoutEnabled()
                || !config.isHideServerNpcs()
                || !serverNpcHook.isAvailable()) {

            return;
        }

        npcScans++;

        List<TabProfile> currentList =
            serverNpcHook.getNpcProfiles();

        Map<UUID, TabProfile> current =
            toMap(
                currentList
            );

        List<TabProfile> toHide =
            new ArrayList<TabProfile>();

        if (force) {

            toHide.addAll(
                current.values()
            );

            npcForceRehides++;

        } else {

            for (Map.Entry<UUID, TabProfile> entry
                    : current.entrySet()) {

                if (!npcSnapshot.containsKey(
                        entry.getKey())) {

                    toHide.add(
                        entry.getValue()
                    );
                }
            }
        }

        if (toHide.isEmpty()) {

            npcNoChangeScans++;

        } else {

            for (Player viewer
                    : Bukkit.getOnlinePlayers()) {

                if (viewer == null
                        || !viewer.isOnline()) {

                    continue;
                }

                sendProfiles(
                    viewer,
                    toHide
                );
            }

            npcDeltaAdds +=
                force
                    ? 0L
                    : toHide.size();

            npcSweeps++;
        }

        npcSnapshot.clear();
        npcSnapshot.putAll(
            current
        );

        lastHiddenNpcs =
            current.size();
    }

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

                KtabConsole.warning(
                    plugin,
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

    public long getNpcScans() {
        return npcScans;
    }

    public long getNpcNoChangeScans() {
        return npcNoChangeScans;
    }

    public long getNpcDeltaAdds() {
        return npcDeltaAdds;
    }

    public long getNpcForceRehides() {
        return npcForceRehides;
    }

    public long getServerNpcReflectionResolves() {
        return serverNpcHook
            .getReflectionResolves();
    }

    public long getServerNpcReadFailures() {
        return serverNpcHook
            .getReadFailures();
    }

    public void resetPerformanceMetrics() {

        targetedHideOperations =
            0L;

        fullSweeps =
            0L;

        npcSweeps =
            0L;

        packetsSent =
            0L;

        profileEntriesSent =
            0L;

        npcScans =
            0L;

        npcNoChangeScans =
            0L;

        npcDeltaAdds =
            0L;

        npcForceRehides =
            0L;

        serverNpcHook
            .resetMetrics();
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

            Map<UUID, TabProfile> current =
                toMap(
                    npcProfiles
                );

            npcSnapshot.clear();
            npcSnapshot.putAll(
                current
            );

            for (TabProfile profile
                    : current.values()) {

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

    private Map<UUID, TabProfile> toMap(
            Collection<TabProfile> profiles) {

        Map<UUID, TabProfile> result =
            new LinkedHashMap<UUID, TabProfile>();

        if (profiles == null) {
            return result;
        }

        for (TabProfile profile : profiles) {

            if (profile == null
                    || profile.getUuid() == null) {

                continue;
            }

            result.put(
                profile.getUuid(),
                profile
            );
        }

        return result;
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

            KtabConsole.warning(
                plugin,
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
