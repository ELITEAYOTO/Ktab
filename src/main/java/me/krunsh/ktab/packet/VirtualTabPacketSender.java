package me.krunsh.ktab.packet;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.krunsh.ktab.skin.ResolvedTabSkin;

/**
 * Couche packet des entrées virtuelles du TAB 1.8.8.
 *
 * V9.3 :
 * - ADD / UPDATE / REMOVE batchables ;
 * - découpage configurable par nombre maximal d'entrées/packet ;
 * - constructeurs/champs/méthodes NMS mis en cache ;
 * - sélection du constructeur PlayerInfoData par types et non uniquement
 *   par nombre de paramètres.
 */
public final class VirtualTabPacketSender {

    private final String nmsVersion;

    private final Class<?> packetClass;
    private final Class<?> actionClass;
    private final Class<?> dataClass;
    private final Class<?> gameProfileClass;
    private final Class<?> propertyClass;
    private final Class<?> gamemodeClass;
    private final Class<?> chatComponentClass;
    private final Class<?> packetInterface;

    private final Constructor<?> packetConstructor;
    private final Constructor<?> gameProfileConstructor;
    private final Constructor<?> propertyConstructor;
    private final Constructor<?> signedPropertyConstructor;
    private final Constructor<?> playerInfoDataConstructor;
    private final boolean playerInfoDataNeedsOuterPacket;

    private final Field actionField;
    private final Field listField;
    private final Field playerConnectionField;

    private final Method chatParseMethod;
    private final Method getHandleMethod;
    private final Method sendPacketMethod;
    private final Method gameProfileGetPropertiesMethod;

    private final Object survivalGamemode;

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

            propertyClass =
                Class.forName(
                    "com.mojang.authlib.properties.Property"
                );

            gamemodeClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".WorldSettings$EnumGamemode"
                );

            chatComponentClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".IChatBaseComponent"
                );

            packetInterface =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".Packet"
                );

            Class<?> entityPlayerClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".EntityPlayer"
                );

            Class<?> playerConnectionClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".PlayerConnection"
                );

            packetConstructor =
                packetClass.getDeclaredConstructor();

            packetConstructor.setAccessible(
                true
            );

            gameProfileConstructor =
                gameProfileClass.getConstructor(
                    UUID.class,
                    String.class
                );

            propertyConstructor =
                propertyClass.getConstructor(
                    String.class,
                    String.class
                );

            signedPropertyConstructor =
                propertyClass.getConstructor(
                    String.class,
                    String.class,
                    String.class
                );

            ConstructorSelection selection =
                findPlayerInfoDataConstructor();

            playerInfoDataConstructor =
                selection.constructor;

            playerInfoDataNeedsOuterPacket =
                selection.needsOuterPacket;

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

            Class<?> craftPlayerClass =
                Class.forName(
                    "org.bukkit.craftbukkit."
                        + nmsVersion
                        + ".entity.CraftPlayer"
                );

            getHandleMethod =
                craftPlayerClass.getMethod(
                    "getHandle"
                );

            playerConnectionField =
                entityPlayerClass.getField(
                    "playerConnection"
                );

            sendPacketMethod =
                playerConnectionClass.getMethod(
                    "sendPacket",
                    packetInterface
                );

            gameProfileGetPropertiesMethod =
                gameProfileClass.getMethod(
                    "getProperties"
                );

            survivalGamemode =
                enumValue(
                    gamemodeClass,
                    "SURVIVAL"
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

    public PacketBatchResult addBatch(
            Player viewer,
            Collection<VirtualEntry> entries,
            int maxEntriesPerPacket) {

        return sendBatch(
            viewer,
            "ADD_PLAYER",
            entries,
            maxEntriesPerPacket,
            true
        );
    }

    public PacketBatchResult updateBatch(
            Player viewer,
            Collection<VirtualEntry> entries,
            int maxEntriesPerPacket) {

        return sendBatch(
            viewer,
            "UPDATE_DISPLAY_NAME",
            entries,
            maxEntriesPerPacket,
            true
        );
    }

    public PacketBatchResult removeBatch(
            Player viewer,
            Collection<VirtualEntry> entries,
            int maxEntriesPerPacket) {

        return sendBatch(
            viewer,
            "REMOVE_PLAYER",
            entries,
            maxEntriesPerPacket,
            false
        );
    }

    public void add(
            Player viewer,
            VirtualEntry entry) {

        addBatch(
            viewer,
            Collections.singletonList(
                entry
            ),
            1
        );
    }

    public void update(
            Player viewer,
            VirtualEntry entry) {

        updateBatch(
            viewer,
            Collections.singletonList(
                entry
            ),
            1
        );
    }

    public void remove(
            Player viewer,
            VirtualEntry entry) {

        removeBatch(
            viewer,
            Collections.singletonList(
                entry
            ),
            1
        );
    }

    private PacketBatchResult sendBatch(
            Player viewer,
            String actionName,
            Collection<VirtualEntry> source,
            int maxEntriesPerPacket,
            boolean includeDisplayName) {

        PacketBatchResult result =
            new PacketBatchResult();

        if (viewer == null
                || !viewer.isOnline()
                || source == null
                || source.isEmpty()) {

            return result;
        }

        List<VirtualEntry> entries =
            new ArrayList<VirtualEntry>();

        for (VirtualEntry entry : source) {

            if (entry != null) {
                entries.add(entry);
            }
        }

        if (entries.isEmpty()) {
            return result;
        }

        int chunkSize =
            Math.max(
                1,
                Math.min(
                    80,
                    maxEntriesPerPacket
                )
            );

        for (int start = 0;
                start < entries.size();
                start += chunkSize) {

            int end =
                Math.min(
                    entries.size(),
                    start + chunkSize
                );

            List<VirtualEntry> chunk =
                new ArrayList<VirtualEntry>(
                    entries.subList(
                        start,
                        end
                    )
                );

            try {

                Object packet =
                    createPacket(
                        actionName,
                        chunk,
                        includeDisplayName
                    );

                sendPacket(
                    viewer,
                    packet
                );

                result.recordSuccess(
                    chunk
                );

            } catch (Exception failure) {

                result.recordFailure(
                    chunk,
                    failure
                );
            }
        }

        return result;
    }

    @SuppressWarnings({
        "unchecked",
        "rawtypes"
    })
    private Object createPacket(
            String actionName,
            List<VirtualEntry> entries,
            boolean includeDisplayName)
            throws Exception {

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

        for (VirtualEntry entry : entries) {

            data.add(
                createPlayerInfoData(
                    packet,
                    entry,
                    includeDisplayName
                        ? entry.getDisplayName()
                        : null
                )
            );
        }

        return packet;
    }

    private Object createPlayerInfoData(
            Object packet,
            VirtualEntry entry,
            String displayName)
            throws Exception {

        Object profile =
            gameProfileConstructor.newInstance(
                entry.getUuid(),
                entry.getTechnicalName()
            );

        applySkin(
            profile,
            entry.getSkin()
        );

        Object component =
            displayName == null
                ? null
                : chatComponent(
                    displayName
                );

        if (playerInfoDataNeedsOuterPacket) {

            return playerInfoDataConstructor.newInstance(
                packet,
                profile,
                Integer.valueOf(0),
                survivalGamemode,
                component
            );
        }

        return playerInfoDataConstructor.newInstance(
            profile,
            Integer.valueOf(0),
            survivalGamemode,
            component
        );
    }

    private void applySkin(
            Object profile,
            ResolvedTabSkin skin)
            throws Exception {

        if (profile == null
                || skin == null
                || !skin.hasTexture()) {

            return;
        }

        Object properties =
            gameProfileGetPropertiesMethod.invoke(
                profile
            );

        Object property =
            skin.hasSignature()
                ? signedPropertyConstructor.newInstance(
                    "textures",
                    skin.getValue(),
                    skin.getSignature()
                )
                : propertyConstructor.newInstance(
                    "textures",
                    skin.getValue()
                );

        Method put =
            findPutMethod(
                properties
            );

        put.invoke(
            properties,
            "textures",
            property
        );
    }

    private ConstructorSelection findPlayerInfoDataConstructor()
            throws NoSuchMethodException {

        for (Constructor<?> constructor
                : dataClass.getDeclaredConstructors()) {

            Class<?>[] types =
                constructor.getParameterTypes();

            if (types.length == 5
                    && packetClass.isAssignableFrom(
                        types[0]
                    )
                    && gameProfileClass.isAssignableFrom(
                        types[1]
                    )
                    && isIntType(
                        types[2]
                    )
                    && gamemodeClass.isAssignableFrom(
                        types[3]
                    )
                    && chatComponentClass.isAssignableFrom(
                        types[4]
                    )) {

                constructor.setAccessible(
                    true
                );

                return new ConstructorSelection(
                    constructor,
                    true
                );
            }

            if (types.length == 4
                    && gameProfileClass.isAssignableFrom(
                        types[0]
                    )
                    && isIntType(
                        types[1]
                    )
                    && gamemodeClass.isAssignableFrom(
                        types[2]
                    )
                    && chatComponentClass.isAssignableFrom(
                        types[3]
                    )) {

                constructor.setAccessible(
                    true
                );

                return new ConstructorSelection(
                    constructor,
                    false
                );
            }
        }

        throw new NoSuchMethodException(
            "Constructeur PlayerInfoData 1.8 compatible introuvable."
        );
    }

    private static boolean isIntType(
            Class<?> type) {

        return type == Integer.TYPE
            || type == Integer.class;
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

        Object connection =
            playerConnectionField.get(
                handle
            );

        sendPacketMethod.invoke(
            connection,
            packet
        );
    }

    private static Method findPutMethod(
            Object properties)
            throws NoSuchMethodException {

        for (Method method
                : properties.getClass()
                    .getMethods()) {

            if (!"put".equals(
                    method.getName())
                    || method.getParameterTypes()
                        .length != 2) {

                continue;
            }

            return method;
        }

        throw new NoSuchMethodException(
            "PropertyMap.put introuvable."
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

            if (expectedType.isAssignableFrom(
                    field.getType()
                )) {

                return field;
            }

        } catch (NoSuchFieldException ignored) {
        }

        for (Field field
                : owner.getDeclaredFields()) {

            if (expectedType.isAssignableFrom(
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

            if (List.class.isAssignableFrom(
                    field.getType()
                )) {

                return field;
            }

        } catch (NoSuchFieldException ignored) {
        }

        for (Field field
                : owner.getDeclaredFields()) {

            if (List.class.isAssignableFrom(
                    field.getType()
                )) {

                return field;
            }
        }

        throw new NoSuchFieldException(
            fallbackName
        );
    }

    @SuppressWarnings({
        "unchecked",
        "rawtypes"
    })
    private static Object enumValue(
            Class<?> enumClass,
            String name) {

        return Enum.valueOf(
            (Class<Enum>)
                enumClass.asSubclass(
                    Enum.class
                ),
            name
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

    private static final class ConstructorSelection {

        private final Constructor<?> constructor;
        private final boolean needsOuterPacket;

        private ConstructorSelection(
                Constructor<?> constructor,
                boolean needsOuterPacket) {

            this.constructor = constructor;
            this.needsOuterPacket = needsOuterPacket;
        }
    }
}
