package me.krunsh.ktab.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Template texte pré-découpé en littéraux + placeholders.
 */
public final class CompiledTemplate {

    public static final class Part {

        private final String value;
        private final boolean placeholder;

        public Part(
                String value,
                boolean placeholder) {

            this.value =
                value == null
                    ? ""
                    : value;

            this.placeholder =
                placeholder;
        }

        public String getValue() {
            return value;
        }

        public boolean isPlaceholder() {
            return placeholder;
        }
    }

    private final String raw;
    private final List<Part> parts;
    private final boolean hasPlaceholders;

    public CompiledTemplate(
            String raw,
            List<Part> parts,
            boolean hasPlaceholders) {

        this.raw =
            raw == null
                ? ""
                : raw;

        this.parts =
            Collections.unmodifiableList(
                parts == null
                    ? new ArrayList<Part>()
                    : new ArrayList<Part>(
                        parts
                    )
            );

        this.hasPlaceholders =
            hasPlaceholders;
    }

    public String getRaw() {
        return raw;
    }

    public List<Part> getParts() {
        return parts;
    }

    public boolean hasPlaceholders() {
        return hasPlaceholders;
    }
}
