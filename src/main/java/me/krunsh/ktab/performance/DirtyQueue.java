package me.krunsh.ktab.performance;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * File FIFO dédupliquée par UUID.
 *
 * Plusieurs invalidations du même joueur fusionnent leurs DirtyReason au lieu
 * de créer plusieurs travaux.
 */
public final class DirtyQueue {

    private final LinkedHashMap<UUID, EnumSet<DirtyReason>> queued =
        new LinkedHashMap<UUID, EnumSet<DirtyReason>>();

    private int peakSize;

    public void mark(
            UUID viewerId,
            DirtyReason reason) {

        if (viewerId == null) {
            return;
        }

        DirtyReason safeReason =
            reason == null
                ? DirtyReason.FALLBACK
                : reason;

        EnumSet<DirtyReason> reasons =
            queued.get(
                viewerId
            );

        if (reasons == null) {

            reasons =
                EnumSet.noneOf(
                    DirtyReason.class
                );

            queued.put(
                viewerId,
                reasons
            );
        }

        reasons.add(
            safeReason
        );

        peakSize =
            Math.max(
                peakSize,
                queued.size()
            );
    }

    public void remove(
            UUID viewerId) {

        if (viewerId != null) {
            queued.remove(viewerId);
        }
    }

    public List<DirtyWork> poll(
            int maximum) {

        int limit =
            Math.max(
                0,
                maximum
            );

        if (limit == 0
                || queued.isEmpty()) {

            return new ArrayList<DirtyWork>();
        }

        List<DirtyWork> result =
            new ArrayList<DirtyWork>(
                Math.min(
                    limit,
                    queued.size()
                )
            );

        Iterator<Map.Entry<UUID, EnumSet<DirtyReason>>> iterator =
            queued.entrySet()
                .iterator();

        while (iterator.hasNext()
                && result.size() < limit) {

            Map.Entry<UUID, EnumSet<DirtyReason>> entry =
                iterator.next();

            result.add(
                new DirtyWork(
                    entry.getKey(),
                    entry.getValue()
                )
            );

            iterator.remove();
        }

        return result;
    }

    public int size() {
        return queued.size();
    }

    public int getPeakSize() {
        return peakSize;
    }

    public void resetPeak() {
        peakSize = queued.size();
    }

    public void clear() {
        queued.clear();
        peakSize = 0;
    }
}
