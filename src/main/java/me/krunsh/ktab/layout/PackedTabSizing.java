package me.krunsh.ktab.layout;

/**
 * Calcul pur du nombre maximum d'entrées virtuelles.
 */
public final class PackedTabSizing {

    private PackedTabSizing() {
    }

    public static int fakeEntryLimit(
            boolean forceClientRows,
            int columns,
            int rows,
            int configuredMaximum,
            int configuredRealReserve,
            int onlinePlayers,
            boolean hideRealPlayers) {

        int safeColumns =
            Math.max(
                1,
                columns
            );

        int safeRows =
            Math.max(
                1,
                Math.min(
                    20,
                    rows
                )
            );

        int safeMaximum =
            Math.max(
                1,
                Math.min(
                    80,
                    configuredMaximum
                )
            );

        if (!forceClientRows) {
            return safeMaximum;
        }

        int totalCells =
            safeColumns * safeRows;

        int visibleRealPlayers =
            hideRealPlayers
                ? 0
                : Math.max(
                    Math.max(
                        0,
                        configuredRealReserve
                    ),
                    Math.max(
                        0,
                        onlinePlayers
                    )
                );

        return Math.max(
            0,
            Math.min(
                safeMaximum,
                totalCells
                    - visibleRealPlayers
            )
        );
    }
}
