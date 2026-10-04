package world.bentobox.islandstats;

import java.util.Locale;

import org.bukkit.entity.EntityType;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.islandstats.data.IslandStat;

/**
 * Registers the island stats placeholders for a game mode. With BSkyBlock and PlaceholderAPI they
 * look like {@code %IslandStats_bskyblock_island_kill_entity_zombie%}:
 * <ul>
 * <li>{@code <gamemode>_island_<stat>_total} - the total for the player's island</li>
 * <li>{@code <gamemode>_island_<stat>_<entity>} - the count of one mob for the player's island</li>
 * <li>{@code <gamemode>_visited_island_<stat>_total} - the total for the island the player is on</li>
 * </ul>
 * where {@code <stat>} is {@code kill_entity} or {@code entity_death} and {@code <entity>} is the
 * lower case entity type, e.g. {@code zombie}.
 *
 * @author tastybento
 */
public class StatsPlaceholders {

    private final IslandStats addon;

    public StatsPlaceholders(IslandStats addon) {
        this.addon = addon;
    }

    /**
     * Register every placeholder for a game mode.
     *
     * @param gm the game mode
     */
    public void register(GameModeAddon gm) {
        PlaceholdersManager pm = addon.getPlugin().getPlaceholdersManager();
        if (pm == null) {
            return;
        }
        String prefix = gm.getDescription().getName().toLowerCase(Locale.ENGLISH) + "_";
        for (IslandStat stat : IslandStat.values()) {
            String statKey = stat.name().toLowerCase(Locale.ENGLISH);
            pm.registerPlaceholder(addon, prefix + "island_" + statKey + "_total",
                    "Island total: " + statKey.replace('_', ' '),
                    user -> String.valueOf(addon.getManager().getTotal(getIsland(gm, user), stat)));
            pm.registerPlaceholder(addon, prefix + "visited_island_" + statKey + "_total",
                    "Total for the island the player is on: " + statKey.replace('_', ' '),
                    user -> String.valueOf(addon.getManager().getTotal(getVisitedIsland(gm, user), stat)));
            for (EntityType type : EntityType.values()) {
                if (type.isAlive() && type != EntityType.PLAYER) {
                    String name = type.name().toLowerCase(Locale.ENGLISH);
                    pm.registerPlaceholder(addon, prefix + "island_" + statKey + "_" + name,
                            "Island " + statKey.replace('_', ' ') + ": " + name,
                            user -> String.valueOf(addon.getManager().getCount(getIsland(gm, user), stat, type)));
                }
            }
        }
    }

    private Island getIsland(GameModeAddon gm, User user) {
        if (user == null || user.getUniqueId() == null) {
            return null;
        }
        return addon.getIslands().getIsland(gm.getOverWorld(), user);
    }

    private Island getVisitedIsland(GameModeAddon gm, User user) {
        if (user == null || !user.isPlayer() || user.getLocation() == null || !gm.inWorld(user.getWorld())) {
            return null;
        }
        return addon.getIslands().getProtectedIslandAt(user.getLocation()).orElse(null);
    }
}
