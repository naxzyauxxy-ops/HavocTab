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

## Tablist pages

The Minecraft Java client draws a maximum of **80 tablist entries**. Past that, the server
can keep sending players but the client just won't render them — some people are invisible
in tab no matter what you configure.

`tablist-pages` works around it by cycling a window: everyone is split into pages of at most
`page-size`, and the visible page advances on a timer, so over one rotation everyone gets
shown.

```yaml
tablist-pages:
  enabled: false
  page-size: 80
  cycle-interval-milliseconds: 5000
  always-show-self: true
```

Hiding uses the tablist **listed flag** rather than removing entries. The entry stays on the
client, so skins stay cached, scoreboard teams and nametags are untouched, and a page flip is
one tiny packet per player instead of a remove-and-re-add churn several times a minute.

The listed flag is 1.19.3+. Older clients have no way to hide an entry without removing it, so
for them the feature stays off and they keep seeing the normal truncated list.

Placeholders `%havoctab_page%` and `%havoctab_pages%` — useful in the header:
`"&7Page &f%havoctab_page%&7/&f%havoctab_pages%"`. While 80 or fewer players are online the
feature does nothing at all.

## Tags

Cosmetic tags configured in `tags.yml`, equipped through `/tags`.

**They show up on their own.** `display.tablist: true` (the default) makes HavocTab append the
equipped tag to the player's tab list line at render time, so you never touch `groups.yml`.
That default exists because per-group values in `groups.yml` *override* each other rather than
combining — placing the tag by hand means editing `tabsuffix` on every single group.

```yaml
display:
  tablist: true      # tab list line
  nametag: false     # name above the player's head
  position: SUFFIX   # or PREFIX
```

Prefer to place it yourself? Set `tablist: false` and put `%havoctab_tag%` wherever you like —
`tabprefix`/`tabsuffix` in `groups.yml`, the chat format, a scoreboard line, the header/footer.
Don't do both, or the tag appears twice.

**Ownership is by permission** — `havoctab.tag.<id>` by default. Your store, crates and rank
packages already grant permissions, so they already grant tags with no integration work.

```yaml
tags:
  teamdrain:
    display: "&#f40d0d[&#f40d0d&lTEAM DRAIN&#f40d0d] "
    name: "&#f40d0d[&lTEAM DRAIN&r&#f40d0d]"
    material: "NAME_TAG"
    limited: false
    weight: 10
```

`display`, `name` and `lore` all go through HavocTab's text parser, so **every** format works:
`&3` legacy codes, `&#f40d0d` hex, `<gradient:#a:#b>`, MiniMessage, and custom fonts via
`<font:namespace:id>text</font>` — including fonts from your own resource pack.

One command does everything. There is deliberately no `/tag` — vanilla Minecraft has owned
that command since 1.13, so registering it would fight with the server.

| | |
|---|---|
| `/tags` | Opens the menu |
| `/tags <id>` | Equips that tag |
| `/tags clear` | Takes your tag off |
| `/tags limited` | Opens the limited tags list |
| `/tags favorites` | Opens your favorited tags |
| Left click | Equip / unequip |
| Right click | Favorite / unfavorite |

**One tag at a time.** With `require-unequip-first: true` (the default), equipping a second
tag while one is already on is refused rather than silently swapping. Clicking the tag you
are already wearing always takes it off, so there is no state a player can get stuck in.
Set it to `false` if you would rather clicking swap straight over.

**Favorites** are a real filter, not just a label: right-click stars a tag, starred tags sort
to the front of every list, and the favorites button switches the menu to showing only those.

**Feedback goes above the hotbar**, not into chat — `Equipped [TEAM DRAIN]`, `Tag removed`,
`Unequip [FRUITY] first`. Tag messages describe what a click just did and are irrelevant a
moment later, so they don't belong in chat history. Set `action-bar-messages: false` to put
them back in chat. Servers older than 1.9 have no action bar and fall back to chat on their
own. The search prompt stays in chat deliberately, since an action bar fades after a few
seconds and the player needs to read it while typing.

The menu has statistics, a limited-tags sub-menu, pagination, clear and chat-based search —
every label configurable in `messages.yml`. Equipped tag and favorites persist in
`playerdata.yml`. A tag whose permission a player loses stops showing automatically, so
expiring ranks need no cleanup — and it never blocks them from equipping something else.

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
| `/mentions [on\|off]` | Stop being pinged when someone says your name. |

| Placeholder | Output |
|---|---|
| `%havoctab_publicchat%` / `%publicchat%` | `&aON` / `&cOFF` |
| `%havoctab_chatformat%` / `%chatformat%` | `&aON` / `&cOFF` |
| `%havoctab_blocked%` / `%blocked%` | number of blocked players |
| `%havoctab_mentions%` / `%mentions%` | `&aON` / `&cOFF` |

`%chatprefix%` and `%chatsuffix%` are real HavocTab properties — set them per group in
`groups.yml` exactly like `tabprefix`, or drop `%luckperms-prefix%` into the format instead.

**The format, hover lines and click command all take placeholders** — every HavocTab
placeholder and, with PlaceholderAPI installed, every PAPI placeholder. They are loaded as
TAB `Property` objects, which registers each placeholder into the refresh cycle and resolves
relational (`%rel_...%`) placeholders per viewer.

What is *not* parsed is the text a player types. Their message is appended as its own
component rather than substituted into the format string, so typing `%vault_eco_balance%`
or `&c` in chat does nothing.

### Mentions

Typing a player's name pings them — `NaxzyAuxxy hello` highlights the name in chat and
shows that player an **action bar** notification plus a sound. Never a chat message; a ping
adds no line to anyone's chat.

```yaml
  mentions:
    enabled: true
    require-at-symbol: false   # true = only "@Name" counts
    highlight: "&#f40d0d&l%player%"
    action-bar: "&fYou have been mentioned by &#f40d0d%player%"
    sound:
      enabled: true
      name: "ENTITY_EXPERIENCE_ORB_PICKUP"   # enum name or namespaced key
      volume: 1.0
      pitch: 1.2
    toggle:
      enabled: true
      toggle-command: "/mentions"
      remember-toggle-choice: true
      hidden-by-default: false
```

**You can never mention yourself.** Your own name in your own message isn't highlighted and
doesn't ping you — saying your own name is just talking, and pinging yourself is noise. There
is no setting for this; `allow-self-mention` was removed.

Names match whole-word and case-insensitively, so `NaxzyAuxxy` pings but `NaxzyAuxxyzzz`
doesn't. Detection is a single pass over the message against a name lookup map, not a scan
of every online player per message.

`/mentions off` stops the ping only — the name still highlights in chat for everyone,
including the person who opted out. Someone who blocked the sender, or who has public chat
off, is never pinged: a ping without the message behind it is just confusing.

### Chat filter

Staff with `havoctab.chat.bypassfilter` (op by default) skip every check.

**Spam.** A per-player cooldown and repeat detection *block* the message. Excess caps and
long character runs *rewrite* it instead — `HELLOOOOOO` becomes `helloo` rather than being
refused, which players find far less irritating than a rejection.

**Blocked words.** The word list ships **empty** — you fill it with what you want blocked.
Matching normalises the message first: lowercase, common letter swaps undone (`4`→`a`,
`$`→`s`, `1`→`i`…), punctuation and spacing stripped, runs of 3+ characters collapsed. One
entry therefore also catches `s1ur`, `s l u r` and `$£ur`-style evasions.

That normalisation is deliberately lossy, which creates false positives inside innocent
words — the classic Scunthorpe problem. `allowed-phrases` is the escape hatch: anything
listed there is removed before matching.

`action: BLOCK` refuses the message. `CENSOR` only catches the plain spelling, because a
word disguised with symbols can be *detected* in normalised form but can't be *located* in
the original text to replace. BLOCK is the default for that reason.

**Advertising.** IPv4 addresses, Discord invites and domains, with domains driven by a
configurable regex. Split-up domains are rejoined first, so `play . example . com` and
`play(dot)example(dot)com` are caught. Put your own links in `allowed` or players won't be
able to share them.

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
