package me.krunsh.ktab.layout;

/**
 * Trace d'une décision conditionnelle du renderer.
 */
public final class LayoutDecision {

    private final String path;
    private final boolean visible;
    private final String reason;

    public LayoutDecision(
            String path,
            boolean visible,
            String reason) {

        this.path =
            path == null
                ? ""
                : path;

        this.visible =
            visible;

        this.reason =
            reason == null
                ? ""
                : reason;
    }

    public String getPath() {
        return path;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getReason() {
        return reason;
    }
}
