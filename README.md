# CoR Clan

A RuneLite plugin for members of the **C o R** clan.

By default everything runs locally inside your own RuneLite client, and **no data leaves your computer**.
Two optional features, both off until you turn them on, talk to something outside it:

- **Clan sync** talks to the CoR clan server; see [Clan sync and your data](#clan-sync-and-your-data).
- The **CoR party** (you join it yourself with a button) uses RuneLite's own party service for the clan map;
  see [CoR party and your data](#cor-party-and-your-data).

## Features

- **Clan sidebar** with the clan logo, Discord and website buttons, gz leaderboards, an all-time and a
  weekly gz podium, weekly #1 streaks and an org chart of clan members by rank.
- **Custom clan chat icons** (only on your client): CoR's own icons replace the default clan rank icon
  next to names in clan chat, and specific members can be given a special icon from the config.
- **GZ tracker**: counts gz / grats / congrats messages in clan chat and attributes them to whoever the
  latest clan broadcast (drop, level, pet, ...) was about, for a short configurable window. Stats are
  stored in your RuneLite settings.
- **Rank glow** (only on your client): the clan Owner gets a golden outline and a lightning storm overhead, and
  Gem League ranks (Opal to Zenyte) get their gem's glow. `::glowzap`, `::glowshock` and `::glowstrike <id>`
  preview the storm effects on your own character. What you pick for yourself reaches other plugin users
  through Clan sync; without it everyone shows their rank's default.
- **Lavasockz's aura** (only on your client, while Show clan rank glows is on): a flickering molten outline
  and the Flames of Zamorak burning around him nonstop, following him as he walks. `::lavafx` plays it once on your own character. Lavasockz can
  switch it off in the CoR side panel (Dev glow, shown only to him) or with `::devglow`; with Clan sync on,
  other plugin users see his choice.
- **Clan sync** (optional, off by default): clan-wide gz leaderboards, podiums, GZ King, weekly trophies and
  streaks, plus the chat icons, member icons, titles and rank icons clan staff set on the clan server, and
  everyone's glow picks.
- **CoR party** (optional, joined with the **Join CoR party** button in the CoR side panel): the clan's RuneLite
  party, used only so members can choose to show each other on the world map.

## Clan sync and your data

Clan sync is **off by default**. Turning it on shows RuneLite's warning that it submits your IP address to a
3rd-party server not controlled or verified by RuneLite developers.

**Your client only ever reports about your own character.** It never sends anything another player said or
did. While sync is on, the plugin sends to the CoR clan server (`cor-clan-api-production.up.railway.app`):

- the gz's (gz / grats / congrats) **you** say in your clan's chat,
- clan broadcasts **about you** (for example "Lavasockz has received a drop: ..."),
- your own clan rank and the glows you picked for yourself, on login and when they change,
- your clan's rank names with their rank numbers (for example 126 "Owner", 5 "Captain"), once per login, so
  clan staff can pick a chat icon per rank. No player names are sent with them,
- your character name, your clan's name and your RuneLite account hash with each of those, so the server
  knows whose they are.

Nothing else from chat is sent: no other messages, nobody else's gz's or broadcasts, no private or public
chat. Gz's and broadcasts go out in small batches about every 30 seconds.

About every 2 minutes while you are logged in it downloads everyone's gz counts, clan ranks, icons, titles
and glow picks (as their own clients reported them or clan staff set them), the past weekly podiums, and the
chat icon images from the clan's admin page (small PNGs, at most 32x16 pixels).

While the server's data is loaded, the leaderboards, podiums, GZ King, weekly trophies, streaks, `!rank` and
`[GZ count]` show the server's numbers, which only include players who have sync on. Your client keeps
counting what it sees in clan chat either way, and shows those counts again whenever sync is off or the
server can't be reached. Turning sync off stops all traffic to the clan server immediately.

The first account to report under a character name owns that name on the server. If the server says your name
is linked to another account, the plugin tells you once in chat and in the side panel and stops reporting;
ask CoR staff to unlink it. Characters outside the CoR clan are refused by the server.

The server stores player names, clan ranks, glow picks, when gz's and broadcasts happened, and short hashes
used to match duplicates. It does not store the text of chat messages.

## CoR party and your data

The CoR party uses **RuneLite's own party service**, the same one behind RuneLite's Party plugin, and is only
used for the clan map. The plugin **never joins a party on its own**: you join with the **Join CoR party**
button in the CoR side panel (it asks first, and leaves any other party you are in, since you can only be in
one), and leave with **Leave CoR party** or by turning the plugin off.

Everyone in the party can see your character name. While you are in it, the plugin sends one thing to the party:

- **your map position** (world and tile), only while **Share my location** is on: every ~3 seconds when
  you move, every 30 seconds when you stand still. Never from inside instances, and inside the Wilderness
  only when **Share in Wilderness** is on. Only people who share see the clan map, but positions go to
  everyone in the party, so treat the passphrase like a key.
  So you can tell it works even when you are the only one sharing, your own rhino marker labelled **You**
  appears on the world map as soon as sharing starts, a chat message says so (and says when sharing pauses
  or what to turn on), and the CoR panel shows a **Clan map** status line.

Positions from players who are not in your clan are ignored. The default passphrase is public (it is in this
source code); staff can pick a new one and share it in Discord to keep strangers out.

Gz counts, staff icon lists and glow picks are no longer sent through the party.

### Local clan icon lists

Two settings in the **Staff** section stay on your own client and are not shared with anyone:

- **Clan member icons**: `name=icon,icon|Title` lines, like **Member icons**.
- **Clan rank icons**: `rank title=icon,icon|Title` lines using the clan's in-game rank titles, for
  example `Gnome child=gem|Gnome`. Everyone holding that rank gets those icons and title on your client.

They are used while Clan sync is off. With Clan sync on and loaded, the icons, titles and per-rank icons set
on the clan server replace them. Your own **Member icons** always win on your client.

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
| Chat icons | GZ King icon | Give the top all-time gz giver (clan-wide with Clan sync on) a special icon |
| Chat icons | Weekly top 3 trophies | This week's top 3 gz givers (resets Sunday 00:00 UTC; clan-wide with Clan sync on) get a gold, silver or bronze trophy next to their name |
| GZ tracker | Track gz's | Master switch |
| GZ tracker | GZ window | Seconds after a broadcast during which gz's count for that member (default 90) |
| GZ tracker | One gz per person | Only the first gz per member per broadcast counts as "received" |
| GZ tracker | Include guest clan chat | Also track the guest clan channel |
| GZ tracker | Show gz overlay | On-screen box with your own counts, the open gz window with a countdown, and a "+1" flash when a gz is counted |
| GZ tracker | Show [GZ count] on gz lines | Append `[GZ count: N]` to a member's chat line when they say gz, N being their all-time total (clan-wide with Clan sync on) |
| GZ tracker | Chat message when counted | Also print a local game message per counted gz (off by default) |
| GZ tracker | Max gz message length | Longer messages are never treated as a gz |
| Rank glow | Show clan rank glows | Draw the glows clan members picked (or their rank's default), only on your client |
| Gem tier glows | Zenyte glow ... Opal glow, Gem outline, Gem sparkles | The gem glow other plugin users see on you; shared through Clan sync |
| Clan sync | Sync with clan server | Off by default. Reports your own gz's, broadcasts about you, your clan rank and glow picks to the CoR clan server and loads the clan-wide counts, icons, titles and glows. See [Clan sync and your data](#clan-sync-and-your-data) |
| CoR party | Party passphrase | Empty uses the default. Everyone in CoR must use the same one |
| CoR party | Share my location | Off by default. Show your world and tile to party members on the world map, and see theirs |
| CoR party | Share in Wilderness | Keep sharing inside the Wilderness. Off means you vanish from the clan map there |
| Staff | Clan member icons, Clan rank icons | Icon lists kept on your client only, used while Clan sync is off. See [Local clan icon lists](#local-clan-icon-lists) |

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
