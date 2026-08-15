package me.krunsh.ktab.performance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.logging.KtabConsole;
import me.krunsh.ktab.render.PlaceholderRenderer;
import me.krunsh.ktab.service.TabService;
import me.krunsh.ktab.service.VirtualTabService;
import me.krunsh.ktab.visibility.TabVisibilityController;

/**
 * Scheduler central Ktab V9.1.
 *
 * Les services Header/Footer et VirtualTab ne possèdent plus leur propre
 * boucle globale. Tous les viewers passent par une roue répartie sur plusieurs
 * ticks + une DirtyQueue prioritaire.
 */
public final class KtabSchedulerService {

    private final KtabPlugin plugin;
    private final KtabConfig config;

    private final TabService tabService;
    private final VirtualTabService virtualTabService;
    private final TabVisibilityController visibilityController;
    private final PlaceholderRenderer placeholderRenderer;

    private final RefreshWheel refreshWheel =
        new RefreshWheel();

    private final DirtyQueue dirtyQueue =
        new DirtyQueue();

    private final PerformanceMetrics metrics =
        new PerformanceMetrics();

    private BukkitTask task;
    private long schedulerTick;

    public KtabSchedulerService(
            KtabPlugin plugin,
            KtabConfig config,
            TabService tabService,
            VirtualTabService virtualTabService,
            TabVisibilityController visibilityController,
            PlaceholderRenderer placeholderRenderer) {

        if (plugin == null
                || config == null
                || tabService == null
                || virtualTabService == null
                || visibilityController == null
                || placeholderRenderer == null) {

            throw new IllegalArgumentException(
                "Dépendance KtabSchedulerService manquante."
            );
        }

        this.plugin = plugin;
        this.config = config;
        this.tabService = tabService;
        this.virtualTabService = virtualTabService;
        this.visibilityController = visibilityController;
        this.placeholderRenderer = placeholderRenderer;
    }

    public void start() {

        stopTask();

        refreshWheel.rebuild(
            Bukkit.getOnlinePlayers()
        );

        dirtyQueue.clear();
        metrics.reset();
        schedulerTick = 0L;

        if (!config.isEnabled()) {
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
                    1L,
                    1L
                );

        KtabConsole.success(
            plugin,
            "Scheduler V9 actif - performance="
                + config.isPerformanceEnabled()
                + ", window="
                + config.getPerformanceRefreshWindowTicks()
                + "t, maxRegular="
                + config.getPerformanceMaxViewersPerTick()
                + ", maxDirty="
                + config.getPerformanceMaxDirtyPerTick()
                + "."
        );
    }

    public void restart() {
        start();
    }

    public void shutdown() {

        stopTask();

        refreshWheel.clear();
        dirtyQueue.clear();
    }

    public void register(
            Player player) {

        if (player == null
                || !player.isOnline()) {

            return;
        }

        refreshWheel.register(
            player
        );
    }

    public void unregister(
            UUID viewerId) {

        if (viewerId == null) {
            return;
        }

        refreshWheel.unregister(
            viewerId
        );

        dirtyQueue.remove(
            viewerId
        );

        tabService.remove(
            viewerId
        );

        virtualTabService.removeCache(
            viewerId
        );

        placeholderRenderer.invalidatePlayer(
            viewerId
        );
    }

    public void markDirty(
            Player player,
            DirtyReason reason) {

        if (player != null) {
            markDirty(
                player.getUniqueId(),
                reason
            );
        }
    }

    public void markDirty(
            UUID viewerId,
            DirtyReason reason) {

        if (viewerId == null) {
            return;
        }

        if (reason == DirtyReason.MANUAL
                || reason == DirtyReason.JOIN) {

            placeholderRenderer.invalidatePlayer(
                viewerId
            );

            virtualTabService.invalidateRenderCache(
                viewerId
            );
        }

        if (!config.isPerformanceEnabled()
                || !config.isPerformanceDirtyQueueEnabled()) {

            Player player =
                Bukkit.getPlayer(
                    viewerId
                );

            if (player != null
                    && player.isOnline()) {

                refreshNow(
                    player,
                    true
                );
            }

            return;
        }

        dirtyQueue.mark(
            viewerId,
            reason
        );
    }

    public void markAllDirty(
            DirtyReason reason) {

        if (reason == DirtyReason.CONFIG) {

            placeholderRenderer.clearCaches();
            virtualTabService.clearRenderCache();

        } else if (reason == DirtyReason.MANUAL) {

            for (UUID viewerId
                    : refreshWheel.snapshot()) {

                placeholderRenderer.invalidatePlayer(
                    viewerId
                );

                virtualTabService.invalidateRenderCache(
                    viewerId
                );
            }
        }

        if (!config.isPerformanceEnabled()
                || !config.isPerformanceDirtyQueueEnabled()) {

            for (Player player
                    : Bukkit.getOnlinePlayers()) {

                if (player != null
                        && player.isOnline()) {

                    refreshNow(
                        player,
                        true
                    );
                }
            }

            return;
        }

        for (UUID viewerId
                : refreshWheel.snapshot()) {

            dirtyQueue.mark(
                viewerId,
                reason
            );
        }
    }

    public void forceRefresh(
            Player viewer) {

        if (viewer == null
                || !viewer.isOnline()) {

            return;
        }

        refreshWheel.register(
            viewer
        );

        placeholderRenderer.invalidatePlayer(
            viewer.getUniqueId()
        );

        virtualTabService.invalidateRenderCache(
            viewer.getUniqueId()
        );

        refreshNow(
            viewer,
            true
        );
    }

    public PerformanceMetrics getMetrics() {
        return metrics;
    }

    public void resetMetrics() {

        metrics.reset();
        dirtyQueue.resetPeak();
        visibilityController.resetPerformanceMetrics();
        virtualTabService.resetPerformanceMetrics();
        tabService.resetPerformanceMetrics();
        placeholderRenderer.resetMetrics();
    }

    public int getWheelSize() {
        return refreshWheel.size();
    }

    public int getWheelCursor() {
        return refreshWheel.getCursor();
    }

    public int getDirtyQueueSize() {
        return dirtyQueue.size();
    }

    public int getDirtyQueuePeak() {
        return dirtyQueue.getPeakSize();
    }

    public int getRecommendedRegularPerTick() {

        return RefreshWheel.recommendedPerTick(
            refreshWheel.size(),
            config.getPerformanceRefreshWindowTicks(),
            config.getPerformanceMaxViewersPerTick()
        );
    }

    public int getEstimatedCycleTicks() {

        int viewers =
            refreshWheel.size();

        int perTick =
            getRecommendedRegularPerTick();

        if (viewers <= 0
                || perTick <= 0) {

            return 0;
        }

        return (viewers + perTick - 1)
            / perTick;
    }

    private void tick() {

        long started =
            System.nanoTime();

        schedulerTick++;

        placeholderRenderer.beginTick(
            Bukkit.getOnlinePlayers()
                .size(),
            Bukkit.getMaxPlayers()
        );

        if (!config.isPerformanceEnabled()) {

            legacyTick(
                started
            );

            return;
        }

        int dirtyProcessed =
            0;

        int regularProcessed =
            0;

        Set<UUID> processed =
            new HashSet<UUID>();

        if (config.isPerformanceDirtyQueueEnabled()) {

            List<DirtyWork> dirty =
                dirtyQueue.poll(
                    config.getPerformanceMaxDirtyPerTick()
                );

            for (DirtyWork work : dirty) {

                UUID viewerId =
                    work.getViewerId();

                Player viewer =
                    Bukkit.getPlayer(
                        viewerId
                    );

                if (viewer == null
                        || !viewer.isOnline()) {

                    unregister(
                        viewerId
                    );

                    continue;
                }

                refreshNow(
                    viewer,
                    true
                );

                processed.add(
                    viewerId
                );

                dirtyProcessed++;
            }
        }

        int regularBudget =
            getRecommendedRegularPerTick();

        /*
         * On demande quelques candidats supplémentaires à la roue afin de
         * compenser ceux déjà traités par la DirtyQueue pendant ce tick.
         */
        int pollCount =
            Math.min(
                refreshWheel.size(),
                regularBudget
                    + processed.size()
            );

        List<UUID> regular =
            refreshWheel.poll(
                pollCount
            );

        for (UUID viewerId : regular) {

            if (regularProcessed >= regularBudget) {
                break;
            }

            if (processed.contains(
                    viewerId)) {

                continue;
            }

            Player viewer =
                Bukkit.getPlayer(
                    viewerId
                );

            if (viewer == null
                    || !viewer.isOnline()) {

                unregister(
                    viewerId
                );

                continue;
            }

            refreshNow(
                viewer,
                false
            );

            regularProcessed++;
        }

        runVisibilityFallbacks();

        metrics.recordTick(
            System.nanoTime() - started,
            dirtyProcessed,
            regularProcessed,
            dirtyQueue.size(),
            dirtyQueue.getPeakSize()
        );
    }

    private void legacyTick(
            long started) {

        long interval =
            Math.max(
                1L,
                Math.min(
                    config.getUpdateIntervalTicks(),
                    config.getVirtualUpdateIntervalTicks()
                )
            );

        int refreshed =
            0;

        if (schedulerTick % interval == 0L) {

            for (Player viewer
                    : Bukkit.getOnlinePlayers()) {

                if (viewer == null
                        || !viewer.isOnline()) {

                    continue;
                }

                refreshNow(
                    viewer,
                    false
                );

                refreshed++;
            }
        }

        runVisibilityFallbacks();

        metrics.recordTick(
            System.nanoTime() - started,
            0,
            refreshed,
            0,
            0
        );
    }

    private void refreshNow(
            Player viewer,
            boolean dirty) {

        long started =
            System.nanoTime();

        tabService.refresh(
            viewer
        );

        virtualTabService.refresh(
            viewer
        );

        metrics.recordViewer(
            System.nanoTime() - started,
            dirty
        );
    }

    private void runVisibilityFallbacks() {

        long fullScanTicks =
            config.getPerformanceVisibilityFallbackScanTicks();

        boolean fullSweepRan =
            false;

        if (fullScanTicks > 0L
                && schedulerTick % fullScanTicks == 0L) {

            visibilityController.applyAllBatched();

            fullSweepRan =
                true;
        }

        long npcScanTicks =
            config.getPerformanceServerNpcScanTicks();

        if (!fullSweepRan
                && npcScanTicks > 0L
                && schedulerTick % npcScanTicks == 0L) {

            long forceTicks =
                config.getPerformanceServerNpcForceRehideTicks();

            boolean force =
                forceTicks > 0L
                    && schedulerTick % forceTicks == 0L;

            visibilityController.auditServerNpcsDelta(
                force
            );
        }
    }

    private void stopTask() {

        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
