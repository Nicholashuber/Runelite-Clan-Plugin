# CoR Clan

A RuneLite plugin for members of the **C o R** clan.

Everything runs locally inside your own RuneLite client. The only exception is the optional **CoR party**
(you join it yourself with a button), which uses RuneLite's own party service; see [CoR party and your data](#cor-party-and-your-data).

## Features

- **Clan sidebar** with the clan logo, Discord and website buttons, gz leaderboards, an all-time and a
  weekly gz podium, weekly #1 streaks and an org chart of clan members by rank.
- **Custom clan chat icons** (only on your client): CoR's own icons replace the default clan rank icon
  next to names in clan chat, and specific members can be given a special icon from the config.
- **GZ tracker**: counts gz / grats / congrats messages in clan chat and attributes them to whoever the
  latest clan broadcast (drop, level, pet, ...) was about, for a short configurable window. Stats are
  stored in your RuneLite settings.
- **Rank glow** (only on your client): the clan Owner gets a golden outline and a lightning storm overhead.
  `::glowzap`, `::glowshock` and `::glowstrike <id>` preview the storm effects on your own character.
- **Lavasockz's aura** (only on your client, while Show clan rank glows is on): a flickering molten outline
  and the Flames of Zamorak burning around him nonstop, following him as he walks. `::lavafx` plays it once on your own character. Lavasockz can
  switch it off in the CoR side panel (Dev glow, shown only to him) or with `::devglow`; party members see his
  choice.
- **CoR party** (optional, joined with the **Join CoR party** button in the CoR side panel): the clan's RuneLite party, so members share gz counts
  (matching leaderboards, GZ King and weekly trophies for everyone online), get icon settings from clan staff,
  and can choose to show each other on the world map.

## CoR party and your data

The CoR party uses **RuneLite's own party service**, the same one behind RuneLite's Party plugin. The plugin
talks to no other server. The plugin **never joins a party on its own**: you join with the **Join CoR party**
button in the CoR side panel (it asks first, and leaves any other party you are in, since you can only be in
one), and leave with **Leave CoR party** or by turning the plugin off.

Everyone in the party can see your character name. While you are in it, the plugin sends to the party:

- **your own gz totals**: how many gz's you gave (all time and this week) and how many you received, as your
  client counted them. Only your own numbers, never anyone else's. Sent when you join, when someone else joins,
  and at most once a minute when they changed. Leaderboards show what your own client counted, with each
  party member's own report where it is higher; weekly reports are capped at what the 100-per-hour limit allows.
- **your map position** (world and tile), only while **Share my location** is on: every ~3 seconds when
  you move, every 30 seconds when you stand still. Never from inside instances, and inside the Wilderness
  only when **Share in Wilderness** is on. Only people who share see the clan map, but positions go to
  everyone in the party, so treat the passphrase like a key.
  So you can tell it works even when you are the only one sharing, your own rhino marker labelled **You**
  appears on the world map as soon as sharing starts, a chat message says so (and says when sharing pauses
  or what to turn on), and the CoR panel shows a **Clan map** status line.
- **staff icon lists**, only if your in-game clan rank is Administrator or higher.

Messages from players who are not in your clan are ignored. The default passphrase is public (it is in this
source code); staff can pick a new one and share it in Discord to keep strangers out.

### Staff icon lists

Two settings in the **Staff** section are shared through the party:

- **Clan member icons**: `name=icon,icon|Title` lines, like **Member icons**.
- **Clan rank icons**: `rank title=icon,icon|Title` lines using the clan's in-game rank titles, for
  example `Gnome child=gem|Gnome`. Everyone holding that rank gets those icons and title, so promoting
  someone in game changes their icon.

When a member with Administrator rank or higher edits them, the new lists go to everyone in the party and the
newest edit wins. Everyone else's boxes show the clan's current lists and are overwritten by staff updates.
Your own **Member icons** always win on your client.

## Commands

Type `::cor` (or `::test`) in the chatbox to print a local CoR banner with the GZ King, the most gz'd
members and your own counts. Double-colon commands are handled inside RuneLite and are never sent to
the game server; only you see the output.

## Config

| Section | Option | What it does |
|---------|--------|--------------|
| Links | Discord invite, Website | URLs opened by the panel buttons |
| Chat icons | Custom rank icons | Replace the default clan rank icon with CoR icons |
| Chat icons | Member icons | One per line, `name=icon`, `name=icon,icon` or `name=icon,icon|Title`. Icons: `crown`, `trophy`, `star`, `skull`, `gem`, `fire`, `founder`, `dev`. A title shows as `[Title]` before the name. Icons stack: member icons, then GZ King, then the weekly trophy, then the rank rhino |
| Chat icons | GZ King icon | Give the top all-time gz giver (party-wide in the CoR party) a special icon |
| Chat icons | Weekly top 3 trophies | This week's top 3 gz givers (resets Sunday 00:00 UTC; party-wide in the CoR party) get a gold, silver or bronze trophy next to their name |
| GZ tracker | Track gz's | Master switch |
| GZ tracker | GZ window | Seconds after a broadcast during which gz's count for that member (default 90) |
| GZ tracker | One gz per person | Only the first gz per member per broadcast counts as "received" |
| GZ tracker | Include guest clan chat | Also track the guest clan channel |
| GZ tracker | Show gz overlay | On-screen box with your own counts, the open gz window with a countdown, and a "+1" flash when a gz is counted |
| GZ tracker | Show [GZ count] on gz lines | Append `[GZ count: N]` to a member's chat line when they say gz, N being their all-time total on your client |
| GZ tracker | Chat message when counted | Also print a local game message per counted gz (off by default) |
| GZ tracker | Max gz message length | Longer messages are never treated as a gz |
| Rank glow | Glow clan ranks | Golden outline and lightning storm on the clan Owner, only on your client |
| CoR party | Party passphrase | Empty uses the default. Everyone in CoR must use the same one |
| CoR party | Share my location | Off by default. Show your world and tile to party members on the world map, and see theirs |
| CoR party | Share in Wilderness | Keep sharing inside the Wilderness. Off means you vanish from the clan map there |
| Staff | Clan member icons, Clan rank icons | Icon lists edited by staff and shared through the party. See [Staff icon lists](#staff-icon-lists) |

## Development

Requires JDK 11 or newer.

```
./gradlew test      # unit tests
./gradlew run       # launches a RuneLite development client with the plugin loaded
```

If you use a Jagex Account, do this once so the development client can log in:

1. Open **RuneLite (configure)** from the Start menu.
2. In *Client arguments* enter `--insecure-write-credentials` and save.
3. Launch RuneLite through the Jagex Launcher, log in, then close it.
   A `credentials.properties` file now exists in your `.runelite` folder. Never share it.
4. Remove the client argument again, then run `./gradlew run`.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
