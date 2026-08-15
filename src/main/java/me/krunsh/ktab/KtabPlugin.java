package me.krunsh.ktab;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.listener.KtabPlayerListener;
import me.krunsh.ktab.render.PlaceholderRenderer;
import me.krunsh.ktab.service.TabService;
import me.krunsh.ktab.service.VirtualTabService;
import me.krunsh.ktab.visibility.TabVisibilityController;

/**
 * Point d'entrée de Ktab.
 */
public final class KtabPlugin extends JavaPlugin {

    private KtabConfig ktabConfig;
    private PlaceholderRenderer placeholderRenderer;

    private TabService tabService;
    private VirtualTabService virtualTabService;
    private TabVisibilityController visibilityController;

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

        visibilityController =
            new TabVisibilityController(
                this,
                ktabConfig
            );

        virtualTabService =
            new VirtualTabService(
                this,
                ktabConfig,
                placeholderRenderer,
                visibilityController
            );

        getServer()
            .getPluginManager()
            .registerEvents(
                new KtabPlayerListener(
                    this,
                    ktabConfig,
                    tabService,
                    virtualTabService,
                    visibilityController
                ),
                this
            );

        tabService.start();
        virtualTabService.start();

        if (ktabConfig.isVirtualLayoutEnabled()) {

            getServer()
                .getScheduler()
                .runTaskLater(
                    this,
                    new Runnable() {
                        @Override
                        public void run() {

                            visibilityController.applyAll();
                            virtualTabService.refreshAll();
                        }
                    },
                    ktabConfig
                        .getVisibilityInitialDelayTicks()
                );
        }

        getLogger().info(
            "Ktab actif."
        );
    }

    @Override
    public void onDisable() {

        if (virtualTabService != null) {
            virtualTabService.shutdown();
        }

        /*
         * Si on a volontairement caché les joueurs réels, on les restaure
         * avant que Ktab disparaisse.
         */
        if (visibilityController != null
                && ktabConfig != null
                && ktabConfig.isHideRealPlayers()) {

            visibilityController
                .restoreRealPlayers();
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

            boolean wasHidingRealPlayers =
                ktabConfig.isHideRealPlayers();

            ktabConfig.reload();

            visibilityController
                .refreshHooks();

            if (wasHidingRealPlayers
                    && !ktabConfig
                        .isHideRealPlayers()) {

                visibilityController
                    .restoreRealPlayers();
            }

            tabService.restart();
            virtualTabService.restart();

            if (ktabConfig
                    .isVirtualLayoutEnabled()) {

                visibilityController
                    .applyAll();

                virtualTabService
                    .refreshAll();
            }

            sender.sendMessage(
                "§aKtab rechargé."
            );

            return true;
        }

        if ("preview".equalsIgnoreCase(
                args[0])) {

            Player target =
                resolveTarget(
                    sender,
                    args,
                    1
                );

            if (target == null) {
                return true;
            }

            sendPreview(
                sender,
                target
            );

            return true;
        }

        if ("debug".equalsIgnoreCase(
                args[0])) {

            Player target =
                resolveTarget(
                    sender,
                    args,
                    1
                );

            if (target == null) {
                return true;
            }

            sendDebug(
                sender,
                target
            );

            return true;
        }

        if ("refresh".equalsIgnoreCase(
                args[0])) {

            if (args.length >= 2
                    && "all".equalsIgnoreCase(
                        args[1])) {

                visibilityController.applyAll();
                tabService.refreshAll();
                virtualTabService.refreshAll();

                sender.sendMessage(
                    "§aRefresh Ktab demandé pour tous les joueurs."
                );

                return true;
            }

            Player target =
                resolveTarget(
                    sender,
                    args,
                    1
                );

            if (target == null) {
                return true;
            }

            visibilityController.apply(
                target
            );

            tabService.refresh(
                target
            );

            virtualTabService.refresh(
                target
            );

            sender.sendMessage(
                "§aRefresh Ktab demandé pour §e"
                    + target.getName()
                    + "§a."
            );

            return true;
        }

        if ("clear".equalsIgnoreCase(
                args[0])) {

            if (args.length >= 2
                    && "all".equalsIgnoreCase(
                        args[1])) {

                virtualTabService.clearAll();

                sender.sendMessage(
                    "§aEntrées virtuelles retirées pour tous les joueurs."
                );

                return true;
            }

            Player target =
                resolveTarget(
                    sender,
                    args,
                    1
                );

            if (target == null) {
                return true;
            }

            virtualTabService.clear(
                target
            );

            sender.sendMessage(
                "§aEntrées virtuelles retirées pour §e"
                    + target.getName()
                    + "§a."
            );

            return true;
        }

        sender.sendMessage(
            "§7Usage: §e/ktab <reload|status|preview|debug|refresh|clear>"
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
            "§7PlaceholderAPI: "
                + yn(
                    Bukkit.getPluginManager()
                        .isPluginEnabled(
                            "PlaceholderAPI"
                        )
                )
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
            "§7Hide real players: "
                + yn(
                    ktabConfig
                        .isHideRealPlayers()
                )
                + " §8| §7last=§f"
                + visibilityController
                    .getLastHiddenRealPlayers()
        );

        sender.sendMessage(
            "§7Hide ServerNPC: "
                + yn(
                    ktabConfig
                        .isHideServerNpcs()
                )
                + " §8| §7hook="
                + yn(
                    visibilityController
                        .isServerNpcAvailable()
                )
                + " §8| §7last=§f"
                + visibilityController
                    .getLastHiddenNpcs()
        );

        sender.sendMessage(
            "§7Virtual cache: §f"
                + virtualTabService
                    .getCachedViewerCount()
                + " §8| §7packets: §a+"
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
            "§7NMS visibility: §f"
                + visibilityController
                    .getNmsVersion()
        );

        sender.sendMessage(
            "§8----------------------------------------"
        );
    }

    private void sendPreview(
            CommandSender sender,
            Player target) {

        List<String> lines =
            virtualTabService.preview(
                target
            );

        sender.sendMessage(
            "§8----- §6Ktab Preview §8-----"
        );

        sender.sendMessage(
            "§7Viewer: §f"
                + target.getName()
        );

        int index = 0;

        for (String line : lines) {

            sender.sendMessage(
                "§8"
                    + index
                    + ". §r"
                    + line
            );

            index++;
        }

        sender.sendMessage(
            "§8Entrées: §f"
                + lines.size()
        );
    }

    private void sendDebug(
            CommandSender sender,
            Player target) {

        sender.sendMessage(
            "§8----------------------------------------"
        );

        sender.sendMessage(
            "§6§lKtab §7- Debug §f"
                + target.getName()
        );

        sender.sendMessage(
            "§7UUID: §f"
                + target.getUniqueId()
        );

        sender.sendMessage(
            "§7Header/Footer cached: "
                + yn(
                    tabService.isCached(
                        target.getUniqueId()
                    )
                )
        );

        sender.sendMessage(
            "§7Virtual entries cached: §f"
                + virtualTabService
                    .getCachedEntryCount(
                        target.getUniqueId()
                    )
        );

        sender.sendMessage(
            "§7Virtual preview entries: §f"
                + virtualTabService
                    .preview(target)
                    .size()
        );

        sender.sendMessage(
            "§7Visibility real/npc: §f"
                + visibilityController
                    .getLastHiddenRealPlayers()
                + "/"
                + visibilityController
                    .getLastHiddenNpcs()
        );

        sender.sendMessage(
            "§7Kjobs PAPI:"
        );

        sender.sendMessage(
            "§8  display_job_name = §f"
                + placeholderRenderer.render(
                    target,
                    "%kjob_display_job_name%",
                    true
                )
        );

        sender.sendMessage(
            "§8  global_level = §f"
                + placeholderRenderer.render(
                    target,
                    "%kjob_global_level%",
                    true
                )
        );

        sender.sendMessage(
            "§8  claimable_quests = §f"
                + placeholderRenderer.render(
                    target,
                    "%kjob_claimable_quests%",
                    true
                )
        );

        sender.sendMessage(
            "§8----------------------------------------"
        );
    }

    private Player resolveTarget(
            CommandSender sender,
            String[] args,
            int index) {

        if (args.length > index) {

            if ("all".equalsIgnoreCase(
                    args[index])) {

                sender.sendMessage(
                    "§cCette commande attend un joueur, pas 'all'."
                );

                return null;
            }

            Player target =
                Bukkit.getPlayerExact(
                    args[index]
                );

            if (target == null) {

                sender.sendMessage(
                    "§cJoueur introuvable ou hors ligne: §e"
                        + args[index]
                );

                return null;
            }

            return target;
        }

        if (sender instanceof Player) {
            return (Player) sender;
        }

        sender.sendMessage(
            "§cDepuis la console, indique un joueur."
        );

        return null;
    }

    private static String yn(
            boolean value) {

        return value
            ? "§aON"
            : "§cOFF";
    }
}
