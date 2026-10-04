package world.bentobox.islandstats.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.islandstats.IslandStats;

/**
 * Removes an island's stats when the island is deleted, so the database does not fill up with
 * records for islands that no longer exist. Resetting an island makes a new island with a new
 * id, so the old island's delete covers resets as well.
 *
 * @author tastybento
 */
public class IslandListener implements Listener {

    private final IslandStats addon;

    public IslandListener(IslandStats addon) {
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDelete(IslandDeleteEvent e) {
        if (e.getIsland() != null) {
            addon.getManager().delete(e.getIsland().getUniqueId());
        }
    }
}
