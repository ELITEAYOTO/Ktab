package me.krunsh.ktab.performance;

/**
 * Compteurs de rendu sélectif V9.4.
 */
public final class SelectiveRenderMetrics {

    private long cellsChecked;
    private long cellsRendered;
    private long cellsSkipped;

    private long conditionsEvaluated;
    private long conditionsSkipped;

    public void recordCell(
            boolean cacheHit) {

        cellsChecked++;

        if (cacheHit) {
            cellsSkipped++;
        } else {
            cellsRendered++;
        }
    }

    public void recordCondition(
            boolean cacheHit) {

        if (cacheHit) {
            conditionsSkipped++;
        } else {
            conditionsEvaluated++;
        }
    }

    public long getCellsChecked() {
        return cellsChecked;
    }

    public long getCellsRendered() {
        return cellsRendered;
    }

    public long getCellsSkipped() {
        return cellsSkipped;
    }

    public long getConditionsEvaluated() {
        return conditionsEvaluated;
    }

    public long getConditionsSkipped() {
        return conditionsSkipped;
    }

    public double getSkipRate() {

        if (cellsChecked <= 0L) {
            return 0.0D;
        }

        return cellsSkipped
            * 100.0D
            / cellsChecked;
    }

    public void reset() {

        cellsChecked = 0L;
        cellsRendered = 0L;
        cellsSkipped = 0L;

        conditionsEvaluated = 0L;
        conditionsSkipped = 0L;
    }
}
