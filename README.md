# CoR Clan

A RuneLite plugin for members of the **C o R** clan.

By default everything runs locally inside your own RuneLite client, and **no data leaves your computer**.
Two optional features, both off until you turn them on, talk to something outside it:

- **Clan sync** talks to the CoR clan server; see [Clan sync and your data](#clan-sync-and-your-data).
- The **CoR party** (you join it yourself with a button) uses RuneLite's own party service for the clan map;
  see [CoR party and your data](#cor-party-and-your-data).

## Features

- **Announcement banner** at the top of the CoR side panel (with Clan sync on): events and news from the clan
  leaders (uimfartqueef, joeboob, Lavasockz and DAYLlGHT), who get an **Edit** button on it.
- **Clan sidebar** with the clan logo, Discord and website buttons, gz leaderboards, an all-time and a
  weekly gz podium, weekly #1 streaks and an org chart of clan members by rank.
- **Custom clan chat icons** (only on your client): CoR's own icons replace the default clan rank icon
  next to names in clan chat, and specific members can be given a special icon from the config.
- **GZ tracker**: counts gz / grats / congrats messages in clan chat and attributes them to whoever the
  latest clan broadcast (drop, level, pet, ...) was about, for a short configurable window. Stats are
  stored in your RuneLite settings.
- **Rank glow** (only on your client): the clan Owner gets a golden outline and a lightning storm overhead, and
  Gem League ranks (Opal to Zenyte) get their gem's glow, and clan staff (administrators up to the deputy owner) twinkle gold
  (each can switch theirs off with the **Staff gold twinkle** setting). `::glowzap`, `::glowshock` and `::glowstrike <id>`
  preview the storm effects on your own character. What you pick for yourself reaches other plugin users
  through Clan sync; without it everyone shows their rank's default.
- **Lavasockz's aura** (only on your client, while Show clan rank glows is on): a flickering molten outline
  and the Flames of Zamorak burning around him nonstop, following him as he walks. `::lavafx` plays it once on your own character. Lavasockz can
  switch it off in the CoR side panel (Dev glow, shown only to him) or with `::devglow`; with Clan sync on,
  other plugin users see his choice.
- **JoeBoob's nature aura** (only on your client, while Show clan rank glows is on): a green take on DAYLlGHT's,
  with a Zanaris fairy ring and leaves springing up around him, a leaf green swirl, the Druidic spirit's bloom and
  emerald sparkles. JoeBoob switches
  each part in the CoR side panel (Nature glow) or with `::natureglow`; `::naturefx` tries other graphics.
- **Rank card** in the CoR side panel: your clan rank with its emblem, your gz counts and one big **Glow** switch
  that turns your own rank glow on or off (the same settings as the glow checkboxes further down).
- **Legion login alerts** (setting, on by default): a chat line when one of the clan leaders logs in, and a golden
  "A Spartan has landed!" with a fanfare when uimfartqueef does. World hops are not announced. Worked out on your
  client from the clan channel; nothing is sent anywhere.
- **Daily gm** (with Clan sync on): your first gm of the day (UTC) in clan chat earns a pack's worth of CoR TCG
  points from the clan server, once a day. Gm's aren't counted or shown anywhere; it's just a boost for the card game.
- **Raid finder** (with Clan sync on): a card in the CoR side panel lists clanmates looking for a CoX, ToB or ToA
  party, with how many slots are filled (for example ToB 3/5), their world and a short note. Put up your own group
  there, update it as people join and take it down when you're full. While you are inside the raid it shows as
  raiding (and clanmates get a "party is raiding now" line); it comes down 30 minutes after you log out (others see
  you as "away" meanwhile). **Ping Discord** posts your group in the
  clan Discord's looking-for-group channel (once every 10 minutes). Open groups are advertised as game messages when one goes up and every
  5 minutes (setting "Raid finder in chat"); while the chat is quiet the ad updates its one line instead of
  adding more. These lines are shown by your own client only; the plugin never posts in the real clan chat.
- **CoX death sound** (setting, on by default, with a volume slider): when a player you can see dies in the Chambers
  of Xeric, a chicken squawks (the game's own chicken death sound), while you are in a clan. Each plugin user's
  client plays it for itself.
- **Kephri dung** (only on your client, while Show clan rank glows is on): when a clan leader (uimfartqueef,
  joeboob, Lavasockz or DAYLlGHT) types `::shame <name>`, Kephri's dung keeps dropping on that clanmate's head until a
  leader takes it off with `::unshame <name>` (it stays on, even through server restarts).
  Everyone with Clan sync on sees it, the shamed player too, and it can't be switched off on its own: only the
  Show clan rank glows master switch hides it. `::dungfx` drops it on your own character for 30 seconds, the way a shame does (type it
  again to stop).
- **Clan sync** (optional, off by default): clan-wide gz leaderboards, podiums, GZ King, weekly trophies and
  streaks, plus the chat icons, member icons, titles and rank icons clan staff set on the clan server, and
  everyone's glow picks.
- **CoR party** (optional, joined with the **Join CoR party** button in the CoR side panel): the clan's RuneLite
  party, used only so members can choose to show each other on the world map.

## Clan sync and your data

Clan sync is **off by default**. Turning it on shows RuneLite's warning that it submits your IP address to a
3rd-party server not controlled or verified by RuneLite developers.

**Your client only ever reports about your own character.** It never sends anything another player said or
did (the one other name it can send is the clanmate a clan leader types after `::shame`). While sync is on, the plugin sends to the CoR clan server (`cor-clan-api-production.up.railway.app`):

- the gz's (gz / grats / congrats) **you** say in your clan's chat,
- clan broadcasts **about you** (for example "Lavasockz has received a drop: ..."),
- your first gm of the day (the line **you** said, for example "gm all"), at most once a day, for the daily gm
  bonus. The server checks it is a gm and does not keep the text,
- your own clan rank and the glows you picked for yourself, on login and when they change,
- only if you put up a raid finder group: the raid, slots filled, party size, your world and your note, sent again
  every few minutes while it is up, whether you are inside the raid, and a "take it down" when you remove it. Ping Discord asks
  the clan server to post that same group in the clan Discord,
- your clan's rank names with their rank numbers (for example 126 "Owner", 5 "Captain"), once per login, so
  clan staff can pick a chat icon per rank. No player names are sent with them,
- only if you are one of the four clan leaders (uimfartqueef, joeboob, Lavasockz, DAYLlGHT): the name of the
  clanmate you `::shame` or `::unshame`, and the announcement text you save with the banner's Edit button, when
  you do it,
- your character name, your clan's name and your RuneLite account hash with each of those, so the server
  knows whose they are.

Nothing else from chat is sent: no other messages, nobody else's gz's or broadcasts, no private or public
chat. Gz's and broadcasts go out in small batches about every 30 seconds.

About every 2 minutes while you are logged in it downloads everyone's gz counts, clan ranks, icons, titles
and glow picks (as their own clients reported them or clan staff set them), the past weekly podiums, the
announcement banner, and the chat icon images from the clan's admin page (small PNGs, at most 32x16 pixels). About every 10 seconds it
downloads the short list of who is shamed with the Kephri dung right now.

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
members and your own counts. The four clan leaders can type `::shame <name>` and `::unshame <name>` (needs Clan
sync on); for anyone else they do nothing. Double-colon commands are handled inside RuneLite and are never sent to
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
