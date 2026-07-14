# TrueRTP

TrueRTP is a fresh Paper plugin inspired by the pain points people run into with older RTP plugins, not a fork of BetterRTP.

## Goals

- Stable teleport flow with bounded attempts and clear failure messages.
- Uniform random coordinates across a configurable min/max radius.
- Configurable countdown delay with editable words for command-based RTP.
- Portal RTP that triggers instantly when a player enters the portal region.
- Per-world enable/disable rules.
- World-specific center, min radius, max radius, min Y, and max Y.
- Debug logging that explains why candidates are rejected.

## Build

This project targets Paper and Java 21.

```bash
mvn clean package
```

The compiled plugin will be created at:

```text
target/TrueRTP-1.0.0.jar
```

## Commands

```text
/rtp
/rtp <world>
/rtp <player>
/rtp <player> <world>
/truertp reload
/truertp debug
/truertp portal create <portal> <target-world>
/truertp portal pos1 <portal>
/truertp portal pos2 <portal>
```

Portal `pos1` and `pos2` use the block the player is looking at within 6 blocks, falling back to the player's current block if no target block is found.

## Permissions

```text
truertp.use       Allows /rtp for yourself and portal RTP
truertp.others    Allows teleporting another player
truertp.reload    Allows /truertp reload
truertp.debug     Allows /truertp debug
```

## Randomness Note

TrueRTP samples a random angle plus a radius transformed with `sqrt(...)`. That matters because picking a raw radius directly would put too many teleports near the center. This gives each area of the configured ring an equal chance.