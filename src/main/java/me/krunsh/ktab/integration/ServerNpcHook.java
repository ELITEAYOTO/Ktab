package me.krunsh.ktab.integration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.visibility.TabProfile;

/**
 * Hook optionnel ServerNPC.
 *
 * Aucune dépendance compile-time vers ServerNPC :
 * Ktab utilise uniquement l'API publique disponible par réflexion.
 *
 * ServerNPC 1.13.11 expose :
 * ServerNPC.getAPI().getNPCList()
 * SnakeNPC#getUuid()
 * SnakeNPC#getName()
 */
public final class ServerNpcHook {

    private static final String PLUGIN_NAME =
        "ServerNPC";

    private final KtabPlugin plugin;

    private boolean available;

    private Method getApiMethod;
    private Method getNpcListMethod;

    public ServerNpcHook(
            KtabPlugin plugin) {

        if (plugin == null) {
            throw new IllegalArgumentException(
                "KtabPlugin manquant."
            );
        }

        this.plugin = plugin;

        refresh();
    }

    public void refresh() {

        available = false;

        getApiMethod = null;
        getNpcListMethod = null;

        Plugin serverNpc =
            Bukkit.getPluginManager()
                .getPlugin(
                    PLUGIN_NAME
                );

        if (serverNpc == null
                || !serverNpc.isEnabled()) {

            return;
        }

        try {

            Class<?> mainClass =
                Class.forName(
                    "com.isnakebuzz.servernpc.ServerNPC"
                );

            getApiMethod =
                mainClass.getMethod(
                    "getAPI"
                );

            Object api =
                getApiMethod.invoke(null);

            if (api == null) {
                return;
            }

            getNpcListMethod =
                api.getClass()
                    .getMethod(
                        "getNPCList"
                    );

            available = true;

            plugin.getLogger()
                .info(
                    "Hook ServerNPC actif."
                );

        } catch (Exception failure) {

            plugin.getLogger()
                .warning(
                    "ServerNPC détecté mais API inaccessible: "
                        + failure.getClass()
                            .getSimpleName()
                        + " "
                        + failure.getMessage()
                );
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public List<TabProfile> getNpcProfiles() {

        if (!available
                || getApiMethod == null
                || getNpcListMethod == null) {

            return Collections.emptyList();
        }

        try {

            Object api =
                getApiMethod.invoke(null);

            if (api == null) {
                return Collections.emptyList();
            }

            Object rawList =
                getNpcListMethod.invoke(
                    api
                );

            if (!(rawList instanceof Iterable<?>)) {
                return Collections.emptyList();
            }

            List<TabProfile> result =
                new ArrayList<TabProfile>();

            for (Object npc
                    : (Iterable<?>) rawList) {

                if (npc == null) {
                    continue;
                }

                Method getUuid =
                    npc.getClass()
                        .getMethod(
                            "getUuid"
                        );

                Method getName =
                    npc.getClass()
                        .getMethod(
                            "getName"
                        );

                Object rawUuid =
                    getUuid.invoke(
                        npc
                    );

                if (!(rawUuid instanceof UUID)) {
                    continue;
                }

                Object rawName =
                    getName.invoke(
                        npc
                    );

                result.add(
                    new TabProfile(
                        (UUID) rawUuid,
                        rawName == null
                            ? ""
                            : String.valueOf(
                                rawName
                            )
                    )
                );
            }

            return result;

        } catch (Exception failure) {

            plugin.getLogger()
                .warning(
                    "Lecture ServerNPC impossible: "
                        + failure.getClass()
                            .getSimpleName()
                        + " "
                        + failure.getMessage()
                );

            return Collections.emptyList();
        }
    }
}
