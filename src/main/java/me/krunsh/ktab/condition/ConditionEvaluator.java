package me.krunsh.ktab.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.krunsh.ktab.config.KtabConfig;
import me.krunsh.ktab.render.PlaceholderRenderer;

/**
 * Évalue les conditions runtime.
 *
 * Types supportés :
 * permission, not_permission,
 * equals, not_equals,
 * contains, not_contains,
 * starts_with, ends_with,
 * empty, not_empty,
 * online_min, online_max.
 */
public final class ConditionEvaluator {

    private final KtabConfig config;
    private final PlaceholderRenderer renderer;

    public ConditionEvaluator(
            KtabConfig config,
            PlaceholderRenderer renderer) {

        if (config == null
                || renderer == null) {

            throw new IllegalArgumentException(
                "Dépendance ConditionEvaluator manquante."
            );
        }

        this.config = config;
        this.renderer = renderer;
    }

    public boolean matches(
            Player viewer,
            TabConditionGroup group) {

        return evaluate(
            viewer,
            group
        ).isMatched();
    }

    public ConditionEvaluation evaluate(
            Player viewer,
            TabConditionGroup group) {

        if (group == null
                || group.isEmpty()) {

            return new ConditionEvaluation(
                true,
                new ArrayList<String>()
            );
        }

        List<String> details =
            new ArrayList<String>();

        boolean result =
            group.getMode()
                == TabConditionGroup.Mode.ALL;

        for (TabCondition condition
                : group.getConditions()) {

            boolean matched =
                evaluateCondition(
                    viewer,
                    condition
                );

            details.add(
                (matched ? "OK " : "KO ")
                    + describe(
                        viewer,
                        condition
                    )
            );

            if (group.getMode()
                    == TabConditionGroup.Mode.ALL) {

                result =
                    result && matched;

            } else {

                result =
                    result || matched;
            }
        }

        return new ConditionEvaluation(
            result,
            details
        );
    }

    private boolean evaluateCondition(
            Player viewer,
            TabCondition condition) {

        if (condition == null) {
            return true;
        }

        String type =
            condition.getType();

        if ("permission".equals(type)) {

            return viewer != null
                && viewer.hasPermission(
                    render(
                        viewer,
                        condition.getValue()
                    )
                );
        }

        if ("not_permission".equals(type)) {

            return viewer == null
                || !viewer.hasPermission(
                    render(
                        viewer,
                        condition.getValue()
                    )
                );
        }

        String input =
            render(
                viewer,
                condition.getInput()
            );

        String value =
            render(
                viewer,
                condition.getValue()
            );

        if ("empty".equals(type)) {
            return input.trim().isEmpty();
        }

        if ("not_empty".equals(type)) {
            return !input.trim().isEmpty();
        }

        if ("online_min".equals(type)) {

            Integer expected =
                parseInt(value);

            return expected != null
                && Bukkit.getOnlinePlayers()
                    .size() >= expected.intValue();
        }

        if ("online_max".equals(type)) {

            Integer expected =
                parseInt(value);

            return expected != null
                && Bukkit.getOnlinePlayers()
                    .size() <= expected.intValue();
        }

        String comparableInput =
            condition.isCaseSensitive()
                ? input
                : input.toLowerCase(
                    Locale.ROOT
                );

        String comparableValue =
            condition.isCaseSensitive()
                ? value
                : value.toLowerCase(
                    Locale.ROOT
                );

        if ("equals".equals(type)) {

            return comparableInput.equals(
                comparableValue
            );
        }

        if ("not_equals".equals(type)) {

            return !comparableInput.equals(
                comparableValue
            );
        }

        if ("contains".equals(type)) {

            return comparableInput.contains(
                comparableValue
            );
        }

        if ("not_contains".equals(type)) {

            return !comparableInput.contains(
                comparableValue
            );
        }

        if ("starts_with".equals(type)) {

            return comparableInput.startsWith(
                comparableValue
            );
        }

        if ("ends_with".equals(type)) {

            return comparableInput.endsWith(
                comparableValue
            );
        }

        return false;
    }

    private String describe(
            Player viewer,
            TabCondition condition) {

        if (condition == null) {
            return "condition-null";
        }

        String type =
            condition.getType();

        if ("permission".equals(type)
                || "not_permission".equals(type)) {

            return type
                + "("
                + render(
                    viewer,
                    condition.getValue()
                )
                + ")";
        }

        if ("empty".equals(type)
                || "not_empty".equals(type)) {

            return type
                + "('"
                + compact(
                    render(
                        viewer,
                        condition.getInput()
                    )
                )
                + "')";
        }

        return type
            + "('"
            + compact(
                render(
                    viewer,
                    condition.getInput()
                )
            )
            + "','"
            + compact(
                render(
                    viewer,
                    condition.getValue()
                )
            )
            + "')";
    }

    private String render(
            Player viewer,
            String value) {

        return renderer.render(
            viewer,
            value,
            config.isPlaceholderApiEnabled()
        );
    }

    private static Integer parseInt(
            String value) {

        try {

            return Integer.valueOf(
                Integer.parseInt(
                    value.trim()
                )
            );

        } catch (Exception ignored) {
            return null;
        }
    }

    private static String compact(
            String value) {

        if (value == null) {
            return "";
        }

        String compact =
            value.replace(
                '\n',
                ' '
            );

        if (compact.length() > 40) {

            return compact.substring(
                0,
                37
            ) + "...";
        }

        return compact;
    }
}
