package me.krunsh.ktab.performance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Rotation stable des viewers.
 *
 * À 700 joueurs avec refresh_window_ticks=40 :
 * ceil(700 / 40) = 18 viewers réguliers par tick.
 */
public final class RefreshWheel {

    private final List<UUID> order =
        new ArrayList<UUID>();

    private final Set<UUID> members =
        new HashSet<UUID>();

    private int cursor;

    public void register(
            UUID viewerId) {

        if (viewerId == null
                || members.contains(
                    viewerId
                )) {

            return;
        }

        members.add(
            viewerId
        );

        order.add(
            viewerId
        );
    }

    public void register(
            Player player) {

        if (player != null) {
            register(
                player.getUniqueId()
            );
        }
    }

    public void unregister(
            UUID viewerId) {

        if (viewerId == null
                || !members.remove(
                    viewerId
                )) {

            return;
        }

        int index =
            order.indexOf(
                viewerId
            );

        if (index < 0) {
            return;
        }

        order.remove(
            index
        );

        if (order.isEmpty()) {
            cursor = 0;
            return;
        }

        if (index < cursor) {
            cursor--;
        }

        if (cursor >= order.size()) {
            cursor = 0;
        }
    }

    public void rebuild(
            Collection<? extends Player> players) {

        clear();

        if (players == null) {
            return;
        }

        for (Player player : players) {

            if (player != null
                    && player.isOnline()) {

                register(player);
            }
        }
    }

    public List<UUID> poll(
            int maximum) {

        int limit =
            Math.max(
                0,
                maximum
            );

        if (limit == 0
                || order.isEmpty()) {

            return new ArrayList<UUID>();
        }

        int count =
            Math.min(
                limit,
                order.size()
            );

        List<UUID> result =
            new ArrayList<UUID>(
                count
            );

        for (int i = 0;
                i < count;
                i++) {

            if (order.isEmpty()) {
                break;
            }

            if (cursor >= order.size()) {
                cursor = 0;
            }

            result.add(
                order.get(
                    cursor
                )
            );

            cursor++;

            if (cursor >= order.size()) {
                cursor = 0;
            }
        }

        return result;
    }

    public List<UUID> snapshot() {
        return new ArrayList<UUID>(
            order
        );
    }

    public int size() {
        return order.size();
    }

    public int getCursor() {
        return cursor;
    }

    public void clear() {
        order.clear();
        members.clear();
        cursor = 0;
    }

    public static int recommendedPerTick(
            int viewers,
            int refreshWindowTicks,
            int configuredMaximum) {

        if (viewers <= 0) {
            return 0;
        }

        int window =
            Math.max(
                1,
                refreshWindowTicks
            );

        int needed =
            (viewers + window - 1)
                / window;

        int maximum =
            Math.max(
                1,
                configuredMaximum
            );

        return Math.max(
            1,
            Math.min(
                needed,
                maximum
            )
        );
    }
}
