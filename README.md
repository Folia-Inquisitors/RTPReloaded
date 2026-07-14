# RTPReloaded

RTPReloaded is a fresh Paper plugin inspired by the pain points people run into with older RTP plugins, not a fork of BetterRTP.

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
target/RTPReloaded-1.0.0.jar
```

## Commands

```text
/rtp
/rtp <world>
/rtp <player>
/rtp <player> <world>
/rtpreloaded reload
/rtpreloaded debug
/rtpreloaded portal create <portal> <target-world>
/rtpreloaded portal pos1 <portal>
/rtpreloaded portal pos2 <portal>
/truertp reload              Legacy alias for /rtpreloaded reload
```

Portal `pos1` and `pos2` use the block the player is looking at within 6 blocks, falling back to the player's current block if no target block is found.

Command labels are normalized case-insensitively, so `/rtP`, `/RTP`, and `/Rtp` all resolve to `/rtp`.

## Permissions

```text
rtpreloaded.use       Allows /rtp for yourself and portal RTP
rtpreloaded.others    Allows teleporting another player
rtpreloaded.reload    Allows /rtpreloaded reload
rtpreloaded.debug     Allows /rtpreloaded debug
rtpreloaded.portal    Allows managing portals
```

Legacy `truertp.*` permission nodes are still accepted for existing setups.

## Randomness Note

RTPReloaded samples a random angle plus a radius transformed with `sqrt(...)`. That matters because picking a raw radius directly would put too many teleports near the center. This gives each area of the configured ring an equal chance.