package me.krunsh.ktab.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.condition.TabCondition;
import me.krunsh.ktab.condition.TabConditionGroup;
import me.krunsh.ktab.layout.TabCell;
import me.krunsh.ktab.layout.TabColumn;
import me.krunsh.ktab.skin.TabSkinDefinition;

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

    private String virtualDefaultSkinId;
    private String virtualBlankSkinId;

    private List<TabColumn> virtualColumns =
        Collections.emptyList();

    private Map<String, TabSkinDefinition> skins =
        Collections.emptyMap();

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

        virtualDefaultSkinId =
            normalizeSkinId(
                config.getString(
                    "virtual_layout.default_skin",
                    "none"
                )
            );

        virtualBlankSkinId =
            normalizeSkinId(
                config.getString(
                    "virtual_layout.render.blank_skin",
                    virtualDefaultSkinId
                )
            );

        skins =
            loadSkins(
                config
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

    public String getVirtualDefaultSkinId() {
        return virtualDefaultSkinId;
    }

    public String getVirtualBlankSkinId() {
        return virtualBlankSkinId;
    }

    public List<TabColumn> getVirtualColumns() {
        return virtualColumns;
    }

    public TabSkinDefinition getSkinDefinition(
            String rawId) {

        String id =
            normalizeSkinId(
                rawId
            );

        if (id.isEmpty()) {
            return null;
        }

        return skins.get(id);
    }

    public Set<String> getSkinIds() {
        return skins.keySet();
    }

    public int getSkinCount() {
        return skins.size();
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

    private Map<String, TabSkinDefinition> loadSkins(
            FileConfiguration config) {

        ConfigurationSection section =
            config.getConfigurationSection(
                "skins"
            );

        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, TabSkinDefinition> result =
            new LinkedHashMap<String, TabSkinDefinition>();

        for (String rawId
                : section.getKeys(false)) {

            ConfigurationSection skin =
                section.getConfigurationSection(
                    rawId
                );

            if (skin == null) {
                continue;
            }

            String id =
                normalizeSkinId(
                    rawId
                );

            if (id.isEmpty()) {
                continue;
            }

            result.put(
                id,
                new TabSkinDefinition(
                    id,
                    skin.getBoolean(
                        "enabled",
                        true
                    ),
                    skin.getString(
                        "value",
                        ""
                    ),
                    skin.getString(
                        "signature",
                        ""
                    ),
                    skin.getString(
                        "texture_hash",
                        ""
                    ),
                    skin.getString(
                        "texture_url",
                        ""
                    ),
                    skin.getString(
                        "cache_key",
                        ""
                    )
                )
            );
        }

        return Collections.unmodifiableMap(
            result
        );
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

            String defaultSkin =
                normalizeSkinId(
                    section.getString(
                        "skin",
                        virtualDefaultSkinId
                    )
                );

            String titleText =
                section.getString(
                    "title",
                    ""
                );

            String titleSkin =
                normalizeSkinId(
                    section.getString(
                        "title_skin",
                        defaultSkin
                    )
                );

            int titleRow =
                section.getInt(
                    "title_row",
                    1
                );

            List<TabCell> lines =
                loadCells(
                    section.getList(
                        "lines"
                    ),
                    defaultSkin
                );

            TabConditionGroup conditions =
                loadConditionGroup(
                    section.get(
                        "when"
                    )
                );

            result.add(
                new TabColumn(
                    id,
                    defaultSkin,
                    new TabCell(
                        titleText,
                        titleSkin,
                        titleRow
                    ),
                    lines,
                    conditions
                )
            );
        }

        return Collections.unmodifiableList(
            result
        );
    }

    private List<TabCell> loadCells(
            List<?> rawLines,
            String defaultSkin) {

        if (rawLines == null
                || rawLines.isEmpty()) {

            return Collections.emptyList();
        }

        List<TabCell> result =
            new ArrayList<TabCell>();

        for (Object raw : rawLines) {

            if (raw == null) {

                result.add(
                    new TabCell(
                        "",
                        defaultSkin
                    )
                );

                continue;
            }

            if (raw instanceof String) {

                result.add(
                    new TabCell(
                        String.valueOf(raw),
                        defaultSkin
                    )
                );

                continue;
            }

            if (raw instanceof Map<?, ?>) {

                Map<?, ?> map =
                    (Map<?, ?>) raw;

                Object enabledValue =
                    map.get(
                        "enabled"
                    );

                if (enabledValue != null
                        && !Boolean.parseBoolean(
                            String.valueOf(
                                enabledValue
                            )
                        )) {

                    continue;
                }

                Object textValue =
                    map.get(
                        "text"
                    );

                Object skinValue =
                    map.get(
                        "skin"
                    );

                Object rowValue =
                    map.get(
                        "row"
                    );

                int configuredRow =
                    parseRow(
                        rowValue
                    );

                TabConditionGroup conditions =
                    loadConditionGroup(
                        map.get(
                            "when"
                        )
                    );

                result.add(
                    new TabCell(
                        textValue == null
                            ? ""
                            : String.valueOf(
                                textValue
                            ),
                        skinValue == null
                            ? defaultSkin
                            : normalizeSkinId(
                                String.valueOf(
                                    skinValue
                                )
                            ),
                        configuredRow,
                        conditions
                    )
                );

                continue;
            }

            result.add(
                new TabCell(
                    String.valueOf(raw),
                    defaultSkin
                )
            );
        }

        return Collections.unmodifiableList(
            result
        );
    }

    private TabConditionGroup loadConditionGroup(
            Object raw) {

        if (raw == null) {
            return TabConditionGroup.ALWAYS;
        }

        if (raw instanceof ConfigurationSection) {

            ConfigurationSection section =
                (ConfigurationSection) raw;

            String mode =
                section.getString(
                    "mode",
                    "all"
                );

            List<TabCondition> conditions =
                loadConditions(
                    section.getList(
                        "conditions"
                    )
                );

            if (conditions.isEmpty()
                    && section.contains(
                        "type"
                    )) {

                TabCondition single =
                    loadCondition(
                        section
                    );

                if (single != null) {
                    conditions.add(single);
                }
            }

            return new TabConditionGroup(
                parseConditionMode(
                    mode
                ),
                conditions
            );
        }

        if (raw instanceof Map<?, ?>) {

            Map<?, ?> map =
                (Map<?, ?>) raw;

            Object modeValue =
                map.get(
                    "mode"
                );

            String mode =
                modeValue == null
                    ? "all"
                    : String.valueOf(
                        modeValue
                    );

            List<TabCondition> conditions =
                loadConditionsObject(
                    map.get(
                        "conditions"
                    )
                );

            if (conditions.isEmpty()
                    && map.containsKey(
                        "type"
                    )) {

                TabCondition single =
                    loadCondition(
                        map
                    );

                if (single != null) {
                    conditions.add(single);
                }
            }

            return new TabConditionGroup(
                parseConditionMode(
                    mode
                ),
                conditions
            );
        }

        return TabConditionGroup.ALWAYS;
    }

    private List<TabCondition> loadConditions(
            List<?> rawConditions) {

        return loadConditionsObject(
            rawConditions
        );
    }

    private List<TabCondition> loadConditionsObject(
            Object rawConditions) {

        List<TabCondition> result =
            new ArrayList<TabCondition>();

        if (!(rawConditions
                instanceof Iterable<?>)) {

            return result;
        }

        for (Object raw
                : (Iterable<?>) rawConditions) {

            TabCondition condition =
                loadCondition(
                    raw
                );

            if (condition != null) {
                result.add(condition);
            }
        }

        return result;
    }

    private TabCondition loadCondition(
            Object raw) {

        if (raw instanceof ConfigurationSection) {

            ConfigurationSection section =
                (ConfigurationSection) raw;

            return new TabCondition(
                section.getString(
                    "type",
                    ""
                ),
                section.getString(
                    "input",
                    ""
                ),
                section.getString(
                    "value",
                    ""
                ),
                section.getBoolean(
                    "case_sensitive",
                    false
                )
            );
        }

        if (raw instanceof Map<?, ?>) {

            Map<?, ?> map =
                (Map<?, ?>) raw;

            return new TabCondition(
                mapString(
                    map,
                    "type"
                ),
                mapString(
                    map,
                    "input"
                ),
                mapString(
                    map,
                    "value"
                ),
                mapBoolean(
                    map,
                    "case_sensitive",
                    false
                )
            );
        }

        return null;
    }

    private static TabConditionGroup.Mode parseConditionMode(
            String raw) {

        if (raw != null
                && "any".equalsIgnoreCase(
                    raw.trim()
                )) {

            return TabConditionGroup.Mode.ANY;
        }

        return TabConditionGroup.Mode.ALL;
    }

    private static String mapString(
            Map<?, ?> map,
            String key) {

        Object value =
            map.get(key);

        return value == null
            ? ""
            : String.valueOf(value);
    }

    private static boolean mapBoolean(
            Map<?, ?> map,
            String key,
            boolean fallback) {

        Object value =
            map.get(key);

        if (value == null) {
            return fallback;
        }

        return Boolean.parseBoolean(
            String.valueOf(value)
        );
    }

    private static int parseRow(
            Object raw) {

        if (raw == null) {
            return 0;
        }

        if (raw instanceof Number) {
            return ((Number) raw)
                .intValue();
        }

        try {

            return Integer.parseInt(
                String.valueOf(raw)
                    .trim()
            );

        } catch (NumberFormatException ignored) {
            return 0;
        }
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

    private static String normalizeSkinId(
            String value) {

        return value == null
            ? ""
            : value.trim()
                .toLowerCase(
                    Locale.ROOT
                );
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
