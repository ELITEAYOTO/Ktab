package me.krunsh.ktab.packet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Résultat d'un envoi batch PlayerInfo.
 *
 * Un UUID est considéré synchronisé uniquement si le packet contenant
 * l'entrée a réellement été remis à PlayerConnection#sendPacket sans erreur
 * de réflexion/invocation.
 */
public final class PacketBatchResult {

    private final Set<UUID> successfulUuids =
        new HashSet<UUID>();

    private int attemptedEntries;
    private int packetsSent;
    private int packetsFailed;

    private String lastError = "";

    public void recordSuccess(
            Iterable<VirtualEntry> entries) {

        packetsSent++;

        if (entries == null) {
            return;
        }

        for (VirtualEntry entry : entries) {

            if (entry == null
                    || entry.getUuid() == null) {

                continue;
            }

            attemptedEntries++;

            successfulUuids.add(
                entry.getUuid()
            );
        }
    }

    public void recordFailure(
            Iterable<VirtualEntry> entries,
            Throwable failure) {

        packetsFailed++;

        if (entries != null) {

            for (VirtualEntry entry : entries) {

                if (entry != null) {
                    attemptedEntries++;
                }
            }
        }

        if (failure != null) {

            String message =
                failure.getMessage();

            lastError =
                failure.getClass()
                    .getSimpleName()
                    + (message == null
                        || message.trim().isEmpty()
                            ? ""
                            : ": " + message);
        }
    }

    public boolean wasSuccessful(
            VirtualEntry entry) {

        return entry != null
            && entry.getUuid() != null
            && successfulUuids.contains(
                entry.getUuid()
            );
    }

    public boolean wasSuccessful(
            UUID uuid) {

        return uuid != null
            && successfulUuids.contains(
                uuid
            );
    }

    public Set<UUID> getSuccessfulUuids() {

        return Collections.unmodifiableSet(
            successfulUuids
        );
    }

    public int getAttemptedEntries() {
        return attemptedEntries;
    }

    public int getSuccessfulEntries() {
        return successfulUuids.size();
    }

    public int getPacketsSent() {
        return packetsSent;
    }

    public int getPacketsFailed() {
        return packetsFailed;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isFullySuccessful() {
        return packetsFailed == 0;
    }
}
