package world.bentobox.islandstats.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

/**
 * The statistics for one island. The unique id is the island's unique id.
 * <p>
 * Counts are held as stat name to sub-key to count, where the stat name is an
 * {@link IslandStat} name and the sub-key says what was counted - an entity type name such as
 * {@code ZOMBIE} for the mob stats. Plain strings are used rather than enums so that a stat or
 * an entity type that a later server version drops still loads.
 *
 * @author tastybento
 */
@Table(name = "IslandStats")
public class IslandStatsData implements DataObject {

    @Expose
    private String uniqueId;

    @Expose
    private Map<String, Map<String, Long>> stats = new HashMap<>();

    /**
     * Required by the database for loading.
     */
    public IslandStatsData() {
        // Required by the database
    }

    /**
     * @param islandId the unique id of the island these stats are for
     */
    public IslandStatsData(String islandId) {
        this.uniqueId = islandId;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Add to a count.
     *
     * @param stat   the statistic
     * @param subKey what was counted, such as an entity type name
     * @param amount how much to add
     */
    public void add(IslandStat stat, String subKey, long amount) {
        getStatsMap().computeIfAbsent(stat.name(), k -> new HashMap<>()).merge(subKey, amount, Long::sum);
    }

    /**
     * @param stat   the statistic
     * @param subKey what was counted, such as an entity type name
     * @return the count, or 0 if nothing has been counted
     */
    public long get(IslandStat stat, String subKey) {
        Map<String, Long> counts = getStatsMap().get(stat.name());
        return counts == null ? 0 : counts.getOrDefault(subKey, 0L);
    }

    /**
     * @param stat the statistic
     * @return every count for the stat, keyed by sub-key. Unmodifiable, may be empty
     */
    public Map<String, Long> getAll(IslandStat stat) {
        Map<String, Long> counts = getStatsMap().get(stat.name());
        return counts == null ? Collections.emptyMap() : Collections.unmodifiableMap(counts);
    }

    /**
     * @param stat the statistic
     * @return the sum of every count for the stat
     */
    public long getTotal(IslandStat stat) {
        return getAll(stat).values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * Forget every count.
     */
    public void clear() {
        getStatsMap().clear();
    }

    /**
     * @return the raw stats map, for the database
     */
    public Map<String, Map<String, Long>> getStats() {
        return getStatsMap();
    }

    /**
     * @param stats the raw stats map, for the database
     */
    public void setStats(Map<String, Map<String, Long>> stats) {
        this.stats = stats;
    }

    private Map<String, Map<String, Long>> getStatsMap() {
        // A record saved with no stats can load with a null map
        if (stats == null) {
            stats = new HashMap<>();
        }
        return stats;
    }
}
