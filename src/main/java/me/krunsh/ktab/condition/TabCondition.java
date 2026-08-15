package me.krunsh.ktab.condition;

/**
 * Condition atomique utilisée par une colonne ou une cellule.
 *
 * input et value peuvent contenir des placeholders.
 */
public final class TabCondition {

    private final String type;
    private final String input;
    private final String value;
    private final boolean caseSensitive;

    public TabCondition(
            String type,
            String input,
            String value,
            boolean caseSensitive) {

        this.type =
            safe(type).toLowerCase();

        this.input =
            safe(input);

        this.value =
            safe(value);

        this.caseSensitive =
            caseSensitive;
    }

    public String getType() {
        return type;
    }

    public String getInput() {
        return input;
    }

    public String getValue() {
        return value;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    private static String safe(
            String value) {

        return value == null
            ? ""
            : value.trim();
    }
}
