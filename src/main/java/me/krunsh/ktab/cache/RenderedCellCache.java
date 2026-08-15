package me.krunsh.ktab.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import me.krunsh.ktab.dependency.DependencySet;

/**
 * Cache final d'une cellule après conditions + PlaceholderAPI + couleurs.
 *
 * Le cache est borné par viewer et ne contient que de petites chaînes.
 */
public final class RenderedCellCache {

    public static final class CachedCell {

        private final boolean visible;
        private final String renderedText;
        private final long expiresAtMillis;
        private final long globalRevision;

        private CachedCell(
                boolean visible,
                String renderedText,
                long expiresAtMillis,
                long globalRevision) {

            this.visible =
                visible;

            this.renderedText =
                renderedText == null
                    ? ""
                    : renderedText;

            this.expiresAtMillis =
                expiresAtMillis;

            this.globalRevision =
                globalRevision;
        }

        public boolean isVisible() {
            return visible;
        }

        public String getRenderedText() {
            return renderedText;
        }

        private boolean isValid(
                long now,
                long currentGlobalRevision,
                DependencySet dependencies) {

            if (expiresAtMillis <= now) {
                return false;
            }

            return dependencies == null
                || !dependencies.isGlobal()
                || globalRevision
                    == currentGlobalRevision;
        }
    }

    private final Map<UUID, LinkedHashMap<String, CachedCell>> viewers =
        new LinkedHashMap<UUID, LinkedHashMap<String, CachedCell>>();

    private int maxCellsPerPlayer =
        96;

    public void setMaxCellsPerPlayer(
            int maxCellsPerPlayer) {

        this.maxCellsPerPlayer =
            Math.max(
                16,
                maxCellsPerPlayer
            );
    }

    public CachedCell get(
            UUID viewerId,
            String key,
            long now,
            long globalRevision,
            DependencySet dependencies) {

        if (viewerId == null
                || key == null) {

            return null;
        }

        LinkedHashMap<String, CachedCell> cells =
            viewers.get(
                viewerId
            );

        if (cells == null) {
            return null;
        }

        CachedCell cached =
            cells.get(
                key
            );

        if (cached == null) {
            return null;
        }

        if (!cached.isValid(
                now,
                globalRevision,
                dependencies)) {

            cells.remove(
                key
            );

            if (cells.isEmpty()) {
                viewers.remove(
                    viewerId
                );
            }

            return null;
        }

        return cached;
    }

    public void put(
            UUID viewerId,
            String key,
            boolean visible,
            String renderedText,
            long ttlMillis,
            long globalRevision) {

        if (viewerId == null
                || key == null
                || ttlMillis <= 0L) {

            return;
        }

        LinkedHashMap<String, CachedCell> cells =
            viewers.get(
                viewerId
            );

        if (cells == null) {

            cells =
                new LinkedHashMap<String, CachedCell>(
                    32,
                    0.75F,
                    true
                );

            viewers.put(
                viewerId,
                cells
            );
        }

        cells.put(
            key,
            new CachedCell(
                visible,
                renderedText,
                System.currentTimeMillis()
                    + ttlMillis,
                globalRevision
            )
        );

        trim(
            cells
        );
    }

    public void invalidate(
            UUID viewerId) {

        if (viewerId != null) {
            viewers.remove(
                viewerId
            );
        }
    }

    public void clear() {
        viewers.clear();
    }

    public int getViewerCount() {
        return viewers.size();
    }

    public int getCellCount() {

        int total =
            0;

        for (Map<String, CachedCell> cells
                : viewers.values()) {

            total +=
                cells.size();
        }

        return total;
    }

    private void trim(
            LinkedHashMap<String, CachedCell> cells) {

        while (cells.size()
                > maxCellsPerPlayer) {

            Iterator<Map.Entry<String, CachedCell>> iterator =
                cells.entrySet()
                    .iterator();

            if (!iterator.hasNext()) {
                return;
            }

            iterator.next();
            iterator.remove();
        }
    }
}
