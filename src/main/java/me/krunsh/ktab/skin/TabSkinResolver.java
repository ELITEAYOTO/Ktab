package me.krunsh.ktab.skin;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Locale;

import org.bukkit.entity.Player;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.config.KtabConfig;

/**
 * Résout un skin_id du layout vers une texture GameProfile.
 *
 * IDs spéciaux :
 * - none    : aucune texture custom ;
 * - viewer  : skin du joueur qui regarde le TAB ;
 * - player  : alias de viewer.
 */
public final class TabSkinResolver {

    private final KtabPlugin plugin;
    private final KtabConfig config;

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

            return ResolvedTabSkin.NONE;
        }

        if ("viewer".equals(skinId)
                || "player".equals(skinId)) {

            return resolveViewerSkin(
                viewer
            );
        }

        TabSkinDefinition definition =
            config.getSkinDefinition(
                skinId
            );

        if (definition == null
                || !definition.isEnabled()) {

            return ResolvedTabSkin.NONE;
        }

        String value =
            definition.getValue();

        String source =
            "value";

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
                    "hash";

            } else if (!url.isEmpty()) {

                source =
                    "url";
            }

            if (!url.isEmpty()) {

                value =
                    encodeTextureUrl(
                        url
                    );
            }
        }

        if (value.isEmpty()) {
            return ResolvedTabSkin.NONE;
        }

        String cacheKey =
            definition.getCacheKey();

        if (cacheKey.isEmpty()) {

            cacheKey =
                "cfg:"
                    + skinId
                    + ":"
                    + source
                    + ":"
                    + Integer.toHexString(
                        value.hashCode()
                    );
        }

        return new ResolvedTabSkin(
            value,
            definition.getSignature(),
            cacheKey
        );
    }

    private ResolvedTabSkin resolveViewerSkin(
            Player viewer) {

        if (viewer == null) {
            return ResolvedTabSkin.NONE;
        }

        try {

            Object profile =
                findGameProfile(
                    viewer
                );

            if (profile == null) {
                return ResolvedTabSkin.NONE;
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
                return ResolvedTabSkin.NONE;
            }

            Object textures =
                invokePropertyGet(
                    properties,
                    "textures"
                );

            if (!(textures instanceof Collection<?>)) {
                return ResolvedTabSkin.NONE;
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
                    "viewer:"
                        + viewer.getUniqueId()
                        + ":"
                        + Integer.toHexString(
                            value.hashCode()
                        )
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
                        "Skin viewer introuvable pour "
                            + viewer.getName()
                            + ": "
                            + failure.getClass()
                                .getSimpleName()
                            + " "
                            + failure.getMessage()
                    );
            }
        }

        return ResolvedTabSkin.NONE;
    }

    private Object findGameProfile(
            Player viewer)
            throws Exception {

        try {

            Method getProfile =
                viewer.getClass()
                    .getMethod(
                        "getProfile"
                    );

            return getProfile.invoke(
                viewer
            );

        } catch (NoSuchMethodException ignored) {
        }

        Method getHandle =
            viewer.getClass()
                .getMethod(
                    "getHandle"
                );

        Object handle =
            getHandle.invoke(
                viewer
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
}
