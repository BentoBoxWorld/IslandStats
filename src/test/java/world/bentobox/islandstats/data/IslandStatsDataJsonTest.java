package world.bentobox.islandstats.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The database stores records as JSON with Gson, using only exposed fields. Check that counts
 * survive the round trip as longs.
 */
class IslandStatsDataJsonTest {

    @Test
    void testRoundTrip() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        IslandStatsData data = new IslandStatsData("island-1");
        data.add(IslandStat.KILL_ENTITY, "ZOMBIE", 5_000_000_000L);
        data.add(IslandStat.ENTITY_DEATH, "COW", 2);

        IslandStatsData loaded = gson.fromJson(gson.toJson(data), IslandStatsData.class);

        assertEquals("island-1", loaded.getUniqueId());
        assertEquals(5_000_000_000L, loaded.get(IslandStat.KILL_ENTITY, "ZOMBIE"));
        assertEquals(2, loaded.getTotal(IslandStat.ENTITY_DEATH));
        // Loaded maps must still accept new counts
        loaded.add(IslandStat.ENTITY_DEATH, "COW", 1);
        assertEquals(3, loaded.get(IslandStat.ENTITY_DEATH, "COW"));
    }
}
