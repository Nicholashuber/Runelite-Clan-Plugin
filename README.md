# CoR Clan

A RuneLite plugin for members of the **C o R** clan.

By default everything runs locally inside your own RuneLite client, and **no data leaves your computer**.
The optional **Clan sync** setting (off by default) is the only feature that talks to a server; see
[Clan sync and your data](#clan-sync-and-your-data).
##UIMFQ
## Features

- **Clan sidebar** with the clan logo, Discord and website buttons, and gz leaderboards.
- **Custom clan chat icons** (only on your client): CoR's own icons replace the default clan rank icon
  next to names in clan chat, and specific members can be given a special icon from the config.
- **GZ tracker**: counts gz / grats / congrats messages in clan chat and attributes them to whoever the
  latest clan broadcast (drop, level, pet, ...) was about, for a short configurable window. Stats are
  stored in your RuneLite settings.
- **Clan sync** (optional, off by default): clan-wide gz leaderboards, plus chat icons, member icons and
  titles managed by clan admins, shared through the CoR clan server.
- **Clan map** (optional, off by default): clanmates who share their position appear on the in-game world map
  with a rhino marker, their name and world. Only people who share can see others.

## Clan sync and your data

Clan sync is **off by default**. Turning it on shows RuneLite's warning that it submits your IP address to a
3rd-party server not controlled or verified by RuneLite developers.

When it is on, the plugin sends to the CoR clan server (`cor-clan-api-production.up.railway.app`):

- clan broadcasts you see in your clan's chat (for example "Zezima has received a drop: ..."),
- clan chat messages that are a gz (gz / grats / congrats), with the name of who said them,
- your clan's rank names with their rank numbers (for example 126 "Owner", 5 "Captain"), once per login, so
  clan admins can pick a chat icon per rank. No player names are sent with them,
- your character name, your clan's name and your RuneLite account hash, so reports from different
  clan members can be merged.

Nothing else from chat is sent: no other messages, no private chat, no public chat. Events are sent in
small batches about every 30 seconds.

It downloads the clan-wide gz leaderboard, the member icons and titles set by clan admins, and the chat
icon images from the clan's admin page (small PNGs, at most 32x16 pixels), about every 2 minutes. If the server is unreachable the plugin keeps working locally. Turning sync off stops all
network traffic immediately.

The server stores player names, when gz's and broadcasts happened, and short hashes used to match
duplicate reports. It does not store the text of chat messages.

## Clan map and your data

**Share my location** is **off by default** and shows RuneLite's IP-address warning when turned on. While it is on,
the plugin sends your character name, RuneLite account hash, world and map tile to the CoR clan server every
3 seconds. The reply lists clanmates who are sharing, and they appear on the world map with a rhino marker;
hovering shows their name and world, and clicking jumps the map to them. Only people who share can see others.

- Inside the **Wilderness** your position is not sent unless **Share in Wilderness** is also on (meant for clan
  PK trips). With it off you vanish from the clan map as soon as you enter the Wilderness and can't see others
  until you leave.
- Positions are never sent from inside instances (raids and some bosses), where coordinates are not map tiles.
- Turning sharing off, logging out or disabling the plugin removes you from the server right away. The server
  keeps positions in memory only, for at most one minute after the last report, and never stores them.
- Positions are as private as the clan-name check: the server only accepts and shows positions to clients
  that report being in C o R. It does not verify that claim.

## Commands

Type `::cor` (or `::test`) in the chatbox to print a local CoR banner with the GZ King, the most gz'd
members and your own counts. Double-colon commands are handled inside RuneLite and are never sent to
the game server; only you see the output.

## Config

| Section | Option | What it does |
|---------|--------|--------------|
| Links | Discord invite, Website | URLs opened by the panel buttons |
| Chat icons | Custom rank icons | Replace the default clan rank icon with CoR icons |
| Chat icons | Member icons | One per line, `name=icon`, `name=icon,icon` or `name=icon,icon|Title`. Icons: `crown`, `trophy`, `star`, `skull`, `gem`, `fire`, `founder`, `dev`. A title shows as `[Title]` before the name. Icons stack: member icons, then GZ King, then the rank rhino |
| Chat icons | GZ King icon | Give the top all-time gz giver a special icon |
| GZ tracker | Track gz's | Master switch |
| GZ tracker | GZ window | Seconds after a broadcast during which gz's count for that member (default 90) |
| GZ tracker | One gz per person | Only the first gz per member per broadcast counts as "received" |
| GZ tracker | Include guest clan chat | Also track the guest clan channel |
| GZ tracker | Show gz overlay | On-screen box with your own counts, the open gz window with a countdown, and a "+1" flash when a gz is counted |
| GZ tracker | Show [GZ count] on gz lines | Append `[GZ count: N]` to a member's chat line when they say gz, N being their all-time total on your client |
| GZ tracker | Chat message when counted | Also print a local game message per counted gz (off by default) |
| GZ tracker | Max gz message length | Longer messages are never treated as a gz |
| Clan map | Share my location | Off by default. Sends your world and map tile every 3 seconds and shows clanmates who share on the world map. See [Clan map and your data](#clan-map-and-your-data) |
| Clan map | Share in Wilderness | Keep sharing inside the Wilderness. Off means you vanish from the clan map there |
| Clan sync | Sync with clan server | Off by default. Shares gz sightings with the CoR clan server and loads the clan-wide leaderboard and member icons. See [Clan sync and your data](#clan-sync-and-your-data) |

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
