# CoR Clan

A RuneLite plugin for members of the **C o R** clan.

Everything runs locally inside your own RuneLite client, and **no data leaves your computer**.

## Features

- **Clan sidebar** with the clan logo, Discord and website buttons, gz leaderboards, an all-time and a
  weekly gz podium, weekly #1 streaks and an org chart of clan members by rank.
- **Custom clan chat icons** (only on your client): CoR's own icons replace the default clan rank icon
  next to names in clan chat, and specific members can be given a special icon from the config.
- **GZ tracker**: counts gz / grats / congrats messages in clan chat and attributes them to whoever the
  latest clan broadcast (drop, level, pet, ...) was about, for a short configurable window. Stats are
  stored in your RuneLite settings.

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
| Chat icons | GZ King icon | Give the top all-time gz giver a special icon |
| Chat icons | Weekly top 3 trophies | This week's top 3 gz givers (on your client, resets Sunday) get a gold, silver or bronze trophy next to their name |
| GZ tracker | Track gz's | Master switch |
| GZ tracker | GZ window | Seconds after a broadcast during which gz's count for that member (default 90) |
| GZ tracker | One gz per person | Only the first gz per member per broadcast counts as "received" |
| GZ tracker | Include guest clan chat | Also track the guest clan channel |
| GZ tracker | Show gz overlay | On-screen box with your own counts, the open gz window with a countdown, and a "+1" flash when a gz is counted |
| GZ tracker | Show [GZ count] on gz lines | Append `[GZ count: N]` to a member's chat line when they say gz, N being their all-time total on your client |
| GZ tracker | Chat message when counted | Also print a local game message per counted gz (off by default) |
| GZ tracker | Max gz message length | Longer messages are never treated as a gz |

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
