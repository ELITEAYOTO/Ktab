package me.krunsh.ktab.cache;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cache du dernier rendu réellement envoyé à chaque viewer.
 *
 * Le cache permet de ne produire aucun packet lorsqu'un cycle donne
 * exactement le même header/footer/player-list-name que le précédent.
 */
public final class RenderedTabCache {

    private final ConcurrentMap<UUID, Snapshot> entries =
        new ConcurrentHashMap<UUID, Snapshot>();

    public boolean changed(
            UUID playerId,
            String header,
            String footer,
            String listName) {

        if (playerId == null) {
            return false;
        }

        Snapshot next =
            new Snapshot(
                safe(header),
                safe(footer),
                safe(listName)
            );

        Snapshot previous =
            entries.put(
                playerId,
                next
            );

        return !next.equals(previous);
    }

    public void remove(
            UUID playerId) {

        if (playerId != null) {
            entries.remove(playerId);
        }
    }

    public void retainOnly(
            Iterable<UUID> onlineIds) {

        Map<UUID, Boolean> online =
            new ConcurrentHashMap<UUID, Boolean>();

        if (onlineIds != null) {
            for (UUID id : onlineIds) {
                if (id != null) {
                    online.put(
                        id,
                        Boolean.TRUE
                    );
                }
            }
        }

        for (UUID id : entries.keySet()) {
            if (!online.containsKey(id)) {
                entries.remove(id);
            }
        }
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    private static String safe(
            String value) {

        return value == null
            ? ""
            : value;
    }

    private static final class Snapshot {

        private final String header;
        private final String footer;
        private final String listName;

        private Snapshot(
                String header,
                String footer,
                String listName) {

            this.header = header;
            this.footer = footer;
            this.listName = listName;
        }

        @Override
        public boolean equals(
                Object object) {

            if (this == object) {
                return true;
            }

            if (!(object instanceof Snapshot)) {
                return false;
            }

            Snapshot other =
                (Snapshot) object;

            return header.equals(other.header)
                && footer.equals(other.footer)
                && listName.equals(other.listName);
        }

        @Override
        public int hashCode() {

            int result =
                header.hashCode();

            result =
                31 * result
                    + footer.hashCode();

            result =
                31 * result
                    + listName.hashCode();

            return result;
        }
    }
}
