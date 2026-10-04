package world.bentobox.islandstats.listeners;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandstats.IslandStats;
import world.bentobox.islandstats.data.IslandStat;

/**
 * Counts mob deaths on islands.
 * <p>
 * This listens to {@link EntityDeathEvent} rather than piggybacking on player statistics,
 * because the player statistic event only fires when a player is the killer and carries no
 * location for the mob. Mob farms kill with fall damage, lava and the like, and the island a
 * mob belongs to is where it died, not where the killer stood.
 * <p>
 * The handler runs on every mob death on the server, so it bails out as early and as cheaply as
 * it can: players and ignored types first, then the world, then one island grid lookup. The count
 * itself is an in-memory map update.
 *
 * @author tastybento
 */
public class EntityDeathListener implements Listener {

    private final IslandStats addon;
    private final Set<EntityType> ignored = EnumSet.noneOf(EntityType.class);

    public EntityDeathListener(IslandStats addon) {
        this.addon = addon;
        loadIgnored();
    }

    /**
     * Read the ignored entity types from the settings. Called again when the settings reload.
     */
    public void loadIgnored() {
        ignored.clear();
        for (String name : addon.getSettings().getIgnoredEntities()) {
            try {
                ignored.add(EntityType.valueOf(name.toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException e) {
                addon.logWarning("Unknown entity type in ignored-entities: " + name);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        if (entity instanceof Player || ignored.contains(entity.getType())
                || !addon.inGameWorld(entity.getWorld())) {
            return;
        }
        addon.getIslands().getProtectedIslandAt(entity.getLocation()).ifPresent(island -> count(island, entity));
    }

    private void count(Island island, LivingEntity entity) {
        EntityType type = entity.getType();
        addon.getManager().increment(island, IslandStat.ENTITY_DEATH, type);
        Player killer = entity.getKiller();
        if (killer != null && (addon.getSettings().isCountVisitorKills()
                || island.getMemberSet().contains(killer.getUniqueId()))) {
            addon.getManager().increment(island, IslandStat.KILL_ENTITY, type);
        }
    }
}
