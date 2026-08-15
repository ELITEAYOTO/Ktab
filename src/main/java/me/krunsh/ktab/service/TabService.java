package me.krunsh.ktab.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.cache.RenderedTabCache;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.logging.KtabConsole;
import me.krunsh.ktab.packet.TabPacketSender;
import me.krunsh.ktab.render.PlaceholderRenderer;

/**
 * Service Header/Footer et player-list-name.
 *
 * V9.1 :
 * - plus aucune boucle globale interne ;
 * - KtabSchedulerService répartit les viewers sur les ticks ;
 * - le cache existant continue d'empêcher les packets inutiles.
 */
public final class TabService {

    private final KtabPlugin plugin;
    private final KtabConfig config;
    private final PlaceholderRenderer renderer;

    private final RenderedTabCache cache =
        new RenderedTabCache();

    private final TabPacketSender packetSender;

    private long lastCycleMillis;
    private int lastPacketCount;

    private long totalRefreshes;
    private long totalPackets;

    public TabService(
            KtabPlugin plugin,
            KtabConfig config,
            PlaceholderRenderer renderer) {

        if (plugin == null
                || config == null
                || renderer == null) {

            throw new IllegalArgumentException(
                "Dépendance Ktab manquante."
            );
        }

        this.plugin = plugin;
        this.config = config;
        this.renderer = renderer;

        packetSender =
            new TabPacketSender();
    }

    public void start() {

        cache.clear();

        if (!config.isEnabled()) {

            plugin.getLogger().info(
                "Ktab désactivé dans config.yml."
            );

            return;
        }

        KtabConsole.success(
            plugin,
            "TabService prêt - NMS="
                + packetSender.getNmsVersion()
                + ", scheduler=central V9."
        );
    }

    public void restart() {
        start();
    }

    public void shutdown() {

        resetListNames();
        cache.clear();
    }

    public void refresh(
            Player player) {

        if (player == null
                || !player.isOnline()
                || !config.isEnabled()) {

            return;
        }

        long started =
            System.nanoTime();

        boolean packet =
            renderAndSend(
                player,
                false
            );

        totalRefreshes++;

        lastPacketCount =
            packet
                ? 1
                : 0;

        if (packet) {
            totalPackets++;
        }

        lastCycleMillis =
            Math.max(
                0L,
                (System.nanoTime() - started)
                    / 1000000L
            );
    }

    public void refreshAll() {

        if (!config.isEnabled()) {
            return;
        }

        long started =
            System.nanoTime();

        int packets =
            0;

        List<UUID> onlineIds =
            new ArrayList<UUID>();

        for (Player player
                : Bukkit.getOnlinePlayers()) {

            if (player == null
                    || !player.isOnline()) {

                continue;
            }

            onlineIds.add(
                player.getUniqueId()
            );

            if (renderAndSend(
                    player,
                    false)) {

                packets++;
            }

            totalRefreshes++;
        }

        cache.retainOnly(
            onlineIds
        );

        lastPacketCount =
            packets;

        totalPackets +=
            packets;

        lastCycleMillis =
            Math.max(
                0L,
                (System.nanoTime() - started)
                    / 1000000L
            );
    }

    public void remove(
            UUID playerId) {

        if (playerId != null) {
            cache.remove(playerId);
        }
    }

    public boolean isCached(
            UUID playerId) {

        if (playerId == null) {
            return false;
        }

        return cache.contains(
            playerId
        );
    }

    public int getCachedViewerCount() {
        return cache.size();
    }

    public long getLastCycleMillis() {
        return lastCycleMillis;
    }

    public int getLastPacketCount() {
        return lastPacketCount;
    }

    public long getTotalRefreshes() {
        return totalRefreshes;
    }

    public long getTotalPackets() {
        return totalPackets;
    }

    public void resetPerformanceMetrics() {

        totalRefreshes = 0L;
        totalPackets = 0L;

        lastPacketCount = 0;
        lastCycleMillis = 0L;
    }

    private boolean renderAndSend(
            Player player,
            boolean force) {

        String header =
            renderer.renderLines(
                player,
                config.getHeaderLines(),
                config.isPlaceholderApiEnabled()
            );

        String footer =
            renderer.renderLines(
                player,
                config.getFooterLines(),
                config.isPlaceholderApiEnabled()
            );

        String listName =
            renderListName(
                player
            );

        boolean changed =
            cache.changed(
                player.getUniqueId(),
                header,
                footer,
                listName
            );

        if (!changed && !force) {
            return false;
        }

        packetSender.send(
            player,
            header,
            footer
        );

        if (config.isPlayerListNameEnabled()
                && !listName.equals(
                    player.getPlayerListName()
                )) {

            player.setPlayerListName(
                listName
            );
        }

        return true;
    }

    private String renderListName(
            Player player) {

        if (!config.isPlayerListNameEnabled()) {
            return player.getName();
        }

        String rendered =
            renderer.render(
                player,
                config.getPlayerListNameFormat(),
                config.isPlaceholderApiEnabled()
            );

        int maxLength =
            config.getPlayerListNameMaxLength();

        if (rendered.length()
                <= maxLength) {

            return rendered;
        }

        return rendered.substring(
            0,
            maxLength
        );
    }

    private void resetListNames() {

        for (Player player
                : Bukkit.getOnlinePlayers()) {

            if (player != null
                    && player.isOnline()) {

                player.setPlayerListName(
                    player.getName()
                );
            }
        }
    }
}
