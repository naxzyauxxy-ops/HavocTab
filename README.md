# HavocTab

HavocTab is a fork of [TAB](https://github.com/NEZNAMY/TAB) (v6.1.2) with client-side
display toggles added. Everything TAB does — tablist formatting, nametags, header/footer,
scoreboard, bossbar, layout, belowname, sorting — works exactly the same.

## What's different from TAB

| | TAB | HavocTab |
|---|---|---|
| Plugin name | TAB | HavocTab |
| Command | `/tab` | `/havoctab` (aliases `/htab`, `/tab`) |
| Proxy command | `/btab` | `/bhavoctab` |
| Permissions | `tab.*` | `havoctab.*` |
| Java package | `me.neznamy.tab` | `dev.havoc.havoctab` |
| PlaceholderAPI expansion | `%tab_...%` | `%havoctab_...%` |
| Data folder | `plugins/TAB` | `plugins/HavocTab` |
| bStats | enabled | removed |
| Client-side toggles | — | **added** |

Your existing TAB `config.yml`, `groups.yml`, `users.yml` and `animations.yml` drop
straight into `plugins/HavocTab/` — the format is unchanged.

## New feature: client-side display toggles

Both toggles are **per-viewer**. When a player turns something off, only *that player*
stops seeing it. Nobody else's view changes, no ranks or permissions are touched
server-side, and the player's own rank still shows normally to everyone else.

### `/ranks` — hide rank prefixes/suffixes for yourself

Strips `tabprefix`/`tabsuffix` from the tablist and `tagprefix`/`tagsuffix` from
nametags above heads, for the player who ran the command only. Each half can be
turned off independently in config.

```
/ranks           toggle
/ranks on        show ranks
/ranks off       hide ranks
```

### `/belowname` — hide the belowname objective for yourself

Hides the text/number under player nametags — including animated ones such as
`%animation:belowname_cycle%` — for the player who ran the command only. The
objective is unregistered on that one client, so it costs nothing to hide.

```
/belowname       toggle
/belowname on    show belowname
/belowname off   hide belowname
```

### Status placeholders

| Placeholder | Output |
|---|---|
| `%havoctab_ranks%` | `&aON` / `&cOFF` |
| `%ranks%` | same, short alias |
| `%havoctab_belowname%` | `&aON` / `&cOFF` |
| `%belowname%` | same, short alias |

The ON/OFF text is configurable via `placeholder-value-on` / `placeholder-value-off`.
Use them anywhere HavocTab parses placeholders — header/footer, scoreboard lines,
tablist format, bossbar text:

```yaml
scoreboard:
  scoreboards:
    scoreboard:
      lines:
        - "* &fRanks&7: %havoctab_ranks%"
        - "* &fBelowname&7: %havoctab_belowname%"
```

### Configuration

Added to `config.yml` (auto-inserted on first start via the config converter,
config-version 7 → 8):

```yaml
client-display-settings:
  placeholder-value-on: "&aON"
  placeholder-value-off: "&cOFF"

  ranks:
    enabled: true
    toggle-command: "/ranks"
    remember-toggle-choice: true      # saved in playerdata.yml, restored on rejoin
    hidden-by-default: false          # start hidden for players who never toggled
    hide-tablist-prefix-suffix: true  # hide tabprefix/tabsuffix
    hide-nametag-prefix-suffix: true  # hide tagprefix/tagsuffix above heads
    require-permission: false         # require havoctab.ranks.toggle

  belowname:
    enabled: true
    toggle-command: "/belowname"
    remember-toggle-choice: true
    hidden-by-default: false
    require-permission: false         # require havoctab.belowname.toggle
```

Messages live in `messages.yml`: `ranks-toggle-on`, `ranks-toggle-off`,
`belowname-toggle-on`, `belowname-toggle-off`.

### Permissions

| Node | Default | Grants |
|---|---|---|
| `havoctab.ranks.toggle` | true | `/ranks` (only checked when `require-permission: true`) |
| `havoctab.belowname.toggle` | true | `/belowname` (only checked when `require-permission: true`) |
| `havoctab.admin` | op | everything |

## Chat module

Disabled by default (`chat.enabled: false`). Turn it on **only after** disabling chat
formatting in whatever plugin currently formats chat, or both will format the same message.

Paper 1.21.6+ renders chat once per recipient, which is what makes all of this per-viewer:
the same message can be styled for one player, plain for another, and never delivered to a
third who blocked the sender.

| Command | Does |
|---|---|
| `/block <player>` / `/ignore <player>` | Same command. Hides their chat, and their DMs if enabled. |
| `/unblock <player>` / `/unignore <player>` | Removes them from your block list. |
| `/blocklist` / `/ignorelist` | Opens the block list dialog. Click a name to unblock. |
| `/publicchat [on\|off]` | Stop receiving public chat entirely, for yourself. |
| `/chatformat [on\|off]` | Drop back to plain chat instead of the styled format. |

| Placeholder | Output |
|---|---|
| `%havoctab_publicchat%` / `%publicchat%` | `&aON` / `&cOFF` |
| `%havoctab_chatformat%` / `%chatformat%` | `&aON` / `&cOFF` |
| `%havoctab_blocked%` / `%blocked%` | number of blocked players |

`%chatprefix%` and `%chatsuffix%` are real HavocTab properties — set them per group in
`groups.yml` exactly like `tabprefix`, or drop `%luckperms-prefix%` into the format instead.

**The format, hover lines and click command all take placeholders** — every HavocTab
placeholder and, with PlaceholderAPI installed, every PAPI placeholder. They are loaded as
TAB `Property` objects, which registers each placeholder into the refresh cycle and resolves
relational (`%rel_...%`) placeholders per viewer.

What is *not* parsed is the text a player types. Their message is appended as its own
component rather than substituted into the format string, so typing `%vault_eco_balance%`
or `&c` in chat does nothing.

### Block list menu — Java and Bedrock

`block.menu-type` controls what `/blocklist` opens:

| Value | Behaviour |
|---|---|
| `AUTO` (default) | Native Paper dialog for Java players, chest GUI for Bedrock players |
| `DIALOG` | Always the dialog. Bedrock players can't see it — they get the chat list |
| `GUI` | Always the chest GUI. Works on every version and every client |
| `CHAT` | Clickable list printed into chat |

Paper dialogs are a Java-edition protocol feature, so a Bedrock client can't render one.
Geyser does translate container UIs into native Bedrock forms, which is why Bedrock players
get a chest menu of player heads instead — clicking a head unblocks that player.

Each layer degrades rather than failing: if the dialog throws (a Paper build whose Dialog API
differs from the one HavocTab was compiled against), it logs one warning and drops to the GUI;
if the GUI can't open, the chat list prints.

## Building

```bash
./gradlew build
```

Requires JDK 25 (build toolchain; output is Java 8 bytecode). Produces:

```
jar/build/libs/HavocTab v<version>.jar
jar/build/libs/HavocTab v<version> - Paper 1.20.5 - 1.21.4.jar
```

Use the second jar only on Paper 1.20.5–1.21.4, which rejects classes compiled with
Java 24+. Everything else uses the first.

CI is in `.github/workflows/build.yml` — it builds on every push and PR, uploads the
jars as artifacts, and publishes a GitHub release when you push a tag.

## License

Apache License 2.0, inherited from TAB. See `LICENSE` and `NOTICE`.
TAB is © NEZNAMY and contributors.
