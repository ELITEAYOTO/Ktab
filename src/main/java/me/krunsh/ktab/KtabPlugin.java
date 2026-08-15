package me.krunsh.ktab;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import me.krunsh.ktab.command.KtabCommand;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.listener.KtabPlayerListener;
import me.krunsh.ktab.logging.KtabConsole;
import me.krunsh.ktab.performance.DirtyReason;
import me.krunsh.ktab.performance.KtabSchedulerService;
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
    private KtabSchedulerService schedulerService;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        ktabConfig =
            new KtabConfig(this);

        ktabConfig.reload();

        placeholderRenderer =
            new PlaceholderRenderer(
                ktabConfig
            );

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
                placeholderRenderer
            );

        schedulerService =
            new KtabSchedulerService(
                this,
                ktabConfig,
                tabService,
                virtualTabService,
                visibilityController,
                placeholderRenderer
            );

        getServer()
            .getPluginManager()
            .registerEvents(
                new KtabPlayerListener(
                    this,
                    ktabConfig,
                    schedulerService,
                    visibilityController
                ),
                this
            );

        registerCommand();

        tabService.start();
        virtualTabService.start();
        schedulerService.start();

        if (ktabConfig.isVirtualLayoutEnabled()) {

            getServer()
                .getScheduler()
                .runTaskLater(
                    this,
                    new Runnable() {
                        @Override
                        public void run() {

                            visibilityController
                                .applyAllBatched();

                            schedulerService
                                .markAllDirty(
                                    DirtyReason.CONFIG
                                );
                        }
                    },
                    ktabConfig
                        .getVisibilityInitialDelayTicks()
                );
        }

        KtabConsole.startupSummary(
            this,
            ktabConfig,
            visibilityController
        );
    }

    @Override
    public void onDisable() {

        if (schedulerService != null) {
            schedulerService.shutdown();
        }

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
                visibilityController,
                schedulerService
            );

        command.setExecutor(
            executor
        );

        command.setTabCompleter(
            executor
        );
    }
}
