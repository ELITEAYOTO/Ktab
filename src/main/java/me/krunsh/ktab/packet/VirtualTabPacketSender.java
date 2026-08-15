package me.krunsh.ktab.packet;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Couche packet des entrées virtuelles du TAB 1.8.8.
 *
 * Aucun import NMS compile-time.
 */
public final class VirtualTabPacketSender {

    private final String nmsVersion;

    private final Class<?> packetClass;
    private final Class<?> actionClass;
    private final Class<?> dataClass;
    private final Class<?> gameProfileClass;
    private final Class<?> gamemodeClass;
    private final Class<?> packetInterface;

    private final Field actionField;
    private final Field listField;

    private final Method chatParseMethod;

    private final Class<?> craftPlayerClass;
    private final Method getHandleMethod;

    public VirtualTabPacketSender() {

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

            actionField.setAccessible(true);

            listField =
                findListField(
                    packetClass,
                    "b"
                );

            listField.setAccessible(true);

            Class<?> serializerClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".IChatBaseComponent$ChatSerializer"
                );

            chatParseMethod =
                serializerClass.getMethod(
                    "a",
                    String.class
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
                "Initialisation VirtualTabPacketSender impossible.",
                failure
            );
        }
    }

    public String getNmsVersion() {
        return nmsVersion;
    }

    public void add(
            Player viewer,
            VirtualEntry entry) {

        send(
            viewer,
            "ADD_PLAYER",
            entry,
            entry.getDisplayName()
        );
    }

    public void update(
            Player viewer,
            VirtualEntry entry) {

        send(
            viewer,
            "UPDATE_DISPLAY_NAME",
            entry,
            entry.getDisplayName()
        );
    }

    public void remove(
            Player viewer,
            VirtualEntry entry) {

        send(
            viewer,
            "REMOVE_PLAYER",
            entry,
            null
        );
    }

    private void send(
            Player viewer,
            String actionName,
            VirtualEntry entry,
            String displayName) {

        if (viewer == null
                || !viewer.isOnline()
                || entry == null) {

            return;
        }

        try {

            Object packet =
                createPacket(
                    actionName,
                    entry,
                    displayName
                );

            sendPacket(
                viewer,
                packet
            );

        } catch (Exception failure) {

            throw new IllegalStateException(
                "Packet "
                    + actionName
                    + " impossible pour "
                    + viewer.getName(),
                failure
            );
        }
    }

    @SuppressWarnings({
        "unchecked",
        "rawtypes"
    })
    private Object createPacket(
            String actionName,
            VirtualEntry entry,
            String displayName)
            throws Exception {

        Constructor<?> packetConstructor =
            packetClass.getDeclaredConstructor();

        packetConstructor.setAccessible(true);

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

        data.add(
            createPlayerInfoData(
                packet,
                entry,
                displayName
            )
        );

        return packet;
    }

    @SuppressWarnings({
        "unchecked",
        "rawtypes"
    })
    private Object createPlayerInfoData(
            Object packet,
            VirtualEntry entry,
            String displayName)
            throws Exception {

        Object profile =
            gameProfileClass
                .getConstructor(
                    UUID.class,
                    String.class
                )
                .newInstance(
                    entry.getUuid(),
                    entry.getTechnicalName()
                );

        Object gamemode =
            Enum.valueOf(
                (Class<Enum>)
                    gamemodeClass.asSubclass(
                        Enum.class
                    ),
                "SURVIVAL"
            );

        Object component =
            displayName == null
                ? null
                : chatComponent(
                    displayName
                );

        for (Constructor<?> constructor
                : dataClass
                    .getDeclaredConstructors()) {

            Class<?>[] types =
                constructor.getParameterTypes();

            constructor.setAccessible(true);

            if (types.length == 5) {

                return constructor.newInstance(
                    packet,
                    profile,
                    Integer.valueOf(0),
                    gamemode,
                    component
                );
            }

            if (types.length == 4) {

                return constructor.newInstance(
                    profile,
                    Integer.valueOf(0),
                    gamemode,
                    component
                );
            }
        }

        throw new NoSuchMethodException(
            "Constructeur PlayerInfoData introuvable."
        );
    }

    private Object chatComponent(
            String text)
            throws Exception {

        return chatParseMethod.invoke(
            null,
            "{\"text\":\""
                + escapeJson(
                    text == null
                        ? ""
                        : text
                )
                + "\"}"
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

        Field connectionField =
            handle.getClass()
                .getField(
                    "playerConnection"
                );

        Object connection =
            connectionField.get(
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

    private static String escapeJson(
            String text) {

        StringBuilder builder =
            new StringBuilder(
                text.length() + 16
            );

        for (int i = 0;
                i < text.length();
                i++) {

            char current =
                text.charAt(i);

            switch (current) {

                case '\\':
                    builder.append("\\\\");
                    break;

                case '"':
                    builder.append("\\\"");
                    break;

                case '\n':
                    builder.append("\\n");
                    break;

                case '\r':
                    builder.append("\\r");
                    break;

                case '\t':
                    builder.append("\\t");
                    break;

                default:
                    builder.append(current);
                    break;
            }
        }

        return builder.toString();
    }
}
