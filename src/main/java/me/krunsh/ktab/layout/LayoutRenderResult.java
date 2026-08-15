package me.krunsh.ktab.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Snapshot complet d'un rendu :
 * cellules visibles + décisions conditionnelles.
 */
public final class LayoutRenderResult {

    private final List<RenderedVirtualCell> cells;
    private final List<LayoutDecision> decisions;

    public LayoutRenderResult(
            List<RenderedVirtualCell> cells,
            List<LayoutDecision> decisions) {

        this.cells =
            Collections.unmodifiableList(
                cells == null
                    ? new ArrayList<RenderedVirtualCell>()
                    : new ArrayList<RenderedVirtualCell>(
                        cells
                    )
            );

        this.decisions =
            Collections.unmodifiableList(
                decisions == null
                    ? new ArrayList<LayoutDecision>()
                    : new ArrayList<LayoutDecision>(
                        decisions
                    )
            );
    }

    public List<RenderedVirtualCell> getCells() {
        return cells;
    }

    public List<LayoutDecision> getDecisions() {
        return decisions;
    }
}
