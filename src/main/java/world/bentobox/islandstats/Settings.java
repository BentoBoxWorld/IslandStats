package world.bentobox.islandstats;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;

/**
 * IslandStats addon settings, saved to and loaded from {@code addons/IslandStats/config.yml}.
 *
 * @author tastybento
 */
@StoreAt(filename = "config.yml", path = "addons/IslandStats")
public class Settings implements ConfigObject {

    @ConfigComment("IslandStats addon configuration file")
    @ConfigComment("")
    @ConfigComment("IslandStats keeps statistics for each island rather than for each player,")
    @ConfigComment("such as how many of each mob has been killed on the island.")
    @ConfigComment("")
    @ConfigComment("Game modes listed here are ignored by IslandStats. Example:")
    @ConfigComment("disabled-gamemodes:")
    @ConfigComment("  - BSkyBlock")
    @ConfigEntry(path = "disabled-gamemodes")
    private Set<String> disabledGameModes = new HashSet<>();

    @ConfigComment("")
    @ConfigComment("Count mobs killed on an island by players who are not members of it,")
    @ConfigComment("such as visitors or coops. This only affects the 'killed by players' stat.")
    @ConfigComment("Every mob death on the island is always counted in the 'all mob deaths' stat.")
    @ConfigEntry(path = "count-visitor-kills")
    private boolean countVisitorKills = true;

    @ConfigComment("")
    @ConfigComment("Entity types that are never counted. Use the Bukkit entity type names.")
    @ConfigComment("Armor stands are living entities to the server, so they are ignored by default.")
    @ConfigEntry(path = "ignored-entities")
    private List<String> ignoredEntities = List.of("ARMOR_STAND");

    @ConfigComment("")
    @ConfigComment("How often, in minutes, changed stats are written to the database.")
    @ConfigComment("Stats are counted in memory and always saved when the server stops.")
    @ConfigComment("Minimum 1.")
    @ConfigEntry(path = "save-interval")
    private int saveInterval = 5;

    @ConfigComment("")
    @ConfigComment("How many mob lines to show on each page of the stats dialog. Range 1 to 50.")
    @ConfigEntry(path = "dialog.page-size")
    private int pageSize = 15;

    /**
     * @return the names of game modes that IslandStats ignores
     */
    public Set<String> getDisabledGameModes() {
        return disabledGameModes;
    }

    /**
     * @param disabledGameModes the names of game modes that IslandStats ignores
     */
    public void setDisabledGameModes(Set<String> disabledGameModes) {
        this.disabledGameModes = disabledGameModes;
    }

    /**
     * @return true if kills by non-members count toward the island's player kill stat
     */
    public boolean isCountVisitorKills() {
        return countVisitorKills;
    }

    /**
     * @param countVisitorKills true if kills by non-members count toward the island's player kill stat
     */
    public void setCountVisitorKills(boolean countVisitorKills) {
        this.countVisitorKills = countVisitorKills;
    }

    /**
     * @return entity type names that are never counted
     */
    public List<String> getIgnoredEntities() {
        return ignoredEntities;
    }

    /**
     * @param ignoredEntities entity type names that are never counted
     */
    public void setIgnoredEntities(List<String> ignoredEntities) {
        this.ignoredEntities = ignoredEntities;
    }

    /**
     * @return minutes between database saves, at least 1
     */
    public int getSaveInterval() {
        return Math.max(1, saveInterval);
    }

    /**
     * @param saveInterval minutes between database saves
     */
    public void setSaveInterval(int saveInterval) {
        this.saveInterval = saveInterval;
    }

    /**
     * @return mob lines per dialog page, 1 to 50
     */
    public int getPageSize() {
        return Math.clamp(pageSize, 1, 50);
    }

    /**
     * @param pageSize mob lines per dialog page
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
