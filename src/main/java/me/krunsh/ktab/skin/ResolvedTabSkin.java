package me.krunsh.ktab.skin;

/**
 * Texture résolue et prête à être injectée dans un GameProfile fake.
 *
 * V6 conserve aussi les métadonnées de résolution afin que /ktab skin info
 * puisse expliquer précisément d'où vient une tête.
 */
public final class ResolvedTabSkin {

    public static final ResolvedTabSkin NONE =
        new ResolvedTabSkin(
            "",
            "",
            "none",
            "none",
            "none"
        );

    private final String value;
    private final String signature;
    private final String cacheKey;

    private final String requestedId;
    private final String source;

    public ResolvedTabSkin(
            String value,
            String signature,
            String cacheKey) {

        this(
            value,
            signature,
            cacheKey,
            "",
            "legacy"
        );
    }

    public ResolvedTabSkin(
            String value,
            String signature,
            String cacheKey,
            String requestedId,
            String source) {

        this.value =
            safe(value);

        this.signature =
            safe(signature);

        String safeCacheKey =
            safe(cacheKey);

        this.cacheKey =
            safeCacheKey.isEmpty()
                ? "none"
                : safeCacheKey;

        this.requestedId =
            safe(requestedId);

        this.source =
            safe(source).isEmpty()
                ? "unknown"
                : safe(source);
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public String getRequestedId() {
        return requestedId;
    }

    public String getSource() {
        return source;
    }

    public int getValueLength() {
        return value.length();
    }

    public boolean hasTexture() {
        return !value.isEmpty();
    }

    public boolean hasSignature() {
        return !signature.isEmpty();
    }

    private static String safe(
            String value) {

        return value == null
            ? ""
            : value.trim();
    }
}
