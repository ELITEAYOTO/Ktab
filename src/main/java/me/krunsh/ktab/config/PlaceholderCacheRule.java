package me.krunsh.ktab.config;

/**
 * Règle de TTL pour un placeholder.
 *
 * Le pattern accepte '*' comme wildcard simple.
 */
public final class PlaceholderCacheRule {

    private final String match;
    private final long ttlTicks;

    public PlaceholderCacheRule(
            String match,
            long ttlTicks) {

        this.match =
            match == null
                ? ""
                : match.trim();

        this.ttlTicks =
            Math.max(
                0L,
                ttlTicks
            );
    }

    public String getMatch() {
        return match;
    }

    public long getTtlTicks() {
        return ttlTicks;
    }

    public boolean matches(
            String placeholder) {

        if (placeholder == null
                || match.isEmpty()) {

            return false;
        }

        if ("*".equals(match)) {
            return true;
        }

        int wildcard =
            match.indexOf('*');

        if (wildcard < 0) {
            return match.equalsIgnoreCase(
                placeholder
            );
        }

        String prefix =
            match.substring(
                0,
                wildcard
            );

        String suffix =
            match.substring(
                wildcard + 1
            );

        if (!prefix.isEmpty()
                && !placeholder
                    .regionMatches(
                        true,
                        0,
                        prefix,
                        0,
                        prefix.length()
                    )) {

            return false;
        }

        if (!suffix.isEmpty()
                && !endsWithIgnoreCase(
                    placeholder,
                    suffix
                )) {

            return false;
        }

        return placeholder.length()
            >= prefix.length()
                + suffix.length();
    }

    private static boolean endsWithIgnoreCase(
            String value,
            String suffix) {

        if (value.length()
                < suffix.length()) {

            return false;
        }

        return value.regionMatches(
            true,
            value.length()
                - suffix.length(),
            suffix,
            0,
            suffix.length()
        );
    }
}
