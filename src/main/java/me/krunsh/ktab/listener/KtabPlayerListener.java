package me.krunsh.ktab.listener;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.service.TabService;
import me.krunsh.ktab.service.VirtualTabService;
import me.krunsh.ktab.visibility.TabVisibilityController;

/**
 * Lifecycle joueur Ktab.
 */
public final class KtabPlayerListener
        implements Listener {

    private final KtabPlugin plugin;
    private final KtabConfig config;

    private final TabService tabService;
    private final VirtualTabService virtualTabService;
    private final TabVisibilityController visibilityController;

    public KtabPlayerListener(
            KtabPlugin plugin,
            KtabConfig config,
            TabService tabService,
            VirtualTabService virtualTabService,
            TabVisibilityController visibilityController) {

        if (plugin == null
                || config == null
                || tabService == null
                || virtualTabService == null
                || visibilityController == null) {

            throw new IllegalArgumentException(
                "Dépendance KtabPlayerListener manquante."
            );
        }

        this.plugin = plugin;
        this.config = config;
        this.tabService = tabService;
        this.virtualTabService = virtualTabService;
        this.visibilityController =
            visibilityController;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(
            PlayerJoinEvent event) {

        final UUID playerId =
            event.getPlayer()
                .getUniqueId();

        plugin.getServer()
            .getScheduler()
            .runTaskLater(
                plugin,
                new Runnable() {
                    @Override
                    public void run() {

                        Player player =
                            plugin.getServer()
                                .getPlayer(
                                    playerId
                                );

                        if (player == null
                                || !player.isOnline()) {

                            return;
                        }

                        tabService.refresh(
                            player
                        );

                        /*
                         * ServerNPC et le serveur vanilla ont eu le temps
                         * d'ajouter leurs PlayerInfo. On les retire ensuite.
                         */
                        visibilityController.applyAll();

                        virtualTabService.refreshAll();
                    }
                },
                config.getVisibilityInitialDelayTicks()
            );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(
            PlayerQuitEvent event) {

        final UUID playerId =
            event.getPlayer()
                .getUniqueId();

        tabService.remove(
            playerId
        );

        virtualTabService.removeCache(
            playerId
        );

        plugin.getServer()
            .getScheduler()
            .runTaskLater(
                plugin,
                new Runnable() {
                    @Override
                    public void run() {

                        visibilityController.applyAll();
                        virtualTabService.refreshAll();
                    }
                },
                1L
            );
    }
}
