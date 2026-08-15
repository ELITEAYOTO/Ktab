package me.krunsh.ktab.snapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cache des snapshots viewers.
 */
public final class PlayerSnapshotCache {

    private final Map<UUID, PlayerSnapshot> snapshots =
        new HashMap<UUID, PlayerSnapshot>();

    public PlayerSnapshot getOrCreate(
            UUID playerId) {

        PlayerSnapshot snapshot =
            snapshots.get(
                playerId
            );

        if (snapshot == null) {

            snapshot =
                new PlayerSnapshot(
                    playerId
                );

            snapshots.put(
                playerId,
                snapshot
            );
        }

        return snapshot;
    }

    public void invalidate(
            UUID playerId) {

        if (playerId != null) {
            snapshots.remove(playerId);
        }
    }

    public void clear() {
        snapshots.clear();
    }

    public int size() {
        return snapshots.size();
    }

    public int cachedValueCount() {

        int count =
            0;

        for (PlayerSnapshot snapshot
                : snapshots.values()) {

            if (snapshot != null) {
                count += snapshot.size();
            }
        }

        return count;
    }
}
