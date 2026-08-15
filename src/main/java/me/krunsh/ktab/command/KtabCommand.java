package me.krunsh.ktab.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.layout.LayoutDecision;
import me.krunsh.ktab.layout.LayoutRenderResult;
import me.krunsh.ktab.layout.RenderedVirtualCell;
import me.krunsh.ktab.performance.DirtyReason;
import me.krunsh.ktab.performance.KtabSchedulerService;
import me.krunsh.ktab.performance.PerformanceMetrics;
import me.krunsh.ktab.render.PlaceholderRenderer;
import me.krunsh.ktab.service.TabService;
import me.krunsh.ktab.service.VirtualTabService;
import me.krunsh.ktab.skin.ResolvedTabSkin;
import me.krunsh.ktab.visibility.TabVisibilityController;
import me.krunsh.ktab.validation.KtabValidator;
import me.krunsh.ktab.validation.ValidationIssue;

/**
 * Commande /ktab.
 *
 * V6 extrait l'administration de la classe principale du plugin et ajoute
 * le toolkit de diagnostic des skins.
 */
public final class KtabCommand
        implements CommandExecutor, TabCompleter {

    private static final long SKIN_TEST_DURATION_TICKS =
        200L;

    private final KtabPlugin plugin;
    private final KtabConfig config;
    private final PlaceholderRenderer renderer;
    private final TabService tabService;
    private final VirtualTabService virtualTabService;
    private final TabVisibilityController visibilityController;
    private final KtabSchedulerService schedulerService;

    public KtabCommand(
            KtabPlugin plugin,
            KtabConfig config,
            PlaceholderRenderer renderer,
            TabService tabService,
            VirtualTabService virtualTabService,
            TabVisibilityController visibilityController,
            KtabSchedulerService schedulerService) {

        this.plugin = plugin;
        this.config = config;
        this.renderer = renderer;
        this.tabService = tabService;
        this.virtualTabService = virtualTabService;
        this.visibilityController = visibilityController;
        this.schedulerService = schedulerService;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

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

            handleReload(sender);
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

            if (target != null) {
                sendPreview(
                    sender,
                    target
                );
            }

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

            if (target != null) {
                sendDebug(
                    sender,
                    target
                );
            }

            return true;
        }

        if ("refresh".equalsIgnoreCase(
                args[0])) {

            handleRefresh(
                sender,
                args
            );

            return true;
        }

        if ("clear".equalsIgnoreCase(
                args[0])) {

            handleClear(
                sender,
                args
            );

            return true;
        }

        if ("validate".equalsIgnoreCase(
                args[0])) {

            sendValidation(sender);
            return true;
        }

        if ("dump".equalsIgnoreCase(
                args[0])) {

            handleDump(
                sender,
                args
            );

            return true;
        }

        if ("perf".equalsIgnoreCase(
                args[0])
                || "performance".equalsIgnoreCase(
                    args[0])) {

            handlePerformance(
                sender,
                args
            );

            return true;
        }

        if ("skin".equalsIgnoreCase(
                args[0])
                || "skins".equalsIgnoreCase(
                    args[0])) {

            handleSkin(
                sender,
                args
            );

            return true;
        }

        sendUsage(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args) {

        if (!sender.hasPermission(
                "ktab.admin")) {

            return Collections.emptyList();
        }

        if (args.length == 1) {

            return complete(
                args[0],
                Arrays.asList(
                    "status",
                    "reload",
                    "preview",
                    "debug",
                    "refresh",
                    "clear",
                    "validate",
                    "dump",
                    "perf",
                    "skin"
                )
            );
        }

        String root =
            args[0].toLowerCase();

        if (("perf".equals(root)
                || "performance".equals(root))
                && args.length == 2) {

            return complete(
                args[1],
                Arrays.asList(
                    "reset",
                    "clearcache"
                )
            );
        }

        if ("skin".equals(root)
                || "skins".equals(root)) {

            if (args.length == 2) {

                return complete(
                    args[1],
                    Arrays.asList(
                        "list",
                        "info",
                        "test",
                        "clear"
                    )
                );
            }

            String sub =
                args[1].toLowerCase();

            if (args.length == 3
                    && ("info".equals(sub)
                        || "test".equals(sub))) {

                return complete(
                    args[2],
                    skinIds()
                );
            }

            if (args.length == 3
                    && "clear".equals(sub)) {

                return complete(
                    args[2],
                    onlinePlayerNames()
                );
            }

            if (args.length == 4
                    && ("info".equals(sub)
                        || "test".equals(sub))) {

                return complete(
                    args[3],
                    onlinePlayerNames()
                );
            }

            return Collections.emptyList();
        }

        if (args.length == 2
                && ("preview".equals(root)
                    || "debug".equals(root))) {

            return complete(
                args[1],
                onlinePlayerNames()
            );
        }

        if (args.length == 2
                && ("refresh".equals(root)
                    || "clear".equals(root))) {

            List<String> values =
                onlinePlayerNames();

            values.add("all");

            return complete(
                args[1],
                values
            );
        }

        if (args.length == 2
                && "dump".equals(root)) {

            return complete(
                args[1],
                onlinePlayerNames()
            );
        }

        if (args.length == 3
                && "dump".equals(root)) {

            return complete(
                args[2],
                Arrays.asList(
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6"
                )
            );
        }

        return Collections.emptyList();
    }

    private void handleReload(
            CommandSender sender) {

        boolean wasHidingRealPlayers =
            config.isHideRealPlayers();

        boolean wasHidingServerNpcs =
            config.isHideServerNpcs();

        config.reload();

        renderer.clearCaches();

        visibilityController
            .refreshHooks();

        if (wasHidingRealPlayers
                && !config
                    .isHideRealPlayers()) {

            visibilityController
                .restoreRealPlayers();
        }

        tabService.restart();
        virtualTabService.restart();
        schedulerService.restart();

        if (config.isVirtualLayoutEnabled()) {

            boolean visibilityBecameStricter =
                (!wasHidingRealPlayers
                    && config.isHideRealPlayers())
                || (!wasHidingServerNpcs
                    && config.isHideServerNpcs());

            if (visibilityBecameStricter) {

                visibilityController
                    .applyAllBatched();
            }

            schedulerService
                .markAllDirty(
                    DirtyReason.CONFIG
                );
        }

        sender.sendMessage(
            "§aKtab rechargé. §7Scheduler V9="
                + yn(
                    config.isPerformanceEnabled()
                )
        );
    }

    private void handleRefresh(
            CommandSender sender,
            String[] args) {

        if (args.length >= 2
                && "all".equalsIgnoreCase(
                    args[1])) {

            /*
             * Un refresh global explicite ne doit pas créer un spike à
             * 700 joueurs : on remplit la DirtyQueue dédupliquée.
             */
            schedulerService
                .markAllDirty(
                    DirtyReason.MANUAL
                );

            sender.sendMessage(
                "§aRefresh Ktab global mis en file. §7Queue=§f"
                    + schedulerService
                        .getDirtyQueueSize()
            );

            return;
        }

        Player target =
            resolveTarget(
                sender,
                args,
                1
            );

        if (target == null) {
            return;
        }

        visibilityController
            .hideExistingFrom(
                target
            );

        schedulerService
            .forceRefresh(
                target
            );

        sender.sendMessage(
            "§aRefresh Ktab immédiat pour §e"
                + target.getName()
                + "§a."
        );
    }

    private void handleClear(
            CommandSender sender,
            String[] args) {

        if (args.length >= 2
                && "all".equalsIgnoreCase(
                    args[1])) {

            virtualTabService.clearAll();

            sender.sendMessage(
                "§aEntrées virtuelles retirées pour tous les joueurs."
            );

            return;
        }

        Player target =
            resolveTarget(
                sender,
                args,
                1
            );

        if (target == null) {
            return;
        }

        virtualTabService.clear(
            target
        );

        sender.sendMessage(
            "§aEntrées virtuelles retirées pour §e"
                + target.getName()
                + "§a."
        );
    }

    private void handlePerformance(
            CommandSender sender,
            String[] args) {

        if (args.length >= 2
                && "reset".equalsIgnoreCase(
                    args[1])) {

            schedulerService
                .resetMetrics();

            sender.sendMessage(
                "§aMétriques Ktab remises à zéro."
            );

            return;
        }

        if (args.length >= 2
                && "clearcache".equalsIgnoreCase(
                    args[1])) {

            renderer.clearCaches();

            schedulerService
                .markAllDirty(
                    DirtyReason.MANUAL
                );

            sender.sendMessage(
                "§aCaches templates/placeholders vidés. §7Refresh global mis en file."
            );

            return;
        }

        PerformanceMetrics metrics =
            schedulerService
                .getMetrics();

        sender.sendMessage(
            "§8----------------------------------------"
        );

        sender.sendMessage(
            "§6§lKtab §7- Performance V9"
        );

        sender.sendMessage(
            "§7Online: §f"
                + Bukkit.getOnlinePlayers()
                    .size()
                + " §8| §7wheel=§f"
                + schedulerService
                    .getWheelSize()
        );

        sender.sendMessage(
            "§7Scheduler: "
                + yn(
                    config.isPerformanceEnabled()
                )
                + " §8| §7window=§f"
                + config
                    .getPerformanceRefreshWindowTicks()
                + "t §8| §7regular/tick=§f"
                + schedulerService
                    .getRecommendedRegularPerTick()
        );

        sender.sendMessage(
            "§7Cycle estimé: §f"
                + schedulerService
                    .getEstimatedCycleTicks()
                + "t §8| §7max regular=§f"
                + config
                    .getPerformanceMaxViewersPerTick()
        );

        sender.sendMessage(
            "§7Dirty queue: "
                + yn(
                    config.isPerformanceDirtyQueueEnabled()
                )
                + " §8| §7now=§f"
                + schedulerService
                    .getDirtyQueueSize()
                + " §8| §7peak=§f"
                + schedulerService
                    .getDirtyQueuePeak()
                + " §8| §7max/tick=§f"
                + config
                    .getPerformanceMaxDirtyPerTick()
        );

        sender.sendMessage(
            "§7Last tick: §f"
                + formatMillis(
                    metrics.getLastTickMillis()
                )
                + "ms §8| §7avg=§f"
                + formatMillis(
                    metrics.getAverageTickMillis()
                )
                + "ms §8| §7max=§f"
                + formatMillis(
                    metrics.getMaxTickMillis()
                )
                + "ms"
        );

        sender.sendMessage(
            "§7Viewer render: avg=§f"
                + formatMillis(
                    metrics.getAverageViewerMillis()
                )
                + "ms §8| §7max=§f"
                + formatMillis(
                    metrics.getMaxViewerMillis()
                )
                + "ms"
        );

        sender.sendMessage(
            "§7Last work: §edirty="
                + metrics
                    .getLastDirtyViewers()
                + " §8| §aregular="
                + metrics
                    .getLastRegularViewers()
        );

        sender.sendMessage(
            "§7Refresh totals: §f"
                + metrics
                    .getTotalViewerRefreshes()
                + " §8(§e"
                + metrics
                    .getTotalDirtyRefreshes()
                + " dirty§8 / §a"
                + metrics
                    .getTotalRegularRefreshes()
                + " regular§8)"
        );

        sender.sendMessage(
            "§7Virtual entry ops: §a+"
                + virtualTabService
                    .getTotalAdds()
                + " §e~"
                + virtualTabService
                    .getTotalUpdates()
                + " §c-"
                + virtualTabService
                    .getTotalRemoves()
        );

        sender.sendMessage(
            "§7Virtual network: §a+"
                + virtualTabService
                    .getTotalAddPackets()
                + " §e~"
                + virtualTabService
                    .getTotalUpdatePackets()
                + " §c-"
                + virtualTabService
                    .getTotalRemovePackets()
                + " §8| §7ops/packet=§f"
                + formatMillis(
                    virtualTabService
                        .getPacketCompressionRatio()
                )
        );

        sender.sendMessage(
            "§7Packet batching: "
                + yn(
                    config
                        .isPerformancePacketBatchingEnabled()
                )
                + " §8| §7max=§f"
                + config
                    .getPerformancePacketMaxEntriesPerPacket()
                + " §8| §7failures=§c"
                + virtualTabService
                    .getTotalPacketFailures()
                + " §8| §7retry entries=§e"
                + virtualTabService
                    .getTotalRetryEntries()
        );

        sender.sendMessage(
            "§7Header/Footer packets: §f"
                + tabService
                    .getTotalPackets()
                + " §8/ §7renders=§f"
                + tabService
                    .getTotalRefreshes()
        );

        sender.sendMessage(
            "§7Placeholder engine: compiled="
                + yn(
                    config
                        .isPerformancePlaceholderCompiledTemplates()
                )
                + " §8| §7cache="
                + yn(
                    config
                        .isPerformancePlaceholderCacheEnabled()
                )
                + " §8| §7templates=§f"
                + renderer
                    .getCompiledTemplateCount()
        );

        sender.sendMessage(
            "§7Placeholder PAPI: req=§f"
                + renderer
                    .getTotalPlaceholderRequests()
                + " §8| §7resolved=§f"
                + renderer
                    .getTotalPlaceholderResolved()
                + " §8| §7hits=§a"
                + renderer
                    .getTotalPlaceholderCacheHits()
                + " §8(§f"
                + formatPercent(
                    renderer
                        .getPlaceholderCacheHitRate()
                )
                + "%§8)"
        );

        sender.sendMessage(
            "§7Placeholder cache: snapshots=§f"
                + renderer
                    .getPlayerSnapshotCount()
                + " §8| §7values=§f"
                + renderer
                    .getCachedPlaceholderValueCount()
                + " §8| §7legacy calls=§f"
                + renderer
                    .getTotalLegacyPapiCalls()
        );

        sender.sendMessage(
            "§7Placeholder TTL: default=§f"
                + config
                    .getPerformancePlaceholderDefaultTtlTicks()
                + "t §8| §7max/player=§f"
                + config
                    .getPerformancePlaceholderMaxEntriesPerPlayer()
        );

        sender.sendMessage(
            "§7Visibility: targeted=§f"
                + visibilityController
                    .getTargetedHideOperations()
                + " §8| §7full=§f"
                + visibilityController
                    .getFullSweeps()
                + " §8| §7npc=§f"
                + visibilityController
                    .getNpcSweeps()
        );

        sender.sendMessage(
            "§7Visibility packets: §f"
                + visibilityController
                    .getPacketsSent()
                + " §8| §7profile entries=§f"
                + visibilityController
                    .getProfileEntriesSent()
        );

        sender.sendMessage(
            "§7Fallback visibility: §f"
                + config
                    .getPerformanceVisibilityFallbackScanTicks()
                + "t §8| §7ServerNPC scan=§f"
                + config
                    .getPerformanceServerNpcScanTicks()
                + "t"
        );

        sender.sendMessage(
            "§8----------------------------------------"
        );
    }

    private void sendValidation(
            CommandSender sender) {

        List<ValidationIssue> issues =
            new KtabValidator(
                config
            ).validate();

        int errors = 0;
        int warnings = 0;

        for (ValidationIssue issue : issues) {

            if (issue.isError()) {
                errors++;
            } else {
                warnings++;
            }
        }

        sender.sendMessage(
            "§8----------------------------------------"
        );

        sender.sendMessage(
            "§6§lKtab §7- Validation"
        );

        if (issues.isEmpty()) {

            sender.sendMessage(
                "§aConfiguration valide : aucune erreur ni alerte."
            );

        } else {

            sender.sendMessage(
                "§7Résultat: §c"
                    + errors
                    + " erreur(s) §8| §e"
                    + warnings
                    + " alerte(s)"
            );

            for (ValidationIssue issue : issues) {

                String prefix =
                    issue.isError()
                        ? "§cERREUR"
                        : "§eALERTE";

                sender.sendMessage(
                    prefix
                        + " §8[§7"
                        + issue.getPath()
                        + "§8] §f"
                        + issue.getMessage()
                );
            }
        }

        sender.sendMessage(
            "§8----------------------------------------"
        );
    }

    private void handleDump(
            CommandSender sender,
            String[] args) {

        Player viewer =
            resolveTarget(
                sender,
                args,
                1
            );

        if (viewer == null) {
            return;
        }

        int page =
            1;

        if (args.length >= 3) {

            try {

                page =
                    Math.max(
                        1,
                        Integer.parseInt(
                            args[2]
                        )
                    );

            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }

        LayoutRenderResult detailed =
            virtualTabService
                .previewDetailed(
                    viewer
                );

        List<RenderedVirtualCell> cells =
            detailed.getCells();

        final int pageSize =
            15;

        int pageCount =
            Math.max(
                1,
                (cells.size()
                    + pageSize
                    - 1)
                    / pageSize
            );

        page =
            Math.min(
                page,
                pageCount
            );

        int start =
            (page - 1)
                * pageSize;

        int end =
            Math.min(
                cells.size(),
                start + pageSize
            );

        sender.sendMessage(
            "§8----------------------------------------"
        );

        sender.sendMessage(
            "§6§lKtab §7- Dump §f"
                + viewer.getName()
                + " §8("
                + page
                + "/"
                + pageCount
                + ")"
        );

        for (int index = start;
                index < end;
                index++) {

            RenderedVirtualCell cell =
                cells.get(index);

            ResolvedTabSkin skin =
                virtualTabService
                    .resolveSkin(
                        viewer,
                        cell.getSkinId()
                    );

            sender.sendMessage(
                "§8#"
                    + pad2(index)
                    + " §7c"
                    + cell.getDisplayColumn()
                    + "/r"
                    + cell.getDisplayRow()
                    + " §8"
                    + cell.getColumnId()
                    + " §7skin=§f"
                    + emptyAsNone(
                        cell.getSkinId()
                    )
                    + " §8("
                    + skin.getSource()
                    + ")"
            );

            sender.sendMessage(
                "§8   §r"
                    + cell.getText()
            );
        }

        sender.sendMessage(
            "§7Entrées: §f"
                + cells.size()
                + " §8| §7page suivante: §e/ktab dump "
                + viewer.getName()
                + " "
                + Math.min(
                    pageCount,
                    page + 1
                )
        );

        if (!detailed.getDecisions()
                .isEmpty()) {

            sender.sendMessage(
                "§7Conditions:"
            );

            int shown =
                0;

            for (LayoutDecision decision
                    : detailed.getDecisions()) {

                if (shown >= 8) {

                    sender.sendMessage(
                        "§8  ... "
                            + (detailed.getDecisions()
                                .size() - shown)
                            + " décision(s) supplémentaire(s)"
                    );

                    break;
                }

                sender.sendMessage(
                    (decision.isVisible()
                        ? "§a✔ "
                        : "§c✘ ")
                        + "§7"
                        + decision.getPath()
                        + " §8- §f"
                        + decision.getReason()
                );

                shown++;
            }
        }

        sender.sendMessage(
            "§8----------------------------------------"
        );
    }

    private void handleSkin(
            CommandSender sender,
            String[] args) {

        String sub =
            args.length >= 2
                ? args[1].toLowerCase()
                : "list";

        if ("list".equals(sub)) {

            sendSkinList(sender);
            return;
        }

        if ("info".equals(sub)) {

            if (args.length < 3) {

                sender.sendMessage(
                    "§7Usage: §e/ktab skin info <skinId> [joueur]"
                );

                return;
            }

            Player viewer =
                resolveOptionalTarget(
                    sender,
                    args,
                    3
                );

            sendSkinInfo(
                sender,
                viewer,
                args[2]
            );

            return;
        }

        if ("test".equals(sub)) {

            if (args.length < 3) {

                sender.sendMessage(
                    "§7Usage: §e/ktab skin test <skinId> [joueur]"
                );

                return;
            }

            Player viewer =
                resolveTarget(
                    sender,
                    args,
                    3
                );

            if (viewer == null) {
                return;
            }

            ResolvedTabSkin skin =
                virtualTabService
                    .resolveSkin(
                        viewer,
                        args[2]
                    );

            virtualTabService
                .previewSkin(
                    viewer,
                    args[2],
                    SKIN_TEST_DURATION_TICKS
                );

            sender.sendMessage(
                "§aPreview skin §e"
                    + args[2]
                    + " §asur la première cellule de §e"
                    + viewer.getName()
                    + "§a pendant 10 secondes."
            );

            sender.sendMessage(
                "§7Résolution: source=§f"
                    + skin.getSource()
                    + " §7texture="
                    + yn(skin.hasTexture())
                    + " §7signed="
                    + yn(skin.hasSignature())
            );

            return;
        }

        if ("clear".equals(sub)) {

            Player viewer =
                resolveTarget(
                    sender,
                    args,
                    2
                );

            if (viewer == null) {
                return;
            }

            virtualTabService
                .clearSkinPreview(
                    viewer
                );

            sender.sendMessage(
                "§aPreview skin retirée pour §e"
                    + viewer.getName()
                    + "§a."
            );

            return;
        }

        sender.sendMessage(
            "§7Usage: §e/ktab skin <list|info|test|clear>"
        );
    }

    private void sendSkinList(
            CommandSender sender) {

        sender.sendMessage(
            "§8----------------------------------------"
        );

        sender.sendMessage(
            "§6§lKtab §7- Skins"
        );

        sender.sendMessage(
            "§7Built-ins: §fnone§7, §fviewer§7, §fplayer:<pseudo>"
        );

        if (config.getSkinIds()
                .isEmpty()) {

            sender.sendMessage(
                "§7Configurées: §8aucune"
            );

        } else {

            sender.sendMessage(
                "§7Configurées (§f"
                    + config.getSkinCount()
                    + "§7): §f"
                    + join(
                        config.getSkinIds()
                    )
            );
        }

        sender.sendMessage(
            "§7Preview actifs: §f"
                + virtualTabService
                    .getSkinPreviewCount()
        );

        sender.sendMessage(
            "§8----------------------------------------"
        );
    }

    private void sendSkinInfo(
            CommandSender sender,
            Player viewer,
            String skinId) {

        ResolvedTabSkin skin =
            virtualTabService
                .resolveSkin(
                    viewer,
                    skinId
                );

        sender.sendMessage(
            "§8----------------------------------------"
        );

        sender.sendMessage(
            "§6§lKtab §7- Skin §f"
                + skinId
        );

        sender.sendMessage(
            "§7Viewer: §f"
                + (viewer == null
                    ? "aucun"
                    : viewer.getName())
        );

        sender.sendMessage(
            "§7Source: §f"
                + skin.getSource()
        );

        sender.sendMessage(
            "§7Texture: "
                + yn(
                    skin.hasTexture()
                )
                + " §8| §7Base64 len=§f"
                + skin.getValueLength()
        );

        sender.sendMessage(
            "§7Signature Mojang: "
                + yn(
                    skin.hasSignature()
                )
        );

        sender.sendMessage(
            "§7Cache key: §8"
                + skin.getCacheKey()
        );

        if (!skin.hasTexture()
                && ("viewer".equalsIgnoreCase(
                        skinId)
                    || skinId
                        .toLowerCase()
                        .startsWith(
                            "player:"))) {

            sender.sendMessage(
                "§eCette skin dynamique nécessite un joueur en ligne avec une texture chargée."
            );
        }

        sender.sendMessage(
            "§8----------------------------------------"
        );
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
                    config.isEnabled()
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
                    config.isVirtualLayoutEnabled()
                )
                + " §8| §7"
                + config.getVirtualColumnsCount()
                + "x"
                + config.getVirtualRows()
        );

        sender.sendMessage(
            "§7Skins configurées: §f"
                + config.getSkinCount()
                + " §8| §7preview=§f"
                + virtualTabService
                    .getSkinPreviewCount()
        );

        sender.sendMessage(
            "§7Hide real players: "
                + yn(
                    config.isHideRealPlayers()
                )
                + " §8| §7last=§f"
                + visibilityController
                    .getLastHiddenRealPlayers()
        );

        sender.sendMessage(
            "§7Hide ServerNPC: "
                + yn(
                    config.isHideServerNpcs()
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
            "§7Performance V9: "
                + yn(
                    config.isPerformanceEnabled()
                )
                + " §8| §7wheel=§f"
                + schedulerService
                    .getWheelSize()
                + " §8| §7dirty=§f"
                + schedulerService
                    .getDirtyQueueSize()
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

        String skinPreview =
            virtualTabService
                .getSkinPreviewId(
                    target.getUniqueId()
                );

        sender.sendMessage(
            "§7Skin preview: §f"
                + (skinPreview.isEmpty()
                    ? "aucune"
                    : skinPreview)
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
                + renderer.render(
                    target,
                    "%kjob_display_job_name%",
                    true
                )
        );

        sender.sendMessage(
            "§8  global_level = §f"
                + renderer.render(
                    target,
                    "%kjob_global_level%",
                    true
                )
        );

        sender.sendMessage(
            "§8  claimable_quests = §f"
                + renderer.render(
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

        Player result =
            resolveOptionalTarget(
                sender,
                args,
                index
            );

        if (result != null) {
            return result;
        }

        if (args.length <= index
                && !(sender instanceof Player)) {

            sender.sendMessage(
                "§cDepuis la console, indique un joueur."
            );
        }

        return null;
    }

    private Player resolveOptionalTarget(
            CommandSender sender,
            String[] args,
            int index) {

        if (args.length > index) {

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

        return null;
    }

    private List<String> onlinePlayerNames() {

        List<String> names =
            new ArrayList<String>();

        for (Player player
                : Bukkit.getOnlinePlayers()) {

            names.add(
                player.getName()
            );
        }

        return names;
    }

    private List<String> skinIds() {

        List<String> ids =
            new ArrayList<String>();

        ids.add("none");
        ids.add("viewer");

        for (String id
                : config.getSkinIds()) {

            ids.add(id);
        }

        return ids;
    }

    private static List<String> complete(
            String token,
            Iterable<String> values) {

        String lower =
            token == null
                ? ""
                : token.toLowerCase();

        List<String> result =
            new ArrayList<String>();

        for (String value : values) {

            if (value != null
                    && value.toLowerCase()
                        .startsWith(lower)) {

                result.add(value);
            }
        }

        Collections.sort(result);
        return result;
    }

    private static String join(
            Iterable<String> values) {

        StringBuilder builder =
            new StringBuilder();

        for (String value : values) {

            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append(value);
        }

        return builder.toString();
    }

    private static String pad2(
            int value) {

        if (value < 10) {
            return "0"
                + value;
        }

        return String.valueOf(
            value
        );
    }

    private static String emptyAsNone(
            String value) {

        return value == null
                || value.trim()
                    .isEmpty()
            ? "none"
            : value;
    }

    private static String formatPercent(
            double value) {

        return String.format(
            java.util.Locale.US,
            "%.1f",
            value
        );
    }

    private static String formatMillis(
            double value) {

        return String.format(
            java.util.Locale.US,
            "%.3f",
            Math.max(
                0.0D,
                value
            )
        );
    }

    private static void sendUsage(
            CommandSender sender) {

        sender.sendMessage(
            "§7Usage: §e/ktab <status|reload|preview|debug|refresh|clear|validate|dump|perf|skin>"
        );
    }

    private static String yn(
            boolean value) {

        return value
            ? "§aON"
            : "§cOFF";
    }
}
