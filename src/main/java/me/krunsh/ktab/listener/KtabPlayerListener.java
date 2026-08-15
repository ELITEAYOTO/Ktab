package me.krunsh.ktab.listener;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.service.TabService;
import me.krunsh.ktab.service.VirtualTabService;

/**
 * Lifecycle joueur Ktab.
 *
 * Le petit délai après join laisse Minecraft terminer l'ajout normal du joueur
 * au player-info avant d'envoyer les entrées virtuelles.
 */
public final class KtabPlayerListener
        implements Listener {

    private final KtabPlugin plugin;
    private final TabService tabService;
    private final VirtualTabService virtualTabService;

    public KtabPlayerListener(
            KtabPlugin plugin,
            TabService tabService,
            VirtualTabService virtualTabService) {

        if (plugin == null
                || tabService == null
                || virtualTabService == null) {

            throw new IllegalArgumentException(
                "Dépendance KtabPlayerListener manquante."
            );
        }

        this.plugin = plugin;
        this.tabService = tabService;
        this.virtualTabService = virtualTabService;
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

                        /*
                         * Le nouveau joueur reçoit immédiatement son Header/Footer.
                         */
                        tabService.refresh(
                            player
                        );

                        /*
                         * Le nombre de vrais joueurs influence le nombre maximum
                         * d'entrées fake : tous les viewers sont donc recalculés.
                         */
                        virtualTabService.refreshAll();
                    }
                },
                5L
            );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(
            PlayerQuitEvent event) {

        final UUID playerId =
            event.getPlayer()
                .getUniqueId();

        /*
         * Aucun packet n'est nécessaire pour le joueur qui part :
         * on libère uniquement ses snapshots.
         */
        tabService.remove(
            playerId
        );

        virtualTabService.removeCache(
            playerId
        );

        /*
         * Après le quit, le nombre de vrais joueurs a changé et peut libérer
         * une ou plusieurs cellules virtuelles.
         */
        plugin.getServer()
            .getScheduler()
            .runTaskLater(
                plugin,
                new Runnable() {
                    @Override
                    public void run() {
                        virtualTabService.refreshAll();
                    }
                },
                1L
            );
    }
}
