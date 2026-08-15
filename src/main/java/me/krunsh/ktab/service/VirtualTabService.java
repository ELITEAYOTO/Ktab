package me.krunsh.ktab.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.layout.VirtualLayoutRenderer;
import me.krunsh.ktab.packet.VirtualEntry;
import me.krunsh.ktab.packet.VirtualTabPacketSender;
import me.krunsh.ktab.render.PlaceholderRenderer;

/**
 * Service des entrées virtuelles du TAB.
 *
 * V3 :
 * - refresh ciblé ;
 * - refresh global immédiat après join/quit ;
 * - inspection du cache par viewer ;
 * - le diff ADD/UPDATE/REMOVE reste inchangé.
 */
public final class VirtualTabService {

    private final KtabPlugin plugin;
    private final KtabConfig config;

    private final VirtualLayoutRenderer layoutRenderer;
    private final VirtualTabPacketSender packetSender;

    private final Map<UUID, List<VirtualEntry>> cache =
        new HashMap<UUID, List<VirtualEntry>>();

    private BukkitTask task;

    private long lastCycleMillis;
    private int lastAdds;
    private int lastUpdates;
    private int lastRemoves;

    public VirtualTabService(
            KtabPlugin plugin,
            KtabConfig config,
            PlaceholderRenderer renderer) {

        if (plugin == null
                || config == null
                || renderer == null) {

            throw new IllegalArgumentException(
                "Dépendance VirtualTabService manquante."
            );
        }

        this.plugin = plugin;
        this.config = config;

        layoutRenderer =
            new VirtualLayoutRenderer(
                config,
                renderer
            );

        packetSender =
            new VirtualTabPacketSender();
    }

    public void start() {

        stopTask();
        clearAll();

        if (!config.isEnabled()
                || !config.isVirtualLayoutEnabled()) {

            return;
        }

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
                    5L,
                    config.getVirtualUpdateIntervalTicks()
                );

        plugin.getLogger().info(
            "VirtualTabService actif - NMS="
                + packetSender.getNmsVersion()
                + ", interval="
                + config.getVirtualUpdateIntervalTicks()
                + " ticks."
        );
    }

    public void restart() {
        start();
    }

    public void shutdown() {

        stopTask();
        clearAll();
    }

    public void refresh(
            Player viewer) {

        if (viewer == null
                || !viewer.isOnline()
                || !config.isEnabled()
                || !config.isVirtualLayoutEnabled()) {

            return;
        }

        updateViewer(
            viewer,
            Bukkit.getOnlinePlayers()
                .size()
        );
    }

    public void refreshAll() {

        if (!config.isEnabled()
                || !config.isVirtualLayoutEnabled()) {

            return;
        }

        int onlinePlayers =
            Bukkit.getOnlinePlayers()
                .size();

        for (Player viewer
                : Bukkit.getOnlinePlayers()) {

            if (viewer != null
                    && viewer.isOnline()) {

                updateViewer(
                    viewer,
                    onlinePlayers
                );
            }
        }
    }

    public void clear(
            Player viewer) {

        if (viewer == null) {
            return;
        }

        List<VirtualEntry> previous =
            cache.remove(
                viewer.getUniqueId()
            );

        if (previous == null) {
            return;
        }

        if (!viewer.isOnline()) {
            return;
        }

        for (VirtualEntry entry : previous) {
            safeRemove(
                viewer,
                entry
            );
        }
    }

    public void removeCache(
            UUID viewerId) {

        if (viewerId != null) {
            cache.remove(viewerId);
        }
    }

    public void clearAll() {

        for (Player viewer
                : Bukkit.getOnlinePlayers()) {

            clear(viewer);
        }

        cache.clear();
    }

    public List<String> preview(
            Player viewer) {

        if (viewer == null) {
            return Collections.emptyList();
        }

        return layoutRenderer.render(
            viewer,
            Bukkit.getOnlinePlayers()
                .size()
        );
    }

    public int getCachedViewerCount() {
        return cache.size();
    }

    public int getCachedEntryCount(
            UUID viewerId) {

        if (viewerId == null) {
            return 0;
        }

        List<VirtualEntry> entries =
            cache.get(viewerId);

        return entries == null
            ? 0
            : entries.size();
    }

    public long getLastCycleMillis() {
        return lastCycleMillis;
    }

    public int getLastAdds() {
        return lastAdds;
    }

    public int getLastUpdates() {
        return lastUpdates;
    }

    public int getLastRemoves() {
        return lastRemoves;
    }

    private void tick() {

        long started =
            System.nanoTime();

        lastAdds = 0;
        lastUpdates = 0;
        lastRemoves = 0;

        purgeOffline();

        int onlinePlayers =
            Bukkit.getOnlinePlayers()
                .size();

        for (Player viewer
                : Bukkit.getOnlinePlayers()) {

            if (viewer == null
                    || !viewer.isOnline()) {

                continue;
            }

            updateViewer(
                viewer,
                onlinePlayers
            );
        }

        lastCycleMillis =
            Math.max(
                0L,
                (System.nanoTime() - started)
                    / 1000000L
            );
    }

    private void updateViewer(
            Player viewer,
            int onlinePlayers) {

        List<String> rendered =
            layoutRenderer.render(
                viewer,
                onlinePlayers
            );

        List<VirtualEntry> previous =
            cache.get(
                viewer.getUniqueId()
            );

        if (previous == null) {
            previous =
                Collections.emptyList();
        }

        List<VirtualEntry> next =
            new ArrayList<VirtualEntry>();

        int max =
            Math.max(
                previous.size(),
                rendered.size()
            );

        for (int index = 0;
                index < max;
                index++) {

            VirtualEntry old =
                index < previous.size()
                    ? previous.get(index)
                    : null;

            String text =
                index < rendered.size()
                    ? rendered.get(index)
                    : null;

            if (text == null) {

                if (old != null) {

                    safeRemove(
                        viewer,
                        old
                    );

                    lastRemoves++;
                }

                continue;
            }

            VirtualEntry entry =
                old == null
                    ? createEntry(
                        viewer,
                        index,
                        text
                    )
                    : old;

            if (old == null) {

                safeAdd(
                    viewer,
                    entry
                );

                lastAdds++;

            } else if (!text.equals(
                    old.getDisplayName())) {

                entry.setDisplayName(
                    text
                );

                safeUpdate(
                    viewer,
                    entry
                );

                lastUpdates++;
            }

            next.add(entry);
        }

        cache.put(
            viewer.getUniqueId(),
            next
        );
    }

    private VirtualEntry createEntry(
            Player viewer,
            int index,
            String text) {

        int stableIndex =
            config.getVirtualStartIndex()
                + index;

        String technicalName =
            config.getVirtualTechnicalPrefix()
                + leftPad(
                    stableIndex,
                    3
                );

        if (technicalName.length() > 16) {

            technicalName =
                technicalName.substring(
                    0,
                    16
                );
        }

        UUID uuid =
            UUID.nameUUIDFromBytes(
                (
                    config.getVirtualUuidSeed()
                        + ":"
                        + viewer.getUniqueId()
                        + ":"
                        + index
                ).getBytes(
                    StandardCharsets.UTF_8
                )
            );

        return new VirtualEntry(
            index,
            uuid,
            technicalName,
            text
        );
    }

    private void purgeOffline() {

        List<UUID> remove =
            new ArrayList<UUID>();

        for (UUID viewerId
                : cache.keySet()) {

            Player player =
                Bukkit.getPlayer(
                    viewerId
                );

            if (player == null
                    || !player.isOnline()) {

                remove.add(
                    viewerId
                );
            }
        }

        for (UUID viewerId : remove) {
            cache.remove(viewerId);
        }
    }

    private void safeAdd(
            Player viewer,
            VirtualEntry entry) {

        try {

            packetSender.add(
                viewer,
                entry
            );

        } catch (RuntimeException failure) {

            plugin.getLogger()
                .warning(
                    failure.getMessage()
                );
        }
    }

    private void safeUpdate(
            Player viewer,
            VirtualEntry entry) {

        try {

            packetSender.update(
                viewer,
                entry
            );

        } catch (RuntimeException failure) {

            plugin.getLogger()
                .warning(
                    failure.getMessage()
                );
        }
    }

    private void safeRemove(
            Player viewer,
            VirtualEntry entry) {

        try {

            packetSender.remove(
                viewer,
                entry
            );

        } catch (RuntimeException failure) {

            plugin.getLogger()
                .warning(
                    failure.getMessage()
                );
        }
    }

    private void stopTask() {

        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private static String leftPad(
            int value,
            int width) {

        String raw =
            String.valueOf(
                Math.max(
                    0,
                    value
                )
            );

        StringBuilder builder =
            new StringBuilder();

        for (int i =
                raw.length();
                i < width;
                i++) {

            builder.append('0');
        }

        builder.append(raw);

        return builder.toString();
    }
}
