package world.bentobox.islandstats.data;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.EntityType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandstats.IslandStats;

/**
 * Owns the island stats: a cache of the records in the database, the counting, and saving.
 * <p>
 * Counting happens on every mob death, so it only ever touches memory. A record is loaded from
 * the database the first time its island is needed, after which it stays cached. Changed records
 * are marked dirty and written out by {@link #saveDirty()}, which the addon runs on a timer, and
 * by {@link #close()} when the server stops.
 *
 * @author tastybento
 */
public class IslandStatsManager {

    private final Database<IslandStatsData> handler;

    /**
     * Records by island unique id. Concurrent because placeholder requests can arrive off the
     * main thread; all counting is done on the main thread.
     */
    private final Map<String, IslandStatsData> cache = new ConcurrentHashMap<>();

    /** Island ids whose records have changed since they were last saved. */
    private final Set<String> dirty = ConcurrentHashMap.newKeySet();

    public IslandStatsManager(IslandStats addon) {
        this.handler = new Database<>(addon, IslandStatsData.class);
    }

    /**
     * Get the stats for an island, loading them from the database if they are not cached.
     *
     * @param island the island
     * @return the island's stats, never null
     */
    @NonNull
    public IslandStatsData getStats(@NonNull Island island) {
        return getStats(island.getUniqueId());
    }

    /**
     * Get the stats for an island, loading them from the database if they are not cached.
     *
     * @param islandId the island's unique id
     * @return the island's stats, never null
     */
    @NonNull
    public IslandStatsData getStats(@NonNull String islandId) {
        return cache.computeIfAbsent(islandId, this::load);
    }

    private IslandStatsData load(String islandId) {
        IslandStatsData data = handler.objectExists(islandId) ? handler.loadObject(islandId) : null;
        return data == null ? new IslandStatsData(islandId) : data;
    }

    /**
     * Count one more for a stat on an island.
     *
     * @param island the island
     * @param stat   the statistic
     * @param type   the entity type counted
     */
    public void increment(@NonNull Island island, @NonNull IslandStat stat, @NonNull EntityType type) {
        getStats(island).add(stat, type.name(), 1);
        dirty.add(island.getUniqueId());
    }

    /**
     * @param island the island, or null
     * @param stat   the statistic
     * @param type   the entity type
     * @return the count, or 0 if there is no island
     */
    public long getCount(@Nullable Island island, @NonNull IslandStat stat, @NonNull EntityType type) {
        return island == null ? 0 : getStats(island).get(stat, type.name());
    }

    /**
     * @param island the island, or null
     * @param stat   the statistic
     * @return the sum of every count for the stat, or 0 if there is no island
     */
    public long getTotal(@Nullable Island island, @NonNull IslandStat stat) {
        return island == null ? 0 : getStats(island).getTotal(stat);
    }

    /**
     * @param island the island
     * @param stat   the statistic
     * @return every non-zero count for the stat, highest first
     */
    public List<Map.Entry<String, Long>> getSorted(@NonNull Island island, @NonNull IslandStat stat) {
        return getStats(island).getAll(stat).entrySet().stream().filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
    }

    /**
     * Erase every stat for an island and save the empty record.
     *
     * @param island the island
     */
    public void reset(@NonNull Island island) {
        IslandStatsData data = getStats(island);
        data.clear();
        dirty.remove(island.getUniqueId());
        handler.saveObjectAsync(data);
    }

    /**
     * Forget an island completely, from the cache and the database. Used when the island is
     * deleted.
     *
     * @param islandId the island's unique id
     */
    public void delete(@NonNull String islandId) {
        cache.remove(islandId);
        dirty.remove(islandId);
        if (handler.objectExists(islandId)) {
            handler.deleteID(islandId);
        }
    }

    /**
     * Write every changed record to the database. The records are serialized on the calling
     * thread and written asynchronously, so this must be called on the main thread.
     */
    public void saveDirty() {
        Iterator<String> it = dirty.iterator();
        while (it.hasNext()) {
            IslandStatsData data = cache.get(it.next());
            it.remove();
            if (data != null) {
                handler.saveObjectAsync(data);
            }
        }
    }

    /**
     * Save every changed record and wait for the writes to finish. Used when the addon is
     * disabled, when there is no later chance to retry.
     */
    public void close() {
        for (String id : dirty) {
            IslandStatsData data = cache.get(id);
            if (data != null) {
                handler.saveObjectNow(data);
            }
        }
        dirty.clear();
        cache.clear();
    }

    /**
     * @return the number of island records held in memory
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * @return true if there are changes waiting to be saved
     */
    public boolean hasUnsavedChanges() {
        return !dirty.isEmpty();
    }
}
