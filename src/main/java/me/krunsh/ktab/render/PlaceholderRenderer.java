package me.krunsh.ktab.render;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;
import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.config.PlaceholderCacheRule;
import me.krunsh.ktab.snapshot.GlobalSnapshot;
import me.krunsh.ktab.snapshot.PlayerSnapshot;
import me.krunsh.ktab.snapshot.PlayerSnapshotCache;
import me.krunsh.ktab.template.CompiledTemplate;
import me.krunsh.ktab.template.TemplateCompiler;

/**
 * Renderer texte central de Ktab.
 *
 * V9.2 :
 * - templates compilés ;
 * - résolution PAPI par placeholder unique ;
 * - cache TTL configurable par placeholder ;
 * - snapshot global par tick ;
 * - snapshot/cache borné par viewer ;
 * - réflexion du ping mise en cache.
 *
 * Bukkit / PlaceholderAPI restent sur le main thread.
 */
public final class PlaceholderRenderer {

    private final KtabConfig config;

    private final TemplateCompiler templateCompiler =
        new TemplateCompiler();

    private final PlayerSnapshotCache playerSnapshots =
        new PlayerSnapshotCache();

    private GlobalSnapshot globalSnapshot =
        new GlobalSnapshot(
            0,
            0,
            0L
        );

    private long globalRevision;

    private Method cachedGetHandleMethod;
    private Field cachedPingField;
    private Class<?> cachedPlayerClass;
    private boolean pingAccessorInitialized;

    private long totalPlaceholderRequests;
    private long totalPlaceholderResolved;
    private long totalPlaceholderCacheHits;
    private long totalLegacyPapiCalls;

    public PlaceholderRenderer(
            KtabConfig config) {

        if (config == null) {
            throw new IllegalArgumentException(
                "KtabConfig manquante."
            );
        }

        this.config =
            config;

        applyConfigLimits();
    }

    public void beginTick(
            int onlinePlayers,
            int maxPlayers) {

        if (globalSnapshot.getOnlinePlayers()
                != onlinePlayers
                || globalSnapshot.getMaxPlayers()
                    != maxPlayers
                || globalSnapshot.getRevision() == 0L) {

            globalRevision++;
        }

        globalSnapshot =
            new GlobalSnapshot(
                onlinePlayers,
                maxPlayers,
                globalRevision
            );
    }

    public void refreshGlobalSnapshot() {

        beginTick(
            Bukkit.getOnlinePlayers()
                .size(),
            Bukkit.getMaxPlayers()
        );
    }

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

        String raw =
            input == null
                ? ""
                : input;

        if (globalSnapshot.getRevision() == 0L) {
            refreshGlobalSnapshot();
        }

        if (!config
                .isPerformancePlaceholderCompiledTemplates()
                || !config
                    .isPerformancePlaceholderDeduplicate()) {

            return legacyRender(
                player,
                raw,
                usePlaceholderApi
            );
        }

        CompiledTemplate template =
            templateCompiler.compile(
                raw
            );

        if (!template.hasPlaceholders()) {

            return ChatColor
                .translateAlternateColorCodes(
                    '&',
                    raw
                );
        }

        Map<String, String> localResolved =
            new HashMap<String, String>();

        StringBuilder builder =
            new StringBuilder(
                raw.length() + 16
            );

        for (CompiledTemplate.Part part
                : template.getParts()) {

            if (!part.isPlaceholder()) {

                builder.append(
                    part.getValue()
                );

                continue;
            }

            String token =
                part.getValue();

            String value =
                localResolved.get(
                    token
                );

            if (value == null) {

                value =
                    resolveToken(
                        player,
                        token,
                        usePlaceholderApi
                    );

                localResolved.put(
                    token,
                    value
                );
            }

            builder.append(
                value
            );
        }

        return ChatColor
            .translateAlternateColorCodes(
                '&',
                builder.toString()
            );
    }

    public void invalidatePlayer(
            UUID playerId) {

        playerSnapshots.invalidate(
            playerId
        );
    }

    public void clearCaches() {

        templateCompiler.clear();
        playerSnapshots.clear();

        applyConfigLimits();

        refreshGlobalSnapshot();
    }

    public void resetMetrics() {

        totalPlaceholderRequests = 0L;
        totalPlaceholderResolved = 0L;
        totalPlaceholderCacheHits = 0L;
        totalLegacyPapiCalls = 0L;
    }

    public int getCompiledTemplateCount() {
        return templateCompiler.size();
    }

    public int getPlayerSnapshotCount() {
        return playerSnapshots.size();
    }

    public int getCachedPlaceholderValueCount() {
        return playerSnapshots.cachedValueCount();
    }

    public long getTotalPlaceholderRequests() {
        return totalPlaceholderRequests;
    }

    public long getTotalPlaceholderResolved() {
        return totalPlaceholderResolved;
    }

    public long getTotalPlaceholderCacheHits() {
        return totalPlaceholderCacheHits;
    }

    public long getTotalLegacyPapiCalls() {
        return totalLegacyPapiCalls;
    }

    public double getPlaceholderCacheHitRate() {

        long cacheable =
            totalPlaceholderResolved
                + totalPlaceholderCacheHits;

        if (cacheable <= 0L) {
            return 0.0D;
        }

        return totalPlaceholderCacheHits
            * 100.0D
            / cacheable;
    }

    public GlobalSnapshot getGlobalSnapshot() {
        return globalSnapshot;
    }

    public List<String> getPlaceholderTokens(
            String raw) {

        CompiledTemplate template =
            templateCompiler.compile(
                raw == null
                    ? ""
                    : raw
            );

        java.util.ArrayList<String> tokens =
            new java.util.ArrayList<String>();

        for (CompiledTemplate.Part part
                : template.getParts()) {

            if (part.isPlaceholder()
                    && !tokens.contains(
                        part.getValue()
                    )) {

                tokens.add(
                    part.getValue()
                );
            }
        }

        return tokens;
    }

    public long getConfiguredTtlTicks(
            String token) {

        return resolveTtlTicks(
            token == null
                ? ""
                : token
        );
    }

    private String resolveToken(
            Player player,
            String token,
            boolean usePlaceholderApi) {

        totalPlaceholderRequests++;

        if ("%server_online%".equalsIgnoreCase(
                token)) {

            return String.valueOf(
                globalSnapshot
                    .getOnlinePlayers()
            );
        }

        if ("%server_max_players%".equalsIgnoreCase(
                token)) {

            return String.valueOf(
                globalSnapshot
                    .getMaxPlayers()
            );
        }

        if ("%player_name%".equalsIgnoreCase(
                token)) {

            return player == null
                ? token
                : player.getName();
        }

        if ("%player_ping%".equalsIgnoreCase(
                token)) {

            if (player == null) {
                return token;
            }

            return resolveCachedPlayerValue(
                player,
                token,
                new ValueResolver() {
                    @Override
                    public String resolve(
                            Player target,
                            String ignored) {

                        return String.valueOf(
                            resolvePing(
                                target
                            )
                        );
                    }
                }
            );
        }

        if (!usePlaceholderApi
                || player == null) {

            return token;
        }

        return resolveCachedPlayerValue(
            player,
            token,
            new ValueResolver() {
                @Override
                public String resolve(
                        Player target,
                        String placeholder) {

                    return PlaceholderAPI
                        .setPlaceholders(
                            target,
                            placeholder
                        );
                }
            }
        );
    }

    private String resolveCachedPlayerValue(
            Player player,
            String token,
            ValueResolver resolver) {

        long ttlTicks =
            resolveTtlTicks(
                token
            );

        boolean cacheEnabled =
            config
                .isPerformancePlaceholderCacheEnabled()
                && ttlTicks > 0L;

        if (!cacheEnabled) {

            totalPlaceholderResolved++;

            return safe(
                resolver.resolve(
                    player,
                    token
                )
            );
        }

        PlayerSnapshot snapshot =
            playerSnapshots
                .getOrCreate(
                    player.getUniqueId()
                );

        long now =
            System.currentTimeMillis();

        PlayerSnapshot.CachedValue cached =
            snapshot.get(
                token,
                now
            );

        if (cached != null) {

            totalPlaceholderCacheHits++;

            return cached.getValue();
        }

        String value =
            safe(
                resolver.resolve(
                    player,
                    token
                )
            );

        totalPlaceholderResolved++;

        snapshot.put(
            token,
            value,
            ticksToMillis(
                ttlTicks
            ),
            config
                .getPerformancePlaceholderMaxEntriesPerPlayer()
        );

        return value;
    }

    private long resolveTtlTicks(
            String token) {

        for (PlaceholderCacheRule rule
                : config
                    .getPerformancePlaceholderCacheRules()) {

            if (rule != null
                    && rule.matches(
                        token
                    )) {

                return rule
                    .getTtlTicks();
            }
        }

        return config
            .getPerformancePlaceholderDefaultTtlTicks();
    }

    private String legacyRender(
            Player player,
            String input,
            boolean usePlaceholderApi) {

        String output =
            input;

        output =
            output.replace(
                "%server_online%",
                String.valueOf(
                    globalSnapshot
                        .getOnlinePlayers()
                )
            );

        output =
            output.replace(
                "%server_max_players%",
                String.valueOf(
                    globalSnapshot
                        .getMaxPlayers()
                )
            );

        if (player != null) {

            output =
                output.replace(
                    "%player_name%",
                    player.getName()
                );

            if (output.contains(
                    "%player_ping%")) {

                output =
                    output.replace(
                        "%player_ping%",
                        String.valueOf(
                            resolvePing(
                                player
                            )
                        )
                    );
            }
        }

        if (usePlaceholderApi
                && player != null
                && output.indexOf('%') >= 0) {

            totalLegacyPapiCalls++;

            output =
                PlaceholderAPI
                    .setPlaceholders(
                        player,
                        output
                    );
        }

        return ChatColor
            .translateAlternateColorCodes(
                '&',
                output
            );
    }

    /**
     * Bukkit 1.8 ne fournit pas Player#getPing().
     */
    private int resolvePing(
            Player player) {

        if (player == null) {
            return 0;
        }

        try {

            ensurePingAccessor(
                player
            );

            if (cachedGetHandleMethod == null
                    || cachedPingField == null) {

                return 0;
            }

            Object handle =
                cachedGetHandleMethod.invoke(
                    player
                );

            return Math.max(
                0,
                cachedPingField.getInt(
                    handle
                )
            );

        } catch (Exception ignored) {

            cachedGetHandleMethod = null;
            cachedPingField = null;
            cachedPlayerClass = null;
            pingAccessorInitialized = false;

            return 0;
        }
    }

    private void ensurePingAccessor(
            Player player)
            throws Exception {

        Class<?> playerClass =
            player.getClass();

        if (pingAccessorInitialized
                && cachedPlayerClass != null
                && cachedPlayerClass
                    .isAssignableFrom(
                        playerClass
                    )
                && cachedGetHandleMethod != null
                && cachedPingField != null) {

            return;
        }

        Method getHandle =
            playerClass.getMethod(
                "getHandle"
            );

        Object handle =
            getHandle.invoke(
                player
            );

        if (handle == null) {
            return;
        }

        Field ping =
            handle.getClass()
                .getField(
                    "ping"
                );

        getHandle.setAccessible(
            true
        );

        ping.setAccessible(
            true
        );

        cachedPlayerClass =
            playerClass;

        cachedGetHandleMethod =
            getHandle;

        cachedPingField =
            ping;

        pingAccessorInitialized =
            true;
    }

    private void applyConfigLimits() {

        templateCompiler
            .setMaxEntries(
                config
                    .getPerformancePlaceholderMaxCompiledTemplates()
            );
    }

    private static long ticksToMillis(
            long ticks) {

        if (ticks <= 0L) {
            return 0L;
        }

        if (ticks
                > Long.MAX_VALUE / 50L) {

            return Long.MAX_VALUE;
        }

        return ticks * 50L;
    }

    private static String safe(
            String value) {

        return value == null
            ? ""
            : value;
    }

    private interface ValueResolver {

        String resolve(
            Player player,
            String token);
    }
}
