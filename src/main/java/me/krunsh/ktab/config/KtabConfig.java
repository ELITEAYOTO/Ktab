package me.krunsh.ktab.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.layout.TabColumn;

/**
 * Snapshot de configuration Ktab.
 */
public final class KtabConfig {

    private final KtabPlugin plugin;

    private boolean enabled;
    private long updateIntervalTicks;
    private boolean placeholderApiEnabled;

    private List<String> headerLines =
        Collections.emptyList();

    private List<String> footerLines =
        Collections.emptyList();

    private boolean playerListNameEnabled;
    private String playerListNameFormat;
    private int playerListNameMaxLength;

    private boolean virtualLayoutEnabled;
    private long virtualUpdateIntervalTicks;
    private int virtualColumnsCount;
    private int virtualRows;
    private int virtualMaxEntries;
    private int virtualReservedRealEntries;
    private boolean virtualForceClientRows;

    private String virtualTechnicalPrefix;
    private String virtualUuidSeed;
    private int virtualStartIndex;

    private String virtualBlankText;
    private String virtualCellPrefix;
    private String virtualCellSuffix;

    private List<TabColumn> virtualColumns =
        Collections.emptyList();

    private boolean hideRealPlayers;
    private boolean hideServerNpcs;
    private long visibilityInitialDelayTicks;

    public KtabConfig(
            KtabPlugin plugin) {

        if (plugin == null) {
            throw new IllegalArgumentException(
                "KtabPlugin ne peut pas être null."
            );
        }

        this.plugin = plugin;
    }

    public void reload() {

        plugin.reloadConfig();

        FileConfiguration config =
            plugin.getConfig();

        enabled =
            config.getBoolean(
                "enabled",
                true
            );

        updateIntervalTicks =
            Math.max(
                20L,
                config.getLong(
                    "update_interval_ticks",
                    40L
                )
            );

        placeholderApiEnabled =
            config.getBoolean(
                "placeholderapi",
                true
            );

        headerLines =
            immutableCopy(
                config.getStringList(
                    "header"
                )
            );

        footerLines =
            immutableCopy(
                config.getStringList(
                    "footer"
                )
            );

        playerListNameEnabled =
            config.getBoolean(
                "player_list_name.enabled",
                false
            );

        playerListNameFormat =
            safe(
                config.getString(
                    "player_list_name.format",
                    "&7%player_name%"
                ),
                "&7%player_name%"
            );

        playerListNameMaxLength =
            Math.max(
                1,
                Math.min(
                    16,
                    config.getInt(
                        "player_list_name.max_length",
                        16
                    )
                )
            );

        virtualLayoutEnabled =
            config.getBoolean(
                "virtual_layout.enabled",
                false
            );

        virtualUpdateIntervalTicks =
            Math.max(
                20L,
                config.getLong(
                    "virtual_layout.update_interval_ticks",
                    40L
                )
            );

        virtualColumnsCount =
            Math.max(
                1,
                Math.min(
                    4,
                    config.getInt(
                        "virtual_layout.columns_count",
                        3
                    )
                )
            );

        virtualRows =
            Math.max(
                1,
                Math.min(
                    20,
                    config.getInt(
                        "virtual_layout.rows",
                        15
                    )
                )
            );

        virtualMaxEntries =
            Math.max(
                1,
                Math.min(
                    80,
                    config.getInt(
                        "virtual_layout.max_entries",
                        44
                    )
                )
            );

        virtualReservedRealEntries =
            Math.max(
                0,
                config.getInt(
                    "virtual_layout.reserve_real_entries",
                    1
                )
            );

        virtualForceClientRows =
            config.getBoolean(
                "virtual_layout.force_client_rows",
                true
            );

        virtualTechnicalPrefix =
            sanitizeTechnicalPrefix(
                config.getString(
                    "virtual_layout.ordering.technical_name_prefix",
                    "!kt_"
                )
            );

        virtualUuidSeed =
            safe(
                config.getString(
                    "virtual_layout.ordering.stable_uuid_seed",
                    "volkaria-ktab"
                ),
                "volkaria-ktab"
            );

        virtualStartIndex =
            Math.max(
                0,
                config.getInt(
                    "virtual_layout.ordering.start_index",
                    1
                )
            );

        virtualBlankText =
            safe(
                config.getString(
                    "virtual_layout.render.blank_text",
                    "&8"
                ),
                "&8"
            );

        virtualCellPrefix =
            safe(
                config.getString(
                    "virtual_layout.render.cell_prefix",
                    ""
                ),
                ""
            );

        virtualCellSuffix =
            safe(
                config.getString(
                    "virtual_layout.render.cell_suffix",
                    ""
                ),
                ""
            );

        virtualColumns =
            loadColumns(
                config
            );

        hideRealPlayers =
            config.getBoolean(
                "visibility.hide_real_players",
                true
            );

        hideServerNpcs =
            config.getBoolean(
                "visibility.hide_servernpc",
                true
            );

        visibilityInitialDelayTicks =
            Math.max(
                1L,
                config.getLong(
                    "visibility.initial_hide_delay_ticks",
                    20L
                )
            );
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getUpdateIntervalTicks() {
        return updateIntervalTicks;
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    public List<String> getHeaderLines() {
        return headerLines;
    }

    public List<String> getFooterLines() {
        return footerLines;
    }

    public boolean isPlayerListNameEnabled() {
        return playerListNameEnabled;
    }

    public String getPlayerListNameFormat() {
        return playerListNameFormat;
    }

    public int getPlayerListNameMaxLength() {
        return playerListNameMaxLength;
    }

    public boolean isVirtualLayoutEnabled() {
        return virtualLayoutEnabled;
    }

    public long getVirtualUpdateIntervalTicks() {
        return virtualUpdateIntervalTicks;
    }

    public int getVirtualColumnsCount() {
        return virtualColumnsCount;
    }

    public int getVirtualRows() {
        return virtualRows;
    }

    public int getVirtualMaxEntries() {
        return virtualMaxEntries;
    }

    public int getVirtualReservedRealEntries() {
        return virtualReservedRealEntries;
    }

    public boolean isVirtualForceClientRows() {
        return virtualForceClientRows;
    }

    public String getVirtualTechnicalPrefix() {
        return virtualTechnicalPrefix;
    }

    public String getVirtualUuidSeed() {
        return virtualUuidSeed;
    }

    public int getVirtualStartIndex() {
        return virtualStartIndex;
    }

    public String getVirtualBlankText() {
        return virtualBlankText;
    }

    public String getVirtualCellPrefix() {
        return virtualCellPrefix;
    }

    public String getVirtualCellSuffix() {
        return virtualCellSuffix;
    }

    public List<TabColumn> getVirtualColumns() {
        return virtualColumns;
    }

    public boolean isHideRealPlayers() {
        return hideRealPlayers;
    }

    public boolean isHideServerNpcs() {
        return hideServerNpcs;
    }

    public long getVisibilityInitialDelayTicks() {
        return visibilityInitialDelayTicks;
    }

    private List<TabColumn> loadColumns(
            FileConfiguration config) {

        ConfigurationSection parent =
            config.getConfigurationSection(
                "virtual_layout.columns"
            );

        if (parent == null) {
            return Collections.emptyList();
        }

        List<TabColumn> result =
            new ArrayList<TabColumn>();

        for (String id
                : parent.getKeys(false)) {

            ConfigurationSection section =
                parent.getConfigurationSection(
                    id
                );

            if (section == null
                    || !section.getBoolean(
                        "enabled",
                        true
                    )) {

                continue;
            }

            result.add(
                new TabColumn(
                    id,
                    section.getString(
                        "title",
                        ""
                    ),
                    section.getStringList(
                        "lines"
                    )
                )
            );
        }

        return Collections.unmodifiableList(
            result
        );
    }

    private static List<String> immutableCopy(
            List<String> input) {

        if (input == null
                || input.isEmpty()) {

            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
            new ArrayList<String>(
                input
            )
        );
    }

    private static String safe(
            String value,
            String fallback) {

        return value == null
            ? fallback
            : value;
    }

    private static String sanitizeTechnicalPrefix(
            String raw) {

        String value =
            raw == null
                ? "!kt_"
                : raw.trim();

        if (value.isEmpty()) {
            value = "!kt_";
        }

        value =
            value.replaceAll(
                "[^A-Za-z0-9_!\\-]",
                ""
            );

        if (value.isEmpty()) {
            value = "!kt_";
        }

        if (value.length() > 12) {

            value =
                value.substring(
                    0,
                    12
                );
        }

        return value;
    }
}
