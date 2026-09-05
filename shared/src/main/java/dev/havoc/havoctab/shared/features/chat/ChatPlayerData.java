package dev.havoc.havoctab.shared.features.chat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player data for HavocTab's chat feature.
 * <p>
 * Like the display toggles, everything here is a <b>viewer</b> preference - it changes
 * what this player sees, never what anyone else sees.
 */
public class ChatPlayerData {

    /** Chat prefix property, loaded from groups.yml / users.yml */
    public dev.havoc.havoctab.shared.Property prefix;

    /** Chat suffix property, loaded from groups.yml / users.yml */
    public dev.havoc.havoctab.shared.Property suffix;

    /** Styled chat format, as a Property so its placeholders are registered and refreshed */
    public dev.havoc.havoctab.shared.Property format;

    /** Plain chat format used by viewers who turned the format off */
    public dev.havoc.havoctab.shared.Property plainFormat;

    /** Hover card lines joined with newlines, split again after resolving */
    public dev.havoc.havoctab.shared.Property hover;

    /** Click command attached to the message */
    public dev.havoc.havoctab.shared.Property clickCommand;

    /** If {@code true}, this player does not want to see public chat at all */
    public volatile boolean publicChatHidden;

    /** If {@code true}, this player wants plain chat instead of the configured format */
    public volatile boolean formatDisabled;

    /**
     * Players this viewer has blocked, mapped to the last known name of each so the
     * block list can display something readable without a Mojang lookup.
     */
    @NotNull
    private final Map<UUID, String> blocked = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Returns {@code true} if this player has blocked the given player.
     *
     * @param   uniqueId
     *          UUID of the player to check
     * @return  {@code true} if blocked
     */
    public boolean hasBlocked(@NotNull UUID uniqueId) {
        return blocked.containsKey(uniqueId);
    }

    /**
     * Returns the last known name of a blocked player, or {@code null} if not blocked.
     *
     * @param   uniqueId
     *          UUID of the player
     * @return  Stored name, or {@code null}
     */
    @Nullable
    public String getBlockedName(@NotNull UUID uniqueId) {
        return blocked.get(uniqueId);
    }

    /**
     * Returns a snapshot copy of the block list, safe to iterate.
     *
     * @return  Copy of blocked players mapped to their last known names
     */
    @NotNull
    public Map<UUID, String> getBlocked() {
        synchronized (blocked) {
            return new LinkedHashMap<>(blocked);
        }
    }

    /**
     * Returns how many players this viewer has blocked.
     *
     * @return  Block list size
     */
    public int getBlockedCount() {
        return blocked.size();
    }

    /**
     * Adds a player to the block list.
     *
     * @param   uniqueId
     *          UUID of the player to block
     * @param   name
     *          Name to remember them by
     * @return  {@code true} if they were not already blocked
     */
    public boolean addBlocked(@NotNull UUID uniqueId, @NotNull String name) {
        return blocked.put(uniqueId, name) == null;
    }

    /**
     * Removes a player from the block list.
     *
     * @param   uniqueId
     *          UUID of the player to unblock
     * @return  {@code true} if they were blocked
     */
    public boolean removeBlocked(@NotNull UUID uniqueId) {
        return blocked.remove(uniqueId) != null;
    }

    /**
     * Replaces the whole block list, used when loading from disk.
     *
     * @param   values
     *          New contents of the block list
     */
    public void setBlocked(@NotNull Map<UUID, String> values) {
        synchronized (blocked) {
            blocked.clear();
            blocked.putAll(values);
        }
    }
}
