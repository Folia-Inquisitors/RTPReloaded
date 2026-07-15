# RTPReloaded

RTPReloaded is a generic rtp plugin that has the core features of popular RTP plugins. It's is not bloated, it is version stable, and has been tested. This rtp plugin also truly randomly teleports you into random locations.

## Build instructions

```bash
mvn clean package
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

##  Portal instructions 

Portal `pos1` and `pos2` use the block the player is looking at within 6 blocks, falling back to the player's current block if no target block is found.

## Permissions

```text
rtpreloaded.use       Allows /rtp for yourself and portal RTP
rtpreloaded.others    Allows teleporting another player
rtpreloaded.reload    Allows /rtpreloaded reload
rtpreloaded.debug     Allows /rtpreloaded debug
rtpreloaded.portal    Allows managing portals
```

## Randomness Note

RTPReloaded samples a random angle plus a radius transformed with `sqrt(...)`. That matters because picking a raw radius directly would put too many teleports near the center. This gives each area of the configured ring an equal chance.

## Folia Inquisitors

[<img src="https://github.com/Folia-Inquisitors.png" width="80" alt="Folia-Inquisitors">](https://github.com/orgs/Folia-Inquisitors/repositories)
[<img src="https://github.com/Yomamaeatstoes.png" width="80" alt="Yomamaeatstoes">](https://github.com/Yomamaeatstoes)
