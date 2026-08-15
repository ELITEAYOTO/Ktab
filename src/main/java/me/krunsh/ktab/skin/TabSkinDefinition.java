package me.krunsh.ktab.skin;

/**
 * Définition immuable d'une skin configurée.
 *
 * Une texture peut être fournie sous quatre formes :
 * - value Base64 Mojang ;
 * - value + signature ;
 * - texture_hash ;
 * - texture_url.
 */
public final class TabSkinDefinition {

    private final String id;
    private final boolean enabled;

    private final String value;
    private final String signature;

    private final String textureHash;
    private final String textureUrl;

    private final String cacheKey;

    public TabSkinDefinition(
            String id,
            boolean enabled,
            String value,
            String signature,
            String textureHash,
            String textureUrl,
            String cacheKey) {

        this.id =
            safe(id);

        this.enabled =
            enabled;

        this.value =
            safe(value);

        this.signature =
            safe(signature);

        this.textureHash =
            safe(textureHash);

        this.textureUrl =
            safe(textureUrl);

        this.cacheKey =
            safe(cacheKey);
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }

    public String getTextureHash() {
        return textureHash;
    }

    public String getTextureUrl() {
        return textureUrl;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    private static String safe(
            String value) {

        return value == null
            ? ""
            : value.trim();
    }
}
