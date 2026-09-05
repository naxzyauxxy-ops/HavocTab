package dev.havoc.havoctab.shared.features.clientdisplay;

/**
 * Per-player data for HavocTab's client-side display toggles.
 * <p>
 * Everything stored here is a <b>viewer</b> preference. It only changes what
 * <i>this</i> player sees, never what other players see, which is the whole point
 * of the feature - a player can turn ranks off for themselves without affecting
 * anyone else on the server.
 */
public class ClientDisplayPlayerData {

    /**
     * If {@code true}, this player chose not to see rank prefixes/suffixes.
     */
    public volatile boolean ranksHidden;

    /**
     * If {@code true}, this player chose not to see the belowname objective
     * (the text/number displayed under player nametags).
     */
    public volatile boolean belowNameHidden;

    /**
     * Mirror of {@code client-display-settings.ranks.hide-tablist-prefix-suffix},
     * copied here so hot paths do not need to look the feature up.
     */
    public volatile boolean ranksAffectTablist = true;

    /**
     * Mirror of {@code client-display-settings.ranks.hide-nametag-prefix-suffix},
     * copied here so hot paths do not need to look the feature up.
     */
    public volatile boolean ranksAffectNameTags = true;

    /**
     * Returns {@code true} if tablist prefixes and suffixes should be stripped for this viewer.
     *
     * @return  {@code true} if tablist ranks are hidden for this viewer
     */
    public boolean hidesTablistRanks() {
        return ranksHidden && ranksAffectTablist;
    }

    /**
     * Returns {@code true} if nametag prefixes and suffixes should be stripped for this viewer.
     *
     * @return  {@code true} if nametag ranks are hidden for this viewer
     */
    public boolean hidesNameTagRanks() {
        return ranksHidden && ranksAffectNameTags;
    }
}
