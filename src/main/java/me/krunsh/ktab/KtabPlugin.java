package me.krunsh.ktab;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.render.PlaceholderRenderer;
import me.krunsh.ktab.service.TabService;
import me.krunsh.ktab.service.VirtualTabService;

/**
 * Point d'entrée de Ktab.
 */
public final class KtabPlugin extends JavaPlugin {

    private KtabConfig ktabConfig;
    private PlaceholderRenderer placeholderRenderer;

    private TabService tabService;
    private VirtualTabService virtualTabService;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        ktabConfig =
            new KtabConfig(this);

        ktabConfig.reload();

        placeholderRenderer =
            new PlaceholderRenderer();

        tabService =
            new TabService(
                this,
                ktabConfig,
                placeholderRenderer
            );

        virtualTabService =
            new VirtualTabService(
                this,
                ktabConfig,
                placeholderRenderer
            );

        tabService.start();
        virtualTabService.start();

        getLogger().info(
            "Ktab actif."
        );
    }

    @Override
    public void onDisable() {

        if (virtualTabService != null) {
            virtualTabService.shutdown();
        }

        if (tabService != null) {
            tabService.shutdown();
        }
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (!command.getName()
                .equalsIgnoreCase("ktab")) {

            return false;
        }

        if (!sender.hasPermission(
                "ktab.admin")) {

            sender.sendMessage(
                "§cTu n'as pas la permission."
            );

            return true;
        }

        if (args.length == 0
                || "status".equalsIgnoreCase(
                    args[0])) {

            sendStatus(sender);
            return true;
        }

        if ("reload".equalsIgnoreCase(
                args[0])) {

            ktabConfig.reload();

            tabService.restart();
            virtualTabService.restart();

            sender.sendMessage(
                "§aKtab rechargé."
            );

            return true;
        }

        if ("preview".equalsIgnoreCase(
                args[0])) {

            if (!(sender instanceof Player)) {

                sender.sendMessage(
                    "§c/ktab preview doit être utilisé en jeu."
                );

                return true;
            }

            sendPreview(
                (Player) sender
            );

            return true;
        }

        if ("clear".equalsIgnoreCase(
                args[0])) {

            if (sender instanceof Player) {

                virtualTabService.clear(
                    (Player) sender
                );

                sender.sendMessage(
                    "§aEntrées virtuelles retirées."
                );

            } else {

                virtualTabService.clearAll();

                sender.sendMessage(
                    "§aEntrées virtuelles retirées pour tous les joueurs."
                );
            }

            return true;
        }

        sender.sendMessage(
            "§7Usage: §e/ktab <reload|status|preview|clear>"
        );

        return true;
    }

    private void sendStatus(
            CommandSender sender) {

        sender.sendMessage(
            "§8----------------------------------------"
        );

        sender.sendMessage(
            "§6§lKtab §7- Status"
        );

        sender.sendMessage(
            "§7Enabled: "
                + yn(
                    ktabConfig.isEnabled()
                )
        );

        sender.sendMessage(
            "§7Header/Footer interval: §f"
                + ktabConfig
                    .getUpdateIntervalTicks()
                + "t"
        );

        sender.sendMessage(
            "§7Virtual: "
                + yn(
                    ktabConfig
                        .isVirtualLayoutEnabled()
                )
                + " §8| §7"
                + ktabConfig
                    .getVirtualColumnsCount()
                + "x"
                + ktabConfig
                    .getVirtualRows()
        );

        sender.sendMessage(
            "§7Virtual cache: §f"
                + virtualTabService
                    .getCachedViewerCount()
        );

        sender.sendMessage(
            "§7Virtual packets: §a+"
                + virtualTabService
                    .getLastAdds()
                + " §e~"
                + virtualTabService
                    .getLastUpdates()
                + " §c-"
                + virtualTabService
                    .getLastRemoves()
        );

        sender.sendMessage(
            "§7Virtual cycle: §f"
                + virtualTabService
                    .getLastCycleMillis()
                + " ms"
        );

        sender.sendMessage(
            "§8----------------------------------------"
        );
    }

    private void sendPreview(
            Player player) {

        List<String> lines =
            virtualTabService.preview(
                player
            );

        player.sendMessage(
            "§8----- §6Ktab Preview §8-----"
        );

        int index = 0;

        for (String line : lines) {

            player.sendMessage(
                "§8"
                    + index
                    + ". §r"
                    + line
            );

            index++;
        }

        player.sendMessage(
            "§8Entrées: §f"
                + lines.size()
        );
    }

    private static String yn(
            boolean value) {

        return value
            ? "§aON"
            : "§cOFF";
    }
}
