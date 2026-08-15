package me.krunsh.ktab.performance;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Travail dédupliqué extrait de DirtyQueue.
 */
public final class DirtyWork {

    private final UUID viewerId;
    private final EnumSet<DirtyReason> reasons;

    public DirtyWork(
            UUID viewerId,
            EnumSet<DirtyReason> reasons) {

        if (viewerId == null) {
            throw new IllegalArgumentException(
                "viewerId manquant."
            );
        }

        this.viewerId =
            viewerId;

        this.reasons =
            reasons == null
                || reasons.isEmpty()
                ? EnumSet.of(
                    DirtyReason.FALLBACK
                )
                : EnumSet.copyOf(
                    reasons
                );
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public Set<DirtyReason> getReasons() {
        return Collections.unmodifiableSet(
            reasons
        );
    }

    public boolean hasReason(
            DirtyReason reason) {

        return reason != null
            && reasons.contains(
                reason
            );
    }
}
