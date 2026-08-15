package me.krunsh.ktab.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.cache.RenderedTabCache;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.packet.TabPacketSender;
import me.krunsh.ktab.render.PlaceholderRenderer;

/**
 * Service Header/Footer et player-list-name.
 *
 * V3 :
 * - refresh ciblé ;
 * - refresh global à la demande ;
 * - invalidation immédiate au quit.
 */
public final class TabService {

    private final KtabPlugin plugin;
    private final KtabConfig config;
    private final PlaceholderRenderer renderer;

    private final RenderedTabCache cache =
        new RenderedTabCache();

    private final TabPacketSender packetSender;

    private BukkitTask task;

    private long lastCycleMillis;
    private int lastPacketCount;

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

        stopTask();
        cache.clear();

        if (!config.isEnabled()) {

            plugin.getLogger().info(
                "Ktab désactivé dans config.yml."
            );

            return;
        }

        long interval =
            config.getUpdateIntervalTicks();

        task =
            Bukkit.getScheduler()
                .runTaskTimer(
                    plugin,
                    new Runnable() {
                        @Override
                        public void run() {
                            tick();
                        }
                    },
                    1L,
                    interval
                );

        plugin.getLogger().info(
            "TabService actif - NMS="
                + packetSender.getNmsVersion()
                + ", interval="
                + interval
                + " ticks."
        );
    }

    public void restart() {
        start();
    }

    public void shutdown() {

        stopTask();
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

        renderAndSend(
            player,
            false
        );
    }

    public void refreshAll() {

        if (!config.isEnabled()) {
            return;
        }

        for (Player player
                : Bukkit.getOnlinePlayers()) {

            refresh(player);
        }
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

    private void tick() {

        long started =
            System.nanoTime();

        int packets = 0;

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
        }

        cache.retainOnly(
            onlineIds
        );

        lastPacketCount =
            packets;

        lastCycleMillis =
            Math.max(
                0L,
                (System.nanoTime() - started)
                    / 1000000L
            );
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

    private void stopTask() {

        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
