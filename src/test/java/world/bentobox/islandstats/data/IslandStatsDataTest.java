package world.bentobox.islandstats.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the per-island stats record.
 */
class IslandStatsDataTest {

    private IslandStatsData data;

    @BeforeEach
    void setUp() {
        data = new IslandStatsData("island-1");
    }

    @Test
    void testUniqueIdIsTheIslandId() {
        assertEquals("island-1", data.getUniqueId());
        data.setUniqueId("island-2");
        assertEquals("island-2", data.getUniqueId());
    }

    @Test
    void testAddAndGet() {
        data.add(IslandStat.KILL_ENTITY, "ZOMBIE", 1);
        data.add(IslandStat.KILL_ENTITY, "ZOMBIE", 2);
        data.add(IslandStat.KILL_ENTITY, "SKELETON", 5);

        assertEquals(3, data.get(IslandStat.KILL_ENTITY, "ZOMBIE"));
        assertEquals(5, data.get(IslandStat.KILL_ENTITY, "SKELETON"));
        assertEquals(0, data.get(IslandStat.KILL_ENTITY, "CREEPER"));
        // Stats are kept apart
        assertEquals(0, data.get(IslandStat.ENTITY_DEATH, "ZOMBIE"));
    }

    @Test
    void testTotal() {
        data.add(IslandStat.ENTITY_DEATH, "ZOMBIE", 10);
        data.add(IslandStat.ENTITY_DEATH, "COW", 4);

        assertEquals(14, data.getTotal(IslandStat.ENTITY_DEATH));
        assertEquals(0, data.getTotal(IslandStat.KILL_ENTITY));
    }

    @Test
    void testGetAllIsUnmodifiable() {
        data.add(IslandStat.ENTITY_DEATH, "ZOMBIE", 1);
        var all = data.getAll(IslandStat.ENTITY_DEATH);
        assertThrows(UnsupportedOperationException.class, () -> all.put("COW", 1L));
        assertTrue(data.getAll(IslandStat.KILL_ENTITY).isEmpty());
    }

    @Test
    void testClear() {
        data.add(IslandStat.ENTITY_DEATH, "ZOMBIE", 1);
        data.clear();
        assertEquals(0, data.getTotal(IslandStat.ENTITY_DEATH));
    }

    @Test
    void testNullStatsMapFromDatabaseIsSafe() {
        IslandStatsData loaded = new IslandStatsData();
        loaded.setStats(null);
        assertEquals(0, loaded.get(IslandStat.KILL_ENTITY, "ZOMBIE"));
        loaded.add(IslandStat.KILL_ENTITY, "ZOMBIE", 1);
        assertEquals(1, loaded.getStats().get("KILL_ENTITY").get("ZOMBIE"));
    }

    @Test
    void testStatKeys() {
        assertEquals("kill-entity", IslandStat.KILL_ENTITY.getKey());
        assertEquals("islandstats.stats.entity-death.name", IslandStat.ENTITY_DEATH.getNameReference());
        assertEquals("islandstats.stats.entity-death.description",
                IslandStat.ENTITY_DEATH.getDescriptionReference());
    }
}
