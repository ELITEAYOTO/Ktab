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

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.layout.LayoutRenderResult;
import me.krunsh.ktab.layout.RenderedVirtualCell;
import me.krunsh.ktab.layout.VirtualLayoutRenderer;
import me.krunsh.ktab.logging.KtabConsole;
import me.krunsh.ktab.performance.SelectiveRenderMetrics;
import me.krunsh.ktab.packet.PacketBatchResult;
import me.krunsh.ktab.packet.VirtualEntry;
import me.krunsh.ktab.packet.VirtualTabPacketSender;
import me.krunsh.ktab.render.PlaceholderRenderer;
import me.krunsh.ktab.skin.ResolvedTabSkin;
import me.krunsh.ktab.skin.TabSkinResolver;

/**
 * Service des entrées virtuelles du TAB.
 *
 * V9.3 :
 * - scheduler central V9 conservé ;
 * - diff calculé avant tout envoi ;
 * - ADD / UPDATE / REMOVE regroupés en batches configurables ;
 * - le cache reflète uniquement les opérations dont l'envoi a réussi ;
 * - un échec packet reste donc naturellement dirty au prochain refresh.
 */
public final class VirtualTabService {

    private final KtabPlugin plugin;
    private final KtabConfig config;

    private final VirtualLayoutRenderer layoutRenderer;
    private final VirtualTabPacketSender packetSender;
    private final TabSkinResolver skinResolver;

    /**
     * Les listes peuvent contenir des null : cela permet de représenter une
     * cellule dont l'ADD a échoué sans décaler tous les index suivants.
     */
    private final Map<UUID, List<VirtualEntry>> cache =
        new HashMap<UUID, List<VirtualEntry>>();

    private final Map<UUID, SkinPreview> skinPreviews =
        new HashMap<UUID, SkinPreview>();

    private long lastCycleMillis;
    private int lastAdds;
    private int lastUpdates;
    private int lastRemoves;

    /** Opérations logiques sur des entrées. */
    private long totalAdds;
    private long totalUpdates;
    private long totalRemoves;
    private long totalRefreshes;

    /** Packets réseau réellement remis à sendPacket. */
    private long totalAddPackets;
    private long totalUpdatePackets;
    private long totalRemovePackets;

    private long totalPacketFailures;
    private long totalRetryEntries;

    private long lastPacketFailureLogMillis;

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

        skinResolver =
            new TabSkinResolver(
                plugin,
                config
            );
    }

    public void start() {

        clearAll();
        skinResolver.clearCache();
        layoutRenderer.clearCaches();

        if (!config.isEnabled()
                || !config.isVirtualLayoutEnabled()) {

            return;
        }

        KtabConsole.success(
            plugin,
            "VirtualTabService prêt - NMS="
                + packetSender.getNmsVersion()
                + ", batching="
                + config.isPerformancePacketBatchingEnabled()
                + ", renderCache="
                + config.isPerformanceRenderCacheEnabled()
                + ", skins="
                + config.getSkinCount()
                + "."
        );
    }

    public void restart() {
        start();
    }

    public void shutdown() {
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

        long started =
            System.nanoTime();

        resetLastOperations();

        updateViewer(
            viewer,
            Bukkit.getOnlinePlayers()
                .size()
        );

        totalRefreshes++;

        lastCycleMillis =
            Math.max(
                0L,
                (System.nanoTime() - started)
                    / 1000000L
            );
    }

    public void refreshAll() {

        if (!config.isEnabled()
                || !config.isVirtualLayoutEnabled()) {

            return;
        }

        long started =
            System.nanoTime();

        resetLastOperations();

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

                totalRefreshes++;
            }
        }

        lastCycleMillis =
            Math.max(
                0L,
                (System.nanoTime() - started)
                    / 1000000L
            );
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

        List<VirtualEntry> entries =
            nonNullEntries(
                previous
            );

        if (entries.isEmpty()) {
            return;
        }

        PacketBatchResult result =
            sendRemoveBatch(
                viewer,
                entries
            );

        recordRemoveBatch(
            result
        );

        recordFailures(
            result,
            "clear"
        );
    }

    public void removeCache(
            UUID viewerId) {

        if (viewerId != null) {
            cache.remove(viewerId);
            skinPreviews.remove(viewerId);
            layoutRenderer.invalidateViewer(
                viewerId
            );
        }
    }

    public void clearAll() {

        for (Player viewer
                : Bukkit.getOnlinePlayers()) {

            clear(viewer);
        }

        cache.clear();
        skinPreviews.clear();
        layoutRenderer.clearCaches();
    }

    public LayoutRenderResult previewDetailed(
            Player viewer) {

        if (viewer == null) {

            return new LayoutRenderResult(
                Collections.<RenderedVirtualCell>emptyList(),
                Collections.<me.krunsh.ktab.layout.LayoutDecision>emptyList()
            );
        }

        return layoutRenderer.renderDetailed(
            viewer,
            Bukkit.getOnlinePlayers()
                .size()
        );
    }

    public List<RenderedVirtualCell> previewCells(
            Player viewer) {

        return previewDetailed(
            viewer
        ).getCells();
    }

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

        if (entries == null) {
            return 0;
        }

        int count =
            0;

        for (VirtualEntry entry : entries) {

            if (entry != null) {
                count++;
            }
        }

        return count;
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

    public long getTotalAdds() {
        return totalAdds;
    }

    public long getTotalUpdates() {
        return totalUpdates;
    }

    public long getTotalRemoves() {
        return totalRemoves;
    }

    public long getTotalRefreshes() {
        return totalRefreshes;
    }

    public long getTotalAddPackets() {
        return totalAddPackets;
    }

    public long getTotalUpdatePackets() {
        return totalUpdatePackets;
    }

    public long getTotalRemovePackets() {
        return totalRemovePackets;
    }

    public long getTotalNetworkPackets() {

        return totalAddPackets
            + totalUpdatePackets
            + totalRemovePackets;
    }

    public long getTotalPacketFailures() {
        return totalPacketFailures;
    }

    public long getTotalRetryEntries() {
        return totalRetryEntries;
    }

    public double getPacketCompressionRatio() {

        long operations =
            totalAdds
                + totalUpdates
                + totalRemoves;

        long packets =
            getTotalNetworkPackets();

        if (operations <= 0L
                || packets <= 0L) {

            return 0.0D;
        }

        return operations
            / (double) packets;
    }

    public void invalidateRenderCache(
            UUID viewerId) {

        layoutRenderer.invalidateViewer(
            viewerId
        );
    }

    public void clearRenderCache() {
        layoutRenderer.clearCaches();
    }

    public int getRenderedCellCacheViewerCount() {
        return layoutRenderer
            .getCachedViewerCount();
    }

    public int getRenderedCellCacheCellCount() {
        return layoutRenderer
            .getCachedCellCount();
    }

    public SelectiveRenderMetrics getSelectiveRenderMetrics() {
        return layoutRenderer
            .getMetrics();
    }

    public void resetPerformanceMetrics() {

        totalAdds = 0L;
        totalUpdates = 0L;
        totalRemoves = 0L;
        totalRefreshes = 0L;

        totalAddPackets = 0L;
        totalUpdatePackets = 0L;
        totalRemovePackets = 0L;
        totalPacketFailures = 0L;
        totalRetryEntries = 0L;

        lastPacketFailureLogMillis = 0L;

        layoutRenderer.resetMetrics();

        resetLastOperations();
        lastCycleMillis = 0L;
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

    private void updateViewer(
            Player viewer,
            int onlinePlayers) {

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

        SkinPreview skinPreview =
            getActivePreview(
                viewer.getUniqueId()
            );

        List<VirtualEntry> desired =
            buildDesiredEntries(
                viewer,
                rendered,
                skinPreview
            );

        int max =
            Math.max(
                previous.size(),
                desired.size()
            );

        OperationKind[] operations =
            new OperationKind[max];

        List<VirtualEntry> removeEntries =
            new ArrayList<VirtualEntry>();

        List<VirtualEntry> pureAddEntries =
            new ArrayList<VirtualEntry>();

        List<VirtualEntry> updateEntries =
            new ArrayList<VirtualEntry>();

        for (int index = 0;
                index < max;
                index++) {

            VirtualEntry old =
                getEntry(
                    previous,
                    index
                );

            VirtualEntry wanted =
                getEntry(
                    desired,
                    index
                );

            if (old == null
                    && wanted == null) {

                operations[index] =
                    OperationKind.NONE;

                continue;
            }

            if (old == null) {

                operations[index] =
                    OperationKind.ADD;

                pureAddEntries.add(
                    wanted
                );

                continue;
            }

            if (wanted == null) {

                operations[index] =
                    OperationKind.REMOVE;

                removeEntries.add(
                    old
                );

                continue;
            }

            boolean skinChanged =
                !wanted.getSkin()
                    .getCacheKey()
                    .equals(
                        old.getSkin()
                            .getCacheKey()
                    );

            if (skinChanged) {

                operations[index] =
                    OperationKind.REPLACE;

                removeEntries.add(
                    old
                );

                continue;
            }

            if (!wanted.getDisplayName()
                    .equals(
                        old.getDisplayName()
                    )) {

                operations[index] =
                    OperationKind.UPDATE;

                updateEntries.add(
                    wanted
                );

                continue;
            }

            operations[index] =
                OperationKind.NONE;
        }

        int replacementCount =
            countOperations(
                operations,
                OperationKind.REPLACE
            );

        int logicalAdds =
            pureAddEntries.size()
                + replacementCount;

        int logicalRemoves =
            removeEntries.size();

        int logicalUpdates =
            updateEntries.size();

        lastAdds += logicalAdds;
        lastRemoves += logicalRemoves;
        lastUpdates += logicalUpdates;

        totalAdds += logicalAdds;
        totalRemoves += logicalRemoves;
        totalUpdates += logicalUpdates;

        PacketBatchResult removeResult =
            sendRemoveBatch(
                viewer,
                removeEntries
            );

        recordRemoveBatch(
            removeResult
        );

        PacketBatchResult updateResult =
            sendUpdateBatch(
                viewer,
                updateEntries
            );

        recordUpdateBatch(
            updateResult
        );

        PacketBatchResult pureAddResult =
            sendAddBatch(
                viewer,
                pureAddEntries
            );

        recordAddBatch(
            pureAddResult
        );

        List<VirtualEntry> replacementAdds =
            new ArrayList<VirtualEntry>();

        for (int index = 0;
                index < max;
                index++) {

            if (operations[index]
                    != OperationKind.REPLACE) {

                continue;
            }

            VirtualEntry old =
                getEntry(
                    previous,
                    index
                );

            if (removeResult.wasSuccessful(
                    old)) {

                VirtualEntry wanted =
                    getEntry(
                        desired,
                        index
                    );

                if (wanted != null) {
                    replacementAdds.add(wanted);
                }
            }
        }

        PacketBatchResult replacementAddResult =
            sendAddBatch(
                viewer,
                replacementAdds
            );

        recordAddBatch(
            replacementAddResult
        );

        recordFailures(
            removeResult,
            "REMOVE"
        );

        recordFailures(
            updateResult,
            "UPDATE"
        );

        recordFailures(
            pureAddResult,
            "ADD"
        );

        recordFailures(
            replacementAddResult,
            "REPLACE_ADD"
        );

        List<VirtualEntry> next =
            new ArrayList<VirtualEntry>(
                max
            );

        for (int index = 0;
                index < max;
                index++) {

            VirtualEntry old =
                getEntry(
                    previous,
                    index
                );

            VirtualEntry wanted =
                getEntry(
                    desired,
                    index
                );

            OperationKind operation =
                operations[index];

            VirtualEntry synchronizedEntry =
                resolveSynchronizedEntry(
                    operation,
                    old,
                    wanted,
                    removeResult,
                    updateResult,
                    pureAddResult,
                    replacementAddResult
                );

            next.add(
                synchronizedEntry
            );
        }

        totalRetryEntries +=
            countRetryEntries(
                operations,
                previous,
                desired,
                removeResult,
                updateResult,
                pureAddResult,
                replacementAddResult
            );

        trimTrailingNulls(
            next
        );

        cache.put(
            viewer.getUniqueId(),
            next
        );
    }

    private List<VirtualEntry> buildDesiredEntries(
            Player viewer,
            List<RenderedVirtualCell> rendered,
            SkinPreview skinPreview) {

        List<VirtualEntry> desired =
            new ArrayList<VirtualEntry>(
                rendered.size()
            );

        for (int index = 0;
                index < rendered.size();
                index++) {

            RenderedVirtualCell cell =
                rendered.get(index);

            if (cell == null) {

                desired.add(null);
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

            desired.add(
                createEntry(
                    viewer,
                    index,
                    cell.getText(),
                    skin
                )
            );
        }

        return desired;
    }

    private VirtualEntry resolveSynchronizedEntry(
            OperationKind operation,
            VirtualEntry old,
            VirtualEntry wanted,
            PacketBatchResult removeResult,
            PacketBatchResult updateResult,
            PacketBatchResult pureAddResult,
            PacketBatchResult replacementAddResult) {

        if (operation == null
                || operation == OperationKind.NONE) {

            return old != null
                ? old
                : wanted;
        }

        if (operation == OperationKind.ADD) {

            return pureAddResult.wasSuccessful(
                    wanted)
                ? wanted
                : null;
        }

        if (operation == OperationKind.UPDATE) {

            return updateResult.wasSuccessful(
                    wanted)
                ? wanted
                : old;
        }

        if (operation == OperationKind.REMOVE) {

            return removeResult.wasSuccessful(
                    old)
                ? null
                : old;
        }

        if (operation == OperationKind.REPLACE) {

            if (!removeResult.wasSuccessful(
                    old)) {

                return old;
            }

            return replacementAddResult.wasSuccessful(
                    wanted)
                ? wanted
                : null;
        }

        return old;
    }

    private long countRetryEntries(
            OperationKind[] operations,
            List<VirtualEntry> previous,
            List<VirtualEntry> desired,
            PacketBatchResult removeResult,
            PacketBatchResult updateResult,
            PacketBatchResult pureAddResult,
            PacketBatchResult replacementAddResult) {

        long retries =
            0L;

        for (int index = 0;
                index < operations.length;
                index++) {

            OperationKind operation =
                operations[index];

            VirtualEntry old =
                getEntry(
                    previous,
                    index
                );

            VirtualEntry wanted =
                getEntry(
                    desired,
                    index
                );

            if (operation == OperationKind.ADD
                    && !pureAddResult.wasSuccessful(
                        wanted)) {

                retries++;

            } else if (operation == OperationKind.UPDATE
                    && !updateResult.wasSuccessful(
                        wanted)) {

                retries++;

            } else if (operation == OperationKind.REMOVE
                    && !removeResult.wasSuccessful(
                        old)) {

                retries++;

            } else if (operation == OperationKind.REPLACE) {

                if (!removeResult.wasSuccessful(
                        old)
                        || !replacementAddResult.wasSuccessful(
                            wanted)) {

                    retries++;
                }
            }
        }

        return retries;
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

    private PacketBatchResult sendAddBatch(
            Player viewer,
            List<VirtualEntry> entries) {

        return packetSender.addBatch(
            viewer,
            entries,
            batchSize(
                config.isPerformancePacketBatchAdd()
            )
        );
    }

    private PacketBatchResult sendUpdateBatch(
            Player viewer,
            List<VirtualEntry> entries) {

        return packetSender.updateBatch(
            viewer,
            entries,
            batchSize(
                config.isPerformancePacketBatchUpdate()
            )
        );
    }

    private PacketBatchResult sendRemoveBatch(
            Player viewer,
            List<VirtualEntry> entries) {

        return packetSender.removeBatch(
            viewer,
            entries,
            batchSize(
                config.isPerformancePacketBatchRemove()
            )
        );
    }

    private int batchSize(
            boolean actionEnabled) {

        if (!config.isPerformancePacketBatchingEnabled()
                || !actionEnabled) {

            return 1;
        }

        return config
            .getPerformancePacketMaxEntriesPerPacket();
    }

    private void recordAddBatch(
            PacketBatchResult result) {

        if (result != null) {
            totalAddPackets += result.getPacketsSent();
        }
    }

    private void recordUpdateBatch(
            PacketBatchResult result) {

        if (result != null) {
            totalUpdatePackets += result.getPacketsSent();
        }
    }

    private void recordRemoveBatch(
            PacketBatchResult result) {

        if (result != null) {
            totalRemovePackets += result.getPacketsSent();
        }
    }

    private void recordFailures(
            PacketBatchResult result,
            String action) {

        if (result == null
                || result.getPacketsFailed() <= 0) {

            return;
        }

        totalPacketFailures +=
            result.getPacketsFailed();

        if (!shouldLogPacketFailure()) {
            return;
        }

        plugin.getLogger().warning(
            "V9.3 PlayerInfo batch "
                + action
                + " : "
                + result.getPacketsFailed()
                + " packet(s) en échec, "
                + result.getSuccessfulEntries()
                + "/"
                + result.getAttemptedEntries()
                + " entrée(s) synchronisée(s)."
                + (result.getLastError().isEmpty()
                    ? ""
                    : " Dernière erreur: "
                        + result.getLastError())
        );
    }

    private boolean shouldLogPacketFailure() {

        long now =
            System.currentTimeMillis();

        long intervalMillis =
            Math.max(
                0L,
                config.getPerformancePacketFailureLogIntervalTicks()
            ) * 50L;

        if (intervalMillis <= 0L
                || now - lastPacketFailureLogMillis
                    >= intervalMillis) {

            lastPacketFailureLogMillis =
                now;

            return true;
        }

        return false;
    }

    private void resetLastOperations() {
        lastAdds = 0;
        lastUpdates = 0;
        lastRemoves = 0;
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

    private static VirtualEntry getEntry(
            List<VirtualEntry> entries,
            int index) {

        if (entries == null
                || index < 0
                || index >= entries.size()) {

            return null;
        }

        return entries.get(index);
    }

    private static List<VirtualEntry> nonNullEntries(
            List<VirtualEntry> entries) {

        List<VirtualEntry> result =
            new ArrayList<VirtualEntry>();

        if (entries == null) {
            return result;
        }

        for (VirtualEntry entry : entries) {

            if (entry != null) {
                result.add(entry);
            }
        }

        return result;
    }

    private static int countOperations(
            OperationKind[] operations,
            OperationKind expected) {

        int count =
            0;

        for (OperationKind operation : operations) {

            if (operation == expected) {
                count++;
            }
        }

        return count;
    }

    private static void trimTrailingNulls(
            List<VirtualEntry> entries) {

        for (int index =
                entries.size() - 1;
                index >= 0;
                index--) {

            if (entries.get(index) != null) {
                return;
            }

            entries.remove(index);
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

    private enum OperationKind {
        NONE,
        ADD,
        UPDATE,
        REMOVE,
        REPLACE
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
