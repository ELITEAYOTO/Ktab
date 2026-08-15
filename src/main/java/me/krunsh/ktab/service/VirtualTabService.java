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
import me.krunsh.ktab.layout.RenderedVirtualCell;
import me.krunsh.ktab.layout.VirtualLayoutRenderer;
import me.krunsh.ktab.packet.VirtualEntry;
import me.krunsh.ktab.packet.VirtualTabPacketSender;
import me.krunsh.ktab.render.PlaceholderRenderer;
import me.krunsh.ktab.skin.ResolvedTabSkin;
import me.krunsh.ktab.skin.TabSkinResolver;
import me.krunsh.ktab.visibility.TabVisibilityController;

/**
 * Service des entrées virtuelles du TAB.
 *
 * V6 :
 * - texte changé seul -> UPDATE_DISPLAY_NAME ;
 * - skin changée -> REMOVE_PLAYER + ADD_PLAYER ;
 * - cache de résolution des skins configurées ;
 * - preview temporaire d'une skin sur la première cellule.
 */
public final class VirtualTabService {

    private final KtabPlugin plugin;
    private final KtabConfig config;

    private final VirtualLayoutRenderer layoutRenderer;
    private final VirtualTabPacketSender packetSender;
    private final TabVisibilityController visibilityController;
    private final TabSkinResolver skinResolver;

    private final Map<UUID, List<VirtualEntry>> cache =
        new HashMap<UUID, List<VirtualEntry>>();

    private final Map<UUID, SkinPreview> skinPreviews =
        new HashMap<UUID, SkinPreview>();

    private BukkitTask task;

    private long lastCycleMillis;
    private int lastAdds;
    private int lastUpdates;
    private int lastRemoves;

    public VirtualTabService(
            KtabPlugin plugin,
            KtabConfig config,
            PlaceholderRenderer renderer,
            TabVisibilityController visibilityController) {

        if (plugin == null
                || config == null
                || renderer == null
                || visibilityController == null) {

            throw new IllegalArgumentException(
                "Dépendance VirtualTabService manquante."
            );
        }

        this.plugin = plugin;
        this.config = config;
        this.visibilityController =
            visibilityController;

        layoutRenderer =
            new VirtualLayoutRenderer(
                config,
                renderer
            );

        packetSender =
            new VirtualTabPacketSender();

        skinResolver =
            new TabSkinResolver(
                plugin,
                config
            );
    }

    public void start() {

        stopTask();
        clearAll();
        skinResolver.clearCache();

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
                + " ticks, skins="
                + config.getSkinCount()
                + "."
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

        if (previous == null
                || !viewer.isOnline()) {

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
            skinPreviews.remove(viewerId);
        }
    }

    public void clearAll() {

        for (Player viewer
                : Bukkit.getOnlinePlayers()) {

            clear(viewer);
        }

        cache.clear();
        skinPreviews.clear();
    }

    public List<RenderedVirtualCell> previewCells(
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

    /**
     * Preview texte historique utilisé par /ktab preview.
     */
    public List<String> preview(
            Player viewer) {

        List<RenderedVirtualCell> cells =
            previewCells(
                viewer
            );

        List<String> result =
            new ArrayList<String>();

        for (RenderedVirtualCell cell : cells) {

            result.add(
                cell == null
                    ? ""
                    : cell.getText()
            );
        }

        return result;
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
            cache.get(
                viewerId
            );

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

    public ResolvedTabSkin resolveSkin(
            Player viewer,
            String skinId) {

        return skinResolver.resolve(
            viewer,
            skinId
        );
    }

    public void clearSkinResolverCache() {
        skinResolver.clearCache();
    }

    /**
     * Applique temporairement une skin sur la première cellule du layout
     * du viewer. Le layout lui-même n'est pas modifié.
     */
    public void previewSkin(
            Player viewer,
            String skinId,
            long durationTicks) {

        if (viewer == null
                || !viewer.isOnline()) {

            return;
        }

        long safeTicks =
            Math.max(
                20L,
                durationTicks
            );

        long expiresAt =
            System.currentTimeMillis()
                + safeTicks * 50L;

        skinPreviews.put(
            viewer.getUniqueId(),
            new SkinPreview(
                skinId == null
                    ? "none"
                    : skinId,
                expiresAt
            )
        );

        refresh(viewer);
    }

    public void clearSkinPreview(
            Player viewer) {

        if (viewer == null) {
            return;
        }

        skinPreviews.remove(
            viewer.getUniqueId()
        );

        if (viewer.isOnline()) {
            refresh(viewer);
        }
    }

    public String getSkinPreviewId(
            UUID viewerId) {

        SkinPreview preview =
            getActivePreview(
                viewerId
            );

        return preview == null
            ? ""
            : preview.skinId;
    }

    public int getSkinPreviewCount() {

        purgeExpiredSkinPreviews();
        return skinPreviews.size();
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

        visibilityController.apply(
            viewer
        );

        List<RenderedVirtualCell> rendered =
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

        SkinPreview skinPreview =
            getActivePreview(
                viewer.getUniqueId()
            );

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

            RenderedVirtualCell cell =
                index < rendered.size()
                    ? rendered.get(index)
                    : null;

            if (cell == null) {

                if (old != null) {

                    safeRemove(
                        viewer,
                        old
                    );

                    lastRemoves++;
                }

                continue;
            }

            String skinId =
                cell.getSkinId();

            if (index == 0
                    && skinPreview != null) {

                skinId =
                    skinPreview.skinId;
            }

            ResolvedTabSkin skin =
                skinResolver.resolve(
                    viewer,
                    skinId
                );

            if (old == null) {

                VirtualEntry entry =
                    createEntry(
                        viewer,
                        index,
                        cell.getText(),
                        skin
                    );

                safeAdd(
                    viewer,
                    entry
                );

                lastAdds++;

                next.add(
                    entry
                );

                continue;
            }

            boolean skinChanged =
                !skin.getCacheKey()
                    .equals(
                        old.getSkin()
                            .getCacheKey()
                    );

            if (skinChanged) {

                safeRemove(
                    viewer,
                    old
                );

                lastRemoves++;

                VirtualEntry replacement =
                    createEntry(
                        viewer,
                        index,
                        cell.getText(),
                        skin
                    );

                safeAdd(
                    viewer,
                    replacement
                );

                lastAdds++;

                next.add(
                    replacement
                );

                continue;
            }

            if (!cell.getText()
                    .equals(
                        old.getDisplayName()
                    )) {

                old.setDisplayName(
                    cell.getText()
                );

                safeUpdate(
                    viewer,
                    old
                );

                lastUpdates++;
            }

            next.add(
                old
            );
        }

        cache.put(
            viewer.getUniqueId(),
            next
        );
    }

    private VirtualEntry createEntry(
            Player viewer,
            int index,
            String text,
            ResolvedTabSkin skin) {

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

        String skinCacheKey =
            skin == null
                ? "none"
                : skin.getCacheKey();

        UUID uuid =
            UUID.nameUUIDFromBytes(
                (
                    config.getVirtualUuidSeed()
                        + ":"
                        + viewer.getUniqueId()
                        + ":"
                        + index
                        + ":"
                        + skinCacheKey
                ).getBytes(
                    StandardCharsets.UTF_8
                )
            );

        return new VirtualEntry(
            index,
            uuid,
            technicalName,
            text,
            skin
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

    private SkinPreview getActivePreview(
            UUID viewerId) {

        if (viewerId == null) {
            return null;
        }

        SkinPreview preview =
            skinPreviews.get(
                viewerId
            );

        if (preview == null) {
            return null;
        }

        if (preview.expiresAtMillis
                <= System.currentTimeMillis()) {

            skinPreviews.remove(
                viewerId
            );

            return null;
        }

        return preview;
    }

    private void purgeExpiredSkinPreviews() {

        List<UUID> expired =
            new ArrayList<UUID>();

        long now =
            System.currentTimeMillis();

        for (Map.Entry<UUID, SkinPreview> entry
                : skinPreviews.entrySet()) {

            if (entry.getValue() == null
                    || entry.getValue()
                        .expiresAtMillis <= now) {

                expired.add(
                    entry.getKey()
                );
            }
        }

        for (UUID viewerId : expired) {
            skinPreviews.remove(viewerId);
        }
    }

    private void stopTask() {

        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private static final class SkinPreview {

        private final String skinId;
        private final long expiresAtMillis;

        private SkinPreview(
                String skinId,
                long expiresAtMillis) {

            this.skinId =
                skinId == null
                    ? "none"
                    : skinId;

            this.expiresAtMillis =
                expiresAtMillis;
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
