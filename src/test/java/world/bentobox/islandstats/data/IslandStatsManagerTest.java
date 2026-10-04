package world.bentobox.islandstats.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.islandstats.CommonTestSetup;
import world.bentobox.islandstats.IslandStats;

/**
 * Tests the stats manager: lazy loading, counting, saving and deleting.
 */
class IslandStatsManagerTest extends CommonTestSetup {

    @Mock
    private IslandStats addon;
    @Mock
    private world.bentobox.bentobox.Settings pluginSettings;

    private AbstractDatabaseHandler<Object> h;
    private MockedStatic<DatabaseSetup> mockDb;
    private IslandStatsManager manager;

    @SuppressWarnings("unchecked")
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        h = mock(AbstractDatabaseHandler.class);
        mockDb = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        mockDb.when(DatabaseSetup::getDatabase).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(h);
        when(h.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
        when(h.saveObjectNow(any())).thenReturn(CompletableFuture.completedFuture(true));
        when(plugin.getSettings()).thenReturn(pluginSettings);
        when(pluginSettings.getDatabaseType()).thenReturn(DatabaseType.JSON);
        when(addon.getPlugin()).thenReturn(plugin);
        when(addon.getLogger()).thenReturn(java.util.logging.Logger.getLogger("IslandStatsManagerTest"));

        when(island.getUniqueId()).thenReturn("island-1");
        manager = new IslandStatsManager(addon);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        mockDb.closeOnDemand();
        super.tearDown();
    }

    @Test
    void testNewIslandStartsEmptyWithoutLoading() throws Exception {
        when(h.objectExists("island-1")).thenReturn(false);

        IslandStatsData data = manager.getStats(island);

        assertEquals("island-1", data.getUniqueId());
        assertEquals(0, data.getTotal(IslandStat.ENTITY_DEATH));
        verify(h, never()).loadObject(anyString());
    }

    @Test
    void testExistingStatsAreLoadedOnceAndCached() throws Exception {
        IslandStatsData stored = new IslandStatsData("island-1");
        stored.add(IslandStat.KILL_ENTITY, "ZOMBIE", 7);
        when(h.objectExists("island-1")).thenReturn(true);
        when(h.loadObject("island-1")).thenReturn(stored);

        assertSame(stored, manager.getStats(island));
        assertSame(stored, manager.getStats(island));

        verify(h, times(1)).loadObject("island-1");
        assertEquals(7, manager.getCount(island, IslandStat.KILL_ENTITY, EntityType.ZOMBIE));
        assertEquals(1, manager.getCacheSize());
    }

    @Test
    void testIncrementCountsAndMarksDirty() {
        assertFalse(manager.hasUnsavedChanges());

        manager.increment(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE);
        manager.increment(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE);
        manager.increment(island, IslandStat.ENTITY_DEATH, EntityType.COW);

        assertEquals(2, manager.getCount(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE));
        assertEquals(3, manager.getTotal(island, IslandStat.ENTITY_DEATH));
        assertTrue(manager.hasUnsavedChanges());
    }

    @Test
    void testNullIslandCountsAreZero() {
        assertEquals(0, manager.getCount(null, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE));
        assertEquals(0, manager.getTotal(null, IslandStat.ENTITY_DEATH));
    }

    @Test
    void testSortedHighestFirstThenByName() {
        manager.increment(island, IslandStat.ENTITY_DEATH, EntityType.COW);
        manager.increment(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE);
        manager.increment(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE);
        manager.increment(island, IslandStat.ENTITY_DEATH, EntityType.CHICKEN);

        List<Map.Entry<String, Long>> sorted = manager.getSorted(island, IslandStat.ENTITY_DEATH);

        assertEquals(List.of("ZOMBIE", "CHICKEN", "COW"), sorted.stream().map(Map.Entry::getKey).toList());
    }

    @Test
    void testSaveDirtyOnlySavesChangedRecords() throws Exception {
        manager.increment(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE);

        manager.saveDirty();
        // Nothing has changed since, so a second save writes nothing
        manager.saveDirty();

        verify(h, times(1)).saveObject(any());
        assertFalse(manager.hasUnsavedChanges());
    }

    @Test
    void testCloseSavesNowAndEmptiesCache() throws Exception {
        manager.increment(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE);

        manager.close();

        verify(h).saveObjectNow(any());
        assertEquals(0, manager.getCacheSize());
    }

    @Test
    void testReset() throws Exception {
        manager.increment(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE);

        manager.reset(island);

        assertEquals(0, manager.getTotal(island, IslandStat.ENTITY_DEATH));
        assertFalse(manager.hasUnsavedChanges());
        verify(h).saveObject(any());
    }

    @Test
    void testDelete() throws Exception {
        manager.increment(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE);
        when(h.objectExists("island-1")).thenReturn(true);

        manager.delete("island-1");

        assertEquals(0, manager.getCacheSize());
        assertFalse(manager.hasUnsavedChanges());
        verify(h).deleteID("island-1");
    }

    @Test
    void testDeleteOfUnknownIslandDoesNotTouchDatabase() throws Exception {
        when(h.objectExists("island-1")).thenReturn(false);

        manager.delete("island-1");

        verify(h, never()).deleteID(anyString());
    }
}
