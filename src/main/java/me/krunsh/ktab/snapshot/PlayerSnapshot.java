package me.krunsh.ktab.snapshot;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cache borné des valeurs d'un viewer.
 */
public final class PlayerSnapshot {

    public static final class CachedValue {

        private final String value;
        private final long expiresAtMillis;

        private CachedValue(
                String value,
                long expiresAtMillis) {

            this.value =
                value == null
                    ? ""
                    : value;

            this.expiresAtMillis =
                expiresAtMillis;
        }

        public String getValue() {
            return value;
        }

        public boolean isExpired(
                long now) {

            return expiresAtMillis
                <= now;
        }
    }

    private final UUID playerId;

    private final LinkedHashMap<String, CachedValue> values =
        new LinkedHashMap<String, CachedValue>(
            16,
            0.75F,
            true
        );

    public PlayerSnapshot(
            UUID playerId) {

        this.playerId =
            playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public CachedValue get(
            String token,
            long now) {

        CachedValue value =
            values.get(token);

        if (value == null) {
            return null;
        }

        if (value.isExpired(now)) {

            values.remove(
                token
            );

            return null;
        }

        return value;
    }

    public void put(
            String token,
            String value,
            long ttlMillis,
            int maxEntries) {

        long expiresAt =
            System.currentTimeMillis()
                + Math.max(
                    1L,
                    ttlMillis
                );

        values.put(
            token,
            new CachedValue(
                value,
                expiresAt
            )
        );

        trim(
            maxEntries
        );
    }

    public int size() {
        return values.size();
    }

    public void clear() {
        values.clear();
    }

    private void trim(
            int maxEntries) {

        int safeMax =
            Math.max(
                8,
                maxEntries
            );

        while (values.size()
                > safeMax) {

            Iterator<Map.Entry<String, CachedValue>> iterator =
                values.entrySet()
                    .iterator();

            if (!iterator.hasNext()) {
                return;
            }

            iterator.next();
            iterator.remove();
        }
    }
}
