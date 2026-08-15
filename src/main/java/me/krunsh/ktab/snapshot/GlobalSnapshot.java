package me.krunsh.ktab.snapshot;

/**
 * Valeurs globales capturées une seule fois par tick du scheduler.
 */
public final class GlobalSnapshot {

    private final int onlinePlayers;
    private final int maxPlayers;
    private final long revision;

    public GlobalSnapshot(
            int onlinePlayers,
            int maxPlayers,
            long revision) {

        this.onlinePlayers =
            Math.max(
                0,
                onlinePlayers
            );

        this.maxPlayers =
            Math.max(
                0,
                maxPlayers
            );

        this.revision =
            Math.max(
                0L,
                revision
            );
    }

    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public long getRevision() {
        return revision;
    }
}
