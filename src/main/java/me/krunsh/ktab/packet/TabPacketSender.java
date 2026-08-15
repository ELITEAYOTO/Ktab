package me.krunsh.ktab.packet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Envoi header/footer compatible Spigot/PandaSpigot 1.8.8 par réflexion.
 *
 * Aucun import NMS compile-time : le jar reste construit uniquement contre
 * spigot-api.
 */
public final class TabPacketSender {

    private final String nmsVersion;

    private final Class<?> packetClass;
    private final Class<?> componentClass;
    private final Method serializerMethod;

    private final Field headerField;
    private final Field footerField;

    private final Method getHandleMethod;
    private final Field playerConnectionField;
    private final Method sendPacketMethod;

    public TabPacketSender() {

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
                        + ".PacketPlayOutPlayerListHeaderFooter"
                );

            componentClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".IChatBaseComponent"
                );

            Class<?> serializerClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".IChatBaseComponent$ChatSerializer"
                );

            serializerMethod =
                serializerClass.getMethod(
                    "a",
                    String.class
                );

            headerField =
                packetClass.getDeclaredField("a");

            footerField =
                packetClass.getDeclaredField("b");

            headerField.setAccessible(true);
            footerField.setAccessible(true);

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

            Class<?> entityPlayerClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".EntityPlayer"
                );

            playerConnectionField =
                entityPlayerClass.getField(
                    "playerConnection"
                );

            Class<?> playerConnectionClass =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".PlayerConnection"
                );

            Class<?> packetInterface =
                Class.forName(
                    "net.minecraft.server."
                        + nmsVersion
                        + ".Packet"
                );

            sendPacketMethod =
                playerConnectionClass.getMethod(
                    "sendPacket",
                    packetInterface
                );

        } catch (Exception failure) {

            throw new IllegalStateException(
                "Initialisation packet TAB impossible.",
                failure
            );
        }
    }

    public void send(
            Player player,
            String header,
            String footer) {

        if (player == null
                || !player.isOnline()) {

            return;
        }

        try {

            Object packet =
                packetClass.newInstance();

            Object headerComponent =
                serializerMethod.invoke(
                    null,
                    jsonText(header)
                );

            Object footerComponent =
                serializerMethod.invoke(
                    null,
                    jsonText(footer)
                );

            if (!componentClass.isInstance(headerComponent)
                    || !componentClass.isInstance(footerComponent)) {

                throw new IllegalStateException(
                    "Composant chat NMS inattendu."
                );
            }

            headerField.set(
                packet,
                headerComponent
            );

            footerField.set(
                packet,
                footerComponent
            );

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

        } catch (Exception failure) {

            throw new IllegalStateException(
                "Envoi TAB impossible pour "
                    + player.getName(),
                failure
            );
        }
    }

    public String getNmsVersion() {
        return nmsVersion;
    }

    private static String jsonText(
            String text) {

        return "{\"text\":\""
            + escapeJson(
                text == null
                    ? ""
                    : text
            )
            + "\"}";
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

                    if (current < 0x20) {

                        String hex =
                            Integer.toHexString(
                                current
                            );

                        builder.append("\\u");

                        for (int pad =
                                hex.length();
                                pad < 4;
                                pad++) {

                            builder.append('0');
                        }

                        builder.append(hex);

                    } else {
                        builder.append(current);
                    }

                    break;
            }
        }

        return builder.toString();
    }
}
