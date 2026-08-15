package me.krunsh.ktab.validation;

/**
 * Problème détecté par /ktab validate.
 */
public final class ValidationIssue {

    public enum Severity {
        ERROR,
        WARNING
    }

    private final Severity severity;
    private final String path;
    private final String message;

    public ValidationIssue(
            Severity severity,
            String path,
            String message) {

        this.severity =
            severity == null
                ? Severity.WARNING
                : severity;

        this.path =
            path == null
                ? ""
                : path;

        this.message =
            message == null
                ? ""
                : message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }
}
