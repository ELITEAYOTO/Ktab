package me.krunsh.ktab.render;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Renderer texte central de Ktab.
 *
 * Placeholders natifs Ktab :
 * - %player_name%
 * - %player_ping%
 * - %server_online%
 * - %server_max_players%
 *
 * Tous les autres placeholders sont ensuite délégués à PlaceholderAPI.
 */
public final class PlaceholderRenderer {

    public String renderLines(
            Player player,
            List<String> lines,
            boolean usePlaceholderApi) {

        if (lines == null
                || lines.isEmpty()) {

            return "";
        }

        StringBuilder builder =
            new StringBuilder();

        for (int i = 0;
                i < lines.size();
                i++) {

            if (i > 0) {
                builder.append('\n');
            }

            builder.append(
                render(
                    player,
                    lines.get(i),
                    usePlaceholderApi
                )
            );
        }

        return builder.toString();
    }

    public String render(
            Player player,
            String input,
            boolean usePlaceholderApi) {

        String output =
            input == null
                ? ""
                : input;

        output =
            output.replace(
                "%server_online%",
                String.valueOf(
                    Bukkit.getOnlinePlayers()
                        .size()
                )
            );

        output =
            output.replace(
                "%server_max_players%",
                String.valueOf(
                    Bukkit.getMaxPlayers()
                )
            );

        if (player != null) {

            output =
                output.replace(
                    "%player_name%",
                    player.getName()
                );

            output =
                output.replace(
                    "%player_ping%",
                    String.valueOf(
                        resolvePing(player)
                    )
                );
        }

        if (usePlaceholderApi
                && player != null) {

            output =
                PlaceholderAPI.setPlaceholders(
                    player,
                    output
                );
        }

        return ChatColor.translateAlternateColorCodes(
            '&',
            output
        );
    }

    /**
     * Bukkit 1.8 ne fournit pas Player#getPing().
     * Lecture fail-safe de EntityPlayer.ping par réflexion.
     */
    private static int resolvePing(
            Player player) {

        if (player == null) {
            return 0;
        }

        try {

            Method getHandle =
                player.getClass()
                    .getMethod(
                        "getHandle"
                    );

            Object handle =
                getHandle.invoke(
                    player
                );

            Field ping =
                handle.getClass()
                    .getField(
                        "ping"
                    );

            return Math.max(
                0,
                ping.getInt(handle)
            );

        } catch (Exception ignored) {
            return 0;
        }
    }
}
