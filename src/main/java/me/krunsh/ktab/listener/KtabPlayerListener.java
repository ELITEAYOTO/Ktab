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
import me.krunsh.ktab.performance.DirtyReason;
import me.krunsh.ktab.performance.KtabSchedulerService;
import me.krunsh.ktab.visibility.TabVisibilityController;

/**
 * Lifecycle joueur Ktab.
 *
 * V9.1 supprime les anciens applyAll()/refreshAll() sur chaque join/quit.
 */
public final class KtabPlayerListener
        implements Listener {

    private final KtabPlugin plugin;
    private final KtabConfig config;

    private final KtabSchedulerService scheduler;
    private final TabVisibilityController visibilityController;

    public KtabPlayerListener(
            KtabPlugin plugin,
            KtabConfig config,
            KtabSchedulerService scheduler,
            TabVisibilityController visibilityController) {

        if (plugin == null
                || config == null
                || scheduler == null
                || visibilityController == null) {

            throw new IllegalArgumentException(
                "Dépendance KtabPlayerListener manquante."
            );
        }

        this.plugin = plugin;
        this.config = config;
        this.scheduler = scheduler;
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

                        if (config.isPerformanceVisibilityEventDriven()) {

                            /*
                             * Le nouveau viewer retire la liste actuelle.
                             */
                            visibilityController
                                .hideExistingFrom(
                                    player
                                );

                            /*
                             * Les viewers existants retirent uniquement le
                             * profil du nouveau joueur.
                             */
                            visibilityController
                                .hideRealPlayerFromOthers(
                                    player
                                );

                        } else {

                            visibilityController
                                .applyAllBatched();
                        }

                        scheduler.register(
                            player
                        );

                        scheduler.markDirty(
                            player,
                            DirtyReason.JOIN
                        );

                        if (config
                                .isPerformanceRefreshGlobalOnJoinQuit()) {

                            scheduler.markAllDirty(
                                DirtyReason.GLOBAL
                            );
                        }
                    }
                },
                config.getVisibilityInitialDelayTicks()
            );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(
            PlayerQuitEvent event) {

        UUID playerId =
            event.getPlayer()
                .getUniqueId();

        scheduler.unregister(
            playerId
        );

        if (config
                .isPerformanceRefreshGlobalOnJoinQuit()) {

            scheduler.markAllDirty(
                DirtyReason.GLOBAL
            );
        }

        /*
         * Aucun applyAll ici :
         * vanilla retire déjà le profil du joueur déconnecté.
         */
    }
}
