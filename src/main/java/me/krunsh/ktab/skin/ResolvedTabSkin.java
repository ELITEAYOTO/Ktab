package me.krunsh.ktab.skin;

/**
 * Texture prête à être injectée dans un GameProfile fake.
 */
public final class ResolvedTabSkin {

    public static final ResolvedTabSkin NONE =
        new ResolvedTabSkin(
            "",
            "",
            "none"
        );

    private final String value;
    private final String signature;
    private final String cacheKey;

    public ResolvedTabSkin(
            String value,
            String signature,
            String cacheKey) {

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
