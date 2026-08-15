package me.krunsh.ktab;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import me.krunsh.ktab.command.KtabCommand;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.listener.KtabPlayerListener;
import me.krunsh.ktab.render.PlaceholderRenderer;
import me.krunsh.ktab.service.TabService;
import me.krunsh.ktab.service.VirtualTabService;
import me.krunsh.ktab.visibility.TabVisibilityController;

/**
 * Point d'entrée de Ktab.
 *
 * La classe principale ne contient plus la logique des commandes :
 * KtabCommand porte désormais toute l'administration et le Skin Toolkit.
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

        registerCommand();

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

    private void registerCommand() {

        PluginCommand command =
            getCommand(
                "ktab"
            );

        if (command == null) {

            throw new IllegalStateException(
                "Commande /ktab absente de plugin.yml."
            );
        }

        KtabCommand executor =
            new KtabCommand(
                this,
                ktabConfig,
                placeholderRenderer,
                tabService,
                virtualTabService,
                visibilityController
            );

        command.setExecutor(
            executor
        );

        command.setTabCompleter(
            executor
        );
    }
}
