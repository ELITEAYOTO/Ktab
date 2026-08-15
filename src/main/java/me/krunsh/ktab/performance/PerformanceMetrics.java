package me.krunsh.ktab.performance;

/**
 * Métriques du scheduler V9.
 *
 * Toutes les écritures ont lieu sur le thread serveur.
 */
public final class PerformanceMetrics {

    private long schedulerTicks;
    private long totalTickNanos;
    private long maxTickNanos;
    private long lastTickNanos;

    private long totalViewerRefreshes;
    private long totalDirtyRefreshes;
    private long totalRegularRefreshes;

    private long totalViewerNanos;
    private long maxViewerNanos;

    private int lastDirtyViewers;
    private int lastRegularViewers;
    private int lastQueueSize;
    private int peakQueueSize;

    public void recordViewer(
            long nanos,
            boolean dirty) {

        long safeNanos =
            Math.max(
                0L,
                nanos
            );

        totalViewerRefreshes++;

        if (dirty) {
            totalDirtyRefreshes++;
        } else {
            totalRegularRefreshes++;
        }

        totalViewerNanos +=
            safeNanos;

        maxViewerNanos =
            Math.max(
                maxViewerNanos,
                safeNanos
            );
    }

    public void recordTick(
            long nanos,
            int dirtyViewers,
            int regularViewers,
            int queueSize,
            int queuePeak) {

        long safeNanos =
            Math.max(
                0L,
                nanos
            );

        schedulerTicks++;
        totalTickNanos += safeNanos;
        lastTickNanos = safeNanos;

        maxTickNanos =
            Math.max(
                maxTickNanos,
                safeNanos
            );

        lastDirtyViewers =
            Math.max(
                0,
                dirtyViewers
            );

        lastRegularViewers =
            Math.max(
                0,
                regularViewers
            );

        lastQueueSize =
            Math.max(
                0,
                queueSize
            );

        peakQueueSize =
            Math.max(
                peakQueueSize,
                Math.max(
                    lastQueueSize,
                    queuePeak
                )
            );
    }

    public long getSchedulerTicks() {
        return schedulerTicks;
    }

    public long getTotalViewerRefreshes() {
        return totalViewerRefreshes;
    }

    public long getTotalDirtyRefreshes() {
        return totalDirtyRefreshes;
    }

    public long getTotalRegularRefreshes() {
        return totalRegularRefreshes;
    }

    public int getLastDirtyViewers() {
        return lastDirtyViewers;
    }

    public int getLastRegularViewers() {
        return lastRegularViewers;
    }

    public int getLastQueueSize() {
        return lastQueueSize;
    }

    public int getPeakQueueSize() {
        return peakQueueSize;
    }

    public double getLastTickMillis() {
        return nanosToMillis(
            lastTickNanos
        );
    }

    public double getAverageTickMillis() {

        if (schedulerTicks <= 0L) {
            return 0.0D;
        }

        return nanosToMillis(
            totalTickNanos
                / schedulerTicks
        );
    }

    public double getMaxTickMillis() {
        return nanosToMillis(
            maxTickNanos
        );
    }

    public double getAverageViewerMillis() {

        if (totalViewerRefreshes <= 0L) {
            return 0.0D;
        }

        return nanosToMillis(
            totalViewerNanos
                / totalViewerRefreshes
        );
    }

    public double getMaxViewerMillis() {
        return nanosToMillis(
            maxViewerNanos
        );
    }

    public void reset() {

        schedulerTicks = 0L;
        totalTickNanos = 0L;
        maxTickNanos = 0L;
        lastTickNanos = 0L;

        totalViewerRefreshes = 0L;
        totalDirtyRefreshes = 0L;
        totalRegularRefreshes = 0L;

        totalViewerNanos = 0L;
        maxViewerNanos = 0L;

        lastDirtyViewers = 0;
        lastRegularViewers = 0;
        lastQueueSize = 0;
        peakQueueSize = 0;
    }

    private static double nanosToMillis(
            long nanos) {

        return nanos
            / 1000000.0D;
    }
}
