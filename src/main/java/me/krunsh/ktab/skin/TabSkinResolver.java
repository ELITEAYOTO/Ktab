package me.krunsh.ktab.skin;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.config.KtabConfig;

/**
 * Résout les skin_id du layout.
 *
 * IDs spéciaux :
 * - none
 * - viewer / player
 * - player:<pseudo> pour copier la skin d'un joueur actuellement en ligne
 *
 * Les skins configurées hash/url/base64 sont mises en cache afin de ne pas
 * reconstruire leur payload Base64 à chaque cellule et à chaque cycle.
 */
public final class TabSkinResolver {

    private final KtabPlugin plugin;
    private final KtabConfig config;

    private final Map<String, CachedConfiguredSkin> configuredCache =
        new HashMap<String, CachedConfiguredSkin>();

    public TabSkinResolver(
            KtabPlugin plugin,
            KtabConfig config) {

        if (plugin == null
                || config == null) {

            throw new IllegalArgumentException(
                "Dépendance TabSkinResolver manquante."
            );
        }

        this.plugin = plugin;
        this.config = config;
    }

    public void clearCache() {
        configuredCache.clear();
    }

    public ResolvedTabSkin resolve(
            Player viewer,
            String rawSkinId) {

        String skinId =
            normalize(
                rawSkinId
            );

        if (skinId.isEmpty()
                || "none".equals(skinId)
                || "default_minecraft".equals(skinId)) {

            return new ResolvedTabSkin(
                "",
                "",
                "none",
                skinId.isEmpty()
                    ? "none"
                    : skinId,
                "none"
            );
        }

        if ("viewer".equals(skinId)
                || "player".equals(skinId)) {

            return resolvePlayerSkin(
                viewer,
                skinId,
                "viewer"
            );
        }

        if (skinId.startsWith(
                "player:")) {

            String playerName =
                rawSkinId == null
                    ? ""
                    : rawSkinId.substring(
                        rawSkinId.indexOf(':') + 1
                    ).trim();

            Player target =
                Bukkit.getPlayerExact(
                    playerName
                );

            if (target == null
                    || !target.isOnline()) {

                return empty(
                    skinId,
                    "player-offline",
                    "missing:"
                        + skinId
                );
            }

            return resolvePlayerSkin(
                target,
                skinId,
                "player:"
                    + target.getName()
            );
        }

        TabSkinDefinition definition =
            config.getSkinDefinition(
                skinId
            );

        if (definition == null) {

            return empty(
                skinId,
                "missing",
                "missing:"
                    + skinId
            );
        }

        if (!definition.isEnabled()) {

            return empty(
                skinId,
                "disabled",
                "disabled:"
                    + skinId
            );
        }

        String fingerprint =
            fingerprint(
                definition
            );

        CachedConfiguredSkin cached =
            configuredCache.get(
                skinId
            );

        if (cached != null
                && fingerprint.equals(
                    cached.fingerprint
                )) {

            return cached.skin;
        }

        ResolvedTabSkin resolved =
            resolveConfigured(
                skinId,
                definition
            );

        configuredCache.put(
            skinId,
            new CachedConfiguredSkin(
                fingerprint,
                resolved
            )
        );

        return resolved;
    }

    private ResolvedTabSkin resolveConfigured(
            String skinId,
            TabSkinDefinition definition) {

        String value =
            definition.getValue();

        String source =
            "configured-value";

        if (value.isEmpty()) {

            String url =
                definition.getTextureUrl();

            if (url.isEmpty()
                    && !definition
                        .getTextureHash()
                        .isEmpty()) {

                url =
                    "http://textures.minecraft.net/texture/"
                        + definition
                            .getTextureHash();

                source =
                    "configured-hash";

            } else if (!url.isEmpty()) {

                source =
                    "configured-url";
            }

            if (!url.isEmpty()) {

                value =
                    encodeTextureUrl(
                        url
                    );
            }
        }

        if (value.isEmpty()) {

            return empty(
                skinId,
                "configured-empty",
                "empty:"
                    + skinId
            );
        }

        String configuredKey =
            definition.getCacheKey();

        if (configuredKey.isEmpty()) {
            configuredKey = "auto";
        }

        /*
         * Le hash de texture fait toujours partie de la clé finale.
         * Même si l'admin oublie d'incrémenter cache_key, changer la texture
         * provoquera donc automatiquement REMOVE + ADD côté client.
         */
        String cacheKey =
            "cfg:"
                + skinId
                + ":"
                + configuredKey
                + ":"
                + Integer.toHexString(
                    value.hashCode()
                )
                + ":"
                + Integer.toHexString(
                    definition
                        .getSignature()
                        .hashCode()
                );

        return new ResolvedTabSkin(
            value,
            definition.getSignature(),
            cacheKey,
            skinId,
            source
        );
    }

    private ResolvedTabSkin resolvePlayerSkin(
            Player target,
            String requestedId,
            String source) {

        if (target == null
                || !target.isOnline()) {

            return empty(
                requestedId,
                "player-unavailable",
                "missing:"
                    + requestedId
            );
        }

        try {

            Object profile =
                findGameProfile(
                    target
                );

            if (profile == null) {

                return empty(
                    requestedId,
                    "player-profile-null",
                    "missing:"
                        + requestedId
                );
            }

            Method getProperties =
                profile.getClass()
                    .getMethod(
                        "getProperties"
                    );

            Object properties =
                getProperties.invoke(
                    profile
                );

            if (properties == null) {

                return empty(
                    requestedId,
                    "player-properties-null",
                    "missing:"
                        + requestedId
                );
            }

            Object textures =
                invokePropertyGet(
                    properties,
                    "textures"
                );

            if (!(textures instanceof Collection<?>)) {

                return empty(
                    requestedId,
                    "player-textures-missing",
                    "missing:"
                        + requestedId
                );
            }

            for (Object property
                    : (Collection<?>) textures) {

                if (property == null) {
                    continue;
                }

                String value =
                    invokeString(
                        property,
                        "getValue"
                    );

                if (value.isEmpty()) {
                    continue;
                }

                String signature =
                    invokeString(
                        property,
                        "getSignature"
                    );

                return new ResolvedTabSkin(
                    value,
                    signature,
                    source
                        + ":"
                        + target.getUniqueId()
                        + ":"
                        + Integer.toHexString(
                            value.hashCode()
                        ),
                    requestedId,
                    source
                );
            }

        } catch (Exception failure) {

            if (plugin.getConfig()
                    .getBoolean(
                        "debug",
                        false
                    )) {

                plugin.getLogger()
                    .warning(
                        "Skin joueur introuvable pour "
                            + target.getName()
                            + ": "
                            + failure.getClass()
                                .getSimpleName()
                            + " "
                            + failure.getMessage()
                    );
            }
        }

        return empty(
            requestedId,
            "player-texture-empty",
            "missing:"
                + requestedId
        );
    }

    private Object findGameProfile(
            Player target)
            throws Exception {

        try {

            Method getProfile =
                target.getClass()
                    .getMethod(
                        "getProfile"
                    );

            return getProfile.invoke(
                target
            );

        } catch (NoSuchMethodException ignored) {
        }

        Method getHandle =
            target.getClass()
                .getMethod(
                    "getHandle"
                );

        Object handle =
            getHandle.invoke(
                target
            );

        if (handle == null) {
            return null;
        }

        Method getProfile =
            handle.getClass()
                .getMethod(
                    "getProfile"
                );

        return getProfile.invoke(
            handle
        );
    }

    private Object invokePropertyGet(
            Object properties,
            String key)
            throws Exception {

        for (Method method
                : properties.getClass()
                    .getMethods()) {

            if (!"get".equals(
                    method.getName())
                    || method
                        .getParameterTypes()
                        .length != 1) {

                continue;
            }

            return method.invoke(
                properties,
                key
            );
        }

        return null;
    }

    private static String invokeString(
            Object target,
            String methodName) {

        try {

            Method method =
                target.getClass()
                    .getMethod(
                        methodName
                    );

            Object value =
                method.invoke(
                    target
                );

            return value == null
                ? ""
                : String.valueOf(
                    value
                );

        } catch (Exception ignored) {
            return "";
        }
    }

    private static ResolvedTabSkin empty(
            String requestedId,
            String source,
            String cacheKey) {

        return new ResolvedTabSkin(
            "",
            "",
            cacheKey,
            requestedId,
            source
        );
    }

    private static String fingerprint(
            TabSkinDefinition definition) {

        return definition.getId()
            + "|"
            + definition.isEnabled()
            + "|"
            + definition.getValue()
            + "|"
            + definition.getSignature()
            + "|"
            + definition.getTextureHash()
            + "|"
            + definition.getTextureUrl()
            + "|"
            + definition.getCacheKey();
    }

    private static String encodeTextureUrl(
            String url) {

        String json =
            "{\"textures\":{\"SKIN\":{\"url\":\""
                + escapeJson(url)
                + "\"}}}";

        return Base64
            .getEncoder()
            .encodeToString(
                json.getBytes(
                    StandardCharsets.UTF_8
                )
            );
    }

    private static String escapeJson(
            String value) {

        return value
            .replace(
                "\\",
                "\\\\"
            )
            .replace(
                "\"",
                "\\\""
            );
    }

    private static String normalize(
            String value) {

        return value == null
            ? ""
            : value.trim()
                .toLowerCase(
                    Locale.ROOT
                );
    }

    private static final class CachedConfiguredSkin {

        private final String fingerprint;
        private final ResolvedTabSkin skin;

        private CachedConfiguredSkin(
                String fingerprint,
                ResolvedTabSkin skin) {

            this.fingerprint =
                fingerprint;

            this.skin =
                skin;
        }
    }
}
