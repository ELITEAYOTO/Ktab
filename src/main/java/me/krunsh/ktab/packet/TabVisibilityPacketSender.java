package me.krunsh.ktab.packet;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.krunsh.ktab.visibility.TabProfile;

/**
 * Packets PlayerInfo utilisés uniquement pour contrôler quelles vraies
 * entrées occupent le TAB client.
 *
 * REMOVE_PLAYER ne despawn pas les entités dans le monde.
 */
public final class TabVisibilityPacketSender {

    private final String nmsVersion;

    private final Class<?> packetClass;
    private final Class<?> actionClass;
    private final Class<?> dataClass;
    private final Class<?> gameProfileClass;
    private final Class<?> gamemodeClass;
    private final Class<?> packetInterface;

    private final Field actionField;
    private final Field listField;

    private final Class<?> craftPlayerClass;
    private final Method getHandleMethod;

    public TabVisibilityPacketSender() {

        try {

            String packageName =
                Bukkit.getServer()
                    .getClass()
                    .getPackage()
                    .getName();

            nmsVersion =
                packageName.substring(
                    packageName.lastIndexOf('.') + 1
                );

            packetClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".PacketPlayOutPlayerInfo"
                );

            actionClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".PacketPlayOutPlayerInfo$EnumPlayerInfoAction"
                );

            dataClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".PacketPlayOutPlayerInfo$PlayerInfoData"
                );

            gameProfileClass =
                Class.forName(
                    "com.mojang.authlib.GameProfile"
                );

            gamemodeClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".WorldSettings$EnumGamemode"
                );

            packetInterface =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".Packet"
                );

            actionField =
                findField(
                    packetClass,
                    actionClass,
                    "a"
                );

            actionField.setAccessible(
                true
            );

            listField =
                findListField(
                    packetClass,
                    "b"
                );

            listField.setAccessible(
                true
            );

            craftPlayerClass =
                Class.forName(
                    "org.bukkit.craftbukkit."
                        + nmsVersion
                        + ".entity.CraftPlayer"
                );

            getHandleMethod =
                craftPlayerClass.getMethod(
                    "getHandle"
                );

        } catch (Exception failure) {

            throw new IllegalStateException(
                "Initialisation TabVisibilityPacketSender impossible.",
                failure
            );
        }
    }

    public String getNmsVersion() {
        return nmsVersion;
    }

    /**
     * Retire en un seul packet une collection de profils du TAB d'un viewer.
     */
    public void removeProfiles(
            Player viewer,
            Collection<TabProfile> profiles) {

        if (viewer == null
                || !viewer.isOnline()
                || profiles == null
                || profiles.isEmpty()) {

            return;
        }

        try {

            Object packet =
                createProfilePacket(
                    "REMOVE_PLAYER",
                    profiles
                );

            sendPacket(
                viewer,
                packet
            );

        } catch (Exception failure) {

            throw new IllegalStateException(
                "Masquage PlayerInfo impossible pour "
                    + viewer.getName(),
                failure
            );
        }
    }

    /**
     * Réajoute les vrais joueurs via leurs vrais EntityPlayer.
     *
     * Utilisé lorsque Ktab est désactivé/reload avec hide_real_players=false.
     */
    public void addRealPlayers(
            Player viewer,
            Collection<? extends Player> players) {

        if (viewer == null
                || !viewer.isOnline()
                || players == null
                || players.isEmpty()) {

            return;
        }

        try {

            @SuppressWarnings({
                "unchecked",
                "rawtypes"
            })
            Object action =
                Enum.valueOf(
                    (Class<Enum>)
                        actionClass.asSubclass(
                            Enum.class
                        ),
                    "ADD_PLAYER"
                );

            List<Object> handles =
                new ArrayList<Object>();

            for (Player player : players) {

                if (player != null
                        && player.isOnline()) {

                    handles.add(
                        getHandleMethod.invoke(
                            player
                        )
                    );
                }
            }

            if (handles.isEmpty()) {
                return;
            }

            Object packet =
                null;

            for (Constructor<?> constructor
                    : packetClass
                        .getConstructors()) {

                Class<?>[] types =
                    constructor.getParameterTypes();

                if (types.length != 2
                        || !types[0]
                            .equals(
                                actionClass
                            )) {

                    continue;
                }

                if (types[1].isArray()) {

                    Object array =
                        Array.newInstance(
                            types[1]
                                .getComponentType(),
                            handles.size()
                        );

                    for (int i = 0;
                            i < handles.size();
                            i++) {

                        Array.set(
                            array,
                            i,
                            handles.get(i)
                        );
                    }

                    packet =
                        constructor.newInstance(
                            action,
                            array
                        );

                    break;
                }

                if (Iterable.class
                        .isAssignableFrom(
                            types[1]
                        )) {

                    packet =
                        constructor.newInstance(
                            action,
                            handles
                        );

                    break;
                }
            }

            if (packet == null) {

                throw new NoSuchMethodException(
                    "Constructeur ADD_PLAYER réel introuvable."
                );
            }

            sendPacket(
                viewer,
                packet
            );

        } catch (Exception failure) {

            throw new IllegalStateException(
                "Restauration PlayerInfo impossible pour "
                    + viewer.getName(),
                failure
            );
        }
    }

    @SuppressWarnings({
        "unchecked",
        "rawtypes"
    })
    private Object createProfilePacket(
            String actionName,
            Collection<TabProfile> profiles)
            throws Exception {

        Constructor<?> packetConstructor =
            packetClass.getDeclaredConstructor();

        packetConstructor.setAccessible(
            true
        );

        Object packet =
            packetConstructor.newInstance();

        Object action =
            Enum.valueOf(
                (Class<Enum>)
                    actionClass.asSubclass(
                        Enum.class
                    ),
                actionName
            );

        actionField.set(
            packet,
            action
        );

        @SuppressWarnings("unchecked")
        List<Object> data =
            (List<Object>)
                listField.get(packet);

        data.clear();

        for (TabProfile profile : profiles) {

            if (profile == null) {
                continue;
            }

            data.add(
                createPlayerInfoData(
                    packet,
                    profile
                )
            );
        }

        return packet;
    }

    @SuppressWarnings({
        "unchecked",
        "rawtypes"
    })
    private Object createPlayerInfoData(
            Object packet,
            TabProfile profile)
            throws Exception {

        Object gameProfile =
            gameProfileClass
                .getConstructor(
                    java.util.UUID.class,
                    String.class
                )
                .newInstance(
                    profile.getUuid(),
                    profile.getName()
                );

        Object gamemode =
            Enum.valueOf(
                (Class<Enum>)
                    gamemodeClass.asSubclass(
                        Enum.class
                    ),
                "SURVIVAL"
            );

        for (Constructor<?> constructor
                : dataClass
                    .getDeclaredConstructors()) {

            Class<?>[] types =
                constructor.getParameterTypes();

            constructor.setAccessible(
                true
            );

            if (types.length == 5) {

                return constructor.newInstance(
                    packet,
                    gameProfile,
                    Integer.valueOf(0),
                    gamemode,
                    null
                );
            }

            if (types.length == 4) {

                return constructor.newInstance(
                    gameProfile,
                    Integer.valueOf(0),
                    gamemode,
                    null
                );
            }
        }

        throw new NoSuchMethodException(
            "Constructeur PlayerInfoData introuvable."
        );
    }

    private void sendPacket(
            Player player,
            Object packet)
            throws Exception {

        Object handle =
            getHandleMethod.invoke(
                player
            );

        Object connection =
            handle.getClass()
                .getField(
                    "playerConnection"
                )
                .get(
                    handle
                );

        Method sendPacket =
            connection.getClass()
                .getMethod(
                    "sendPacket",
                    packetInterface
                );

        sendPacket.invoke(
            connection,
            packet
        );
    }

    private static Field findField(
            Class<?> owner,
            Class<?> expectedType,
            String fallbackName)
            throws NoSuchFieldException {

        try {

            Field field =
                owner.getDeclaredField(
                    fallbackName
                );

            if (expectedType
                    .isAssignableFrom(
                        field.getType()
                    )) {

                return field;
            }

        } catch (NoSuchFieldException ignored) {
        }

        for (Field field
                : owner.getDeclaredFields()) {

            if (expectedType
                    .isAssignableFrom(
                        field.getType()
                    )) {

                return field;
            }
        }

        throw new NoSuchFieldException(
            fallbackName
        );
    }

    private static Field findListField(
            Class<?> owner,
            String fallbackName)
            throws NoSuchFieldException {

        try {

            Field field =
                owner.getDeclaredField(
                    fallbackName
                );

            if (List.class
                    .isAssignableFrom(
                        field.getType()
                    )) {

                return field;
            }

        } catch (NoSuchFieldException ignored) {
        }

        for (Field field
                : owner.getDeclaredFields()) {

            if (List.class
                    .isAssignableFrom(
                        field.getType()
                    )) {

                return field;
            }
        }

        throw new NoSuchFieldException(
            fallbackName
        );
    }
}
