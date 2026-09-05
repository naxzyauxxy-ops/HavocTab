package dev.havoc.havoctab.shared.features.playerlist;

import dev.havoc.havoctab.shared.Property;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class holding tablist formatting data for players.
 */
public class TablistFormattingPlayerData {

    /** Player's tabprefix */
    public Property prefix;

    /** Player's customtabname */
    public Property name;

    /** Player's tabsuffix */
    public Property suffix;

    /** Flag tracking whether this feature is disabled for the player with condition or not */
    public final AtomicBoolean disabled = new AtomicBoolean();
}