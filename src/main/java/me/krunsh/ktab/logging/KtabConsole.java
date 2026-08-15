package me.krunsh.ktab.logging;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.visibility.TabVisibilityController;

/**
 * Console colorée centralisée.
 *
 * PandaSpigot affiche les couleurs envoyées au ConsoleSender.
 * Si logging.colors=false, les codes sont automatiquement retirés.
 */
public final class KtabConsole {

    private static final String PREFIX =
        "&8[&6Ktab&8] &r";

    private KtabConsole() {
    }

    public static void info(
            KtabPlugin plugin,
            String message) {

        send(
            plugin,
            "&7" + message
        );
    }

    public static void success(
            KtabPlugin plugin,
            String message) {

        send(
            plugin,
            "&a✔ &f" + message
        );
    }

    public static void warning(
            KtabPlugin plugin,
            String message) {

        send(
            plugin,
            "&e⚠ &f" + message
        );
    }

    public static void error(
            KtabPlugin plugin,
            String message) {

        send(
            plugin,
            "&c✖ &f" + message
        );
    }

    public static void send(
            KtabPlugin plugin,
            String message) {

        if (plugin == null) {
            return;
        }

        CommandSender console =
            Bukkit.getConsoleSender();

        String translated =
            ChatColor.translateAlternateColorCodes(
                '&',
                PREFIX
                    + (message == null
                        ? ""
                        : message)
            );

        if (!plugin.getConfig()
                .getBoolean(
                    "logging.colors",
                    true
                )) {

            translated =
                ChatColor.stripColor(
                    translated
                );
        }

        console.sendMessage(
            translated
        );
    }

    public static void separator(
            KtabPlugin plugin) {

        send(
            plugin,
            "&8&m--------------------------------------------------"
        );
    }

    public static void startupSummary(
            KtabPlugin plugin,
            KtabConfig config,
            TabVisibilityController visibilityController) {

        if (plugin == null
                || config == null
                || !plugin.getConfig()
                    .getBoolean(
                        "logging.startup.enabled",
                        true
                    )) {

            return;
        }

        boolean compact =
            plugin.getConfig()
                .getBoolean(
                    "logging.startup.compact",
                    false
                );

        if (compact) {

            send(
                plugin,
                "&a✔ &f"
                    + plugin.getDescription()
                        .getVersion()
                    + " &8| &7NMS &f"
                    + (visibilityController == null
                        ? "?"
                        : visibilityController
                            .getNmsVersion())
                    + " &8| &7PAPI &aON"
                    + " &8| &7ServerNPC "
                    + (visibilityController != null
                        && visibilityController
                            .isServerNpcAvailable()
                        ? "&aON"
                        : "&cOFF")
                    + " &8| &7V9 &aON"
                    + " &8| &7Batch "
                    + (config
                        .isPerformancePacketBatchingEnabled()
                        ? "&aON"
                        : "&cOFF")
                    + " &8| &aReady"
            );

            return;
        }

        if (plugin.getConfig()
                .getBoolean(
                    "logging.startup.banner",
                    true
                )) {

            separator(plugin);

            send(
                plugin,
                "&6&lKtab &f"
                    + plugin.getDescription()
                        .getVersion()
                    + " &8- &7Volkaria TAB Engine"
            );

            separator(plugin);
        }

        if (plugin.getConfig()
                .getBoolean(
                    "logging.startup.hooks",
                    true
                )) {

            send(
                plugin,
                "&a✔ &7PlaceholderAPI    &aHOOKED"
            );

            send(
                plugin,
                (visibilityController != null
                    && visibilityController
                        .isServerNpcAvailable()
                    ? "&a✔"
                    : "&c✖")
                    + " &7ServerNPC         "
                    + (visibilityController != null
                        && visibilityController
                            .isServerNpcAvailable()
                        ? "&aHOOKED"
                        : "&cOFF")
            );

            send(
                plugin,
                "&a✔ &7NMS               &f"
                    + (visibilityController == null
                        ? "?"
                        : visibilityController
                            .getNmsVersion())
            );
        }

        if (plugin.getConfig()
                .getBoolean(
                    "logging.startup.performance",
                    true
                )) {

            send(
                plugin,
                "&6Performance V9"
            );

            send(
                plugin,
                "&a✔ &7Scheduler         "
                    + (config.isPerformanceEnabled()
                        ? "&aDISTRIBUTED"
                        : "&eLEGACY")
                    + " &8(window=&f"
                    + config
                        .getPerformanceRefreshWindowTicks()
                    + "t&8)"
            );

            send(
                plugin,
                "&a✔ &7Compiled PAPI     "
                    + onOff(
                        config
                            .isPerformancePlaceholderCompiledTemplates()
                    )
            );

            send(
                plugin,
                "&a✔ &7Snapshot cache    "
                    + onOff(
                        config
                            .isPerformancePlaceholderCacheEnabled()
                    )
            );

            send(
                plugin,
                "&a✔ &7Render cache      "
                    + onOff(
                        config
                            .isPerformanceRenderCacheEnabled()
                    )
            );

            send(
                plugin,
                "&a✔ &7Packet batching   "
                    + onOff(
                        config
                            .isPerformancePacketBatchingEnabled()
                    )
                    + " &8(max=&f"
                    + config
                        .getPerformancePacketMaxEntriesPerPacket()
                    + "&8)"
            );

            send(
                plugin,
                "&a✔ &7Visibility        "
                    + (config
                        .isPerformanceVisibilityEventDriven()
                        ? "&aEVENT-DRIVEN"
                        : "&eSWEEP")
            );
        }

        if (plugin.getConfig()
                .getBoolean(
                    "logging.startup.layout",
                    true
                )) {

            send(
                plugin,
                "&6Layout"
            );

            send(
                plugin,
                "&7Grid              &f"
                    + config
                        .getVirtualColumnsCount()
                    + "x"
                    + config
                        .getVirtualRows()
                    + " &8| &7max entries=&f"
                    + config
                        .getVirtualMaxEntries()
            );

            send(
                plugin,
                "&7Skins             &f"
                    + config.getSkinCount()
                    + " &8| &7hide real="
                    + onOff(
                        config.isHideRealPlayers()
                    )
            );
        }

        send(
            plugin,
            "&a&lReady."
        );

        separator(plugin);
    }

    private static String onOff(
            boolean value) {

        return value
            ? "&aON"
            : "&cOFF";
    }
}
