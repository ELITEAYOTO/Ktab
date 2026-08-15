package me.krunsh.ktab.template;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compile et met en cache les templates %placeholder%.
 *
 * Le cache est borné afin qu'une config ou une commande dynamique ne puisse
 * pas faire grossir la mémoire indéfiniment.
 */
public final class TemplateCompiler {

    private final Map<String, CompiledTemplate> cache =
        new LinkedHashMap<String, CompiledTemplate>(
            128,
            0.75F,
            true
        );

    private int maxEntries =
        512;

    public void setMaxEntries(
            int maxEntries) {

        this.maxEntries =
            Math.max(
                32,
                maxEntries
            );

        trim();
    }

    public CompiledTemplate compile(
            String input) {

        String raw =
            input == null
                ? ""
                : input;

        CompiledTemplate cached =
            cache.get(raw);

        if (cached != null) {
            return cached;
        }

        CompiledTemplate compiled =
            compileNew(raw);

        cache.put(
            raw,
            compiled
        );

        trim();

        return compiled;
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }

    private CompiledTemplate compileNew(
            String raw) {

        List<CompiledTemplate.Part> parts =
            new ArrayList<CompiledTemplate.Part>();

        int cursor =
            0;

        boolean hasPlaceholders =
            false;

        while (cursor < raw.length()) {

            int start =
                raw.indexOf(
                    '%',
                    cursor
                );

            if (start < 0) {

                parts.add(
                    new CompiledTemplate.Part(
                        raw.substring(
                            cursor
                        ),
                        false
                    )
                );

                break;
            }

            int end =
                raw.indexOf(
                    '%',
                    start + 1
                );

            if (end < 0) {

                parts.add(
                    new CompiledTemplate.Part(
                        raw.substring(
                            cursor
                        ),
                        false
                    )
                );

                break;
            }

            if (start > cursor) {

                parts.add(
                    new CompiledTemplate.Part(
                        raw.substring(
                            cursor,
                            start
                        ),
                        false
                    )
                );
            }

            String token =
                raw.substring(
                    start,
                    end + 1
                );

            if (token.length() <= 2) {

                parts.add(
                    new CompiledTemplate.Part(
                        token,
                        false
                    )
                );

            } else {

                parts.add(
                    new CompiledTemplate.Part(
                        token,
                        true
                    )
                );

                hasPlaceholders =
                    true;
            }

            cursor =
                end + 1;
        }

        if (raw.isEmpty()) {

            parts.add(
                new CompiledTemplate.Part(
                    "",
                    false
                )
            );
        }

        return new CompiledTemplate(
            raw,
            parts,
            hasPlaceholders
        );
    }

    private void trim() {

        while (cache.size()
                > maxEntries) {

            String eldest =
                cache.keySet()
                    .iterator()
                    .next();

            cache.remove(
                eldest
            );
        }
    }
}
