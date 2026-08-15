package me.krunsh.ktab.integration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import me.krunsh.ktab.KtabPlugin;
import me.krunsh.ktab.logging.KtabConsole;
import me.krunsh.ktab.visibility.TabProfile;

/**
 * Hook optionnel ServerNPC.
 *
 * V9.4 :
 * - aucune dépendance compile-time ;
 * - méthodes NPC getUuid/getName mises en cache par classe ;
 * - warnings throttlés pour éviter le spam console.
 */
public final class ServerNpcHook {

    private static final String PLUGIN_NAME =
        "ServerNPC";

    private final KtabPlugin plugin;

    private boolean available;

    private Method getApiMethod;
    private Method getNpcListMethod;

    private Class<?> cachedNpcClass;
    private Method cachedGetUuidMethod;
    private Method cachedGetNameMethod;

    private long reflectionResolves;
    private long readFailures;
    private long lastFailureLogMillis;

    public ServerNpcHook(
            KtabPlugin plugin) {

        if (plugin == null) {
            throw new IllegalArgumentException(
                "KtabPlugin manquant."
            );
        }

        this.plugin =
            plugin;

        refresh();
    }

    public void refresh() {

        available =
            false;

        getApiMethod =
            null;

        getNpcListMethod =
            null;

        cachedNpcClass =
            null;

        cachedGetUuidMethod =
            null;

        cachedGetNameMethod =
            null;

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
                getApiMethod.invoke(
                    null
                );

            if (api == null) {
                return;
            }

            getNpcListMethod =
                api.getClass()
                    .getMethod(
                        "getNPCList"
                    );

            available =
                true;

            KtabConsole.success(
                plugin,
                "Hook ServerNPC actif."
            );

        } catch (Exception failure) {

            logFailure(
                "ServerNPC détecté mais API inaccessible",
                failure
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
                getApiMethod.invoke(
                    null
                );

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

                ensureNpcAccessors(
                    npc
                );

                Object rawUuid =
                    cachedGetUuidMethod.invoke(
                        npc
                    );

                if (!(rawUuid instanceof UUID)) {
                    continue;
                }

                Object rawName =
                    cachedGetNameMethod.invoke(
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

            readFailures++;

            logFailure(
                "Lecture ServerNPC impossible",
                failure
            );

            return Collections.emptyList();
        }
    }

    public long getReflectionResolves() {
        return reflectionResolves;
    }

    public long getReadFailures() {
        return readFailures;
    }

    public void resetMetrics() {

        reflectionResolves =
            0L;

        readFailures =
            0L;
    }

    private void ensureNpcAccessors(
            Object npc)
            throws Exception {

        Class<?> npcClass =
            npc.getClass();

        if (cachedNpcClass == npcClass
                && cachedGetUuidMethod != null
                && cachedGetNameMethod != null) {

            return;
        }

        cachedNpcClass =
            npcClass;

        cachedGetUuidMethod =
            npcClass.getMethod(
                "getUuid"
            );

        cachedGetNameMethod =
            npcClass.getMethod(
                "getName"
            );

        reflectionResolves++;
    }

    private void logFailure(
            String prefix,
            Exception failure) {

        long now =
            System.currentTimeMillis();

        if (now - lastFailureLogMillis
                < 5000L) {

            return;
        }

        lastFailureLogMillis =
            now;

        KtabConsole.warning(
            plugin,
            prefix
                + ": "
                + failure.getClass()
                    .getSimpleName()
                + " "
                + failure.getMessage()
        );
    }
}
