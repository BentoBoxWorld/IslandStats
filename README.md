# IslandStats

An addon for [BentoBox](https://bentobox.world) that keeps statistics for each **island**
rather than for each player. It works with every game mode (BSkyBlock, AcidIsland, CaveBlock,
OneBlock, ...) and is not a game mode itself.

To start with it counts mob deaths: how many of each mob has died on an island, and how many of
those a player killed.

## Statistics

| Stat | Meaning |
| --- | --- |
| `KILL_ENTITY` | A mob was killed by a player on the island. Same meaning as the vanilla `KILL_ENTITY` player statistic. |
| `ENTITY_DEATH` | A mob died on the island from any cause, including mob farms, fall damage and lava. |

Both are counted per entity type. A death counts for the island whose protected area it happened
in. Players and armor stands are not counted (the ignored list is configurable).

### Why events and not player statistics

Piggybacking on vanilla player statistics (`PlayerStatisticIncrementEvent`) was considered. It
only fires when a player is the killer and it says nothing about where the mob was, so it would
miss every mob farm and would credit the killer's island rather than the island the mob died on.
IslandStats listens to `EntityDeathEvent` at `MONITOR` priority instead, and keeps the handler
cheap: it returns early for players, ignored types and non-game-mode worlds, then does one island
grid lookup and an in-memory map update.

## Storage

Stats are stored with the BentoBox database API (whatever database BentoBox is set up to use),
one `IslandStatsData` record per island, keyed by the island's unique id
(with the JSON database these are in `plugins/BentoBox/database/IslandStatsData/`). A record is loaded the first
time its island is needed and then cached. Changes are written every `save-interval` minutes and
when the server stops. When an island is deleted or reset, its stats are deleted too.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/<gamemode> stats` | `<gamemode>.island.stats` (default: true) | Open your island's stats dialog. Must be used in that game mode's world. |
| `/<gamemode>admin stats <player>` | `<gamemode>.admin.stats` (default: op) | Show a player's island stats. Players get the dialog, the console gets chat. |
| `/<gamemode>admin stats <player> reset` | `<gamemode>.admin.stats` | Erase a player's island stats, after confirmation. |

The stats are shown as dialog pages (BentoBox Dialogs API): a summary with the total for each
stat, then a page per stat listing each mob, highest first, with Previous and Next buttons. Mob
names are sent as translatable text, so each player sees them in their own client language.

## Placeholders

For each game mode, with PlaceholderAPI they look like `%IslandStats_bskyblock_island_kill_entity_zombie%`.

| Placeholder | Value |
| --- | --- |
| `<gamemode>_island_<stat>_total` | Total for the player's island |
| `<gamemode>_island_<stat>_<entity>` | Count of one mob for the player's island |
| `<gamemode>_visited_island_<stat>_total` | Total for the island the player is standing on |

`<stat>` is `kill_entity` or `entity_death`; `<entity>` is the lower case entity type, such as
`zombie` or `iron_golem`.

## Configuration

See `config.yml`:

* `disabled-gamemodes` - game modes to leave alone
* `count-visitor-kills` - whether kills by non-members count toward `KILL_ENTITY` (default true)
* `ignored-entities` - entity types never counted (default `ARMOR_STAND`)
* `save-interval` - minutes between database saves (default 5)
* `dialog.page-size` - mob lines per dialog page (default 15)

## For addon developers

```java
IslandStats stats = (IslandStats) BentoBox.getInstance().getAddonsManager().getAddonByName("IslandStats").orElseThrow();
long zombies = stats.getManager().getCount(island, IslandStat.KILL_ENTITY, EntityType.ZOMBIE);
long allDeaths = stats.getManager().getTotal(island, IslandStat.ENTITY_DEATH);
```

## Requirements

* Paper 26.2 or later (Java 25)
* BentoBox 3.23.0 or later

## Building

```
mvn clean package
```
