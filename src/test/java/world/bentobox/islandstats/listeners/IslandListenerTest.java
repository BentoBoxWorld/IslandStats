package world.bentobox.islandstats.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandstats.CommonTestSetup;
import world.bentobox.islandstats.IslandStats;
import world.bentobox.islandstats.data.IslandStatsManager;

/**
 * Island stats must not outlive their island.
 */
class IslandListenerTest extends CommonTestSetup {

    @Mock
    private IslandStats addon;
    @Mock
    private IslandStatsManager manager;

    private IslandListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(addon.getManager()).thenReturn(manager);
        listener = new IslandListener(addon);
    }

    @Test
    void testIslandDeleteRemovesItsStats() {
        // The event copies the island it is given, so use a real one with a known id
        Island deleted = new Island(location, UUID.randomUUID(), 100);

        listener.onIslandDelete(new IslandDeleteEvent(deleted, UUID.randomUUID(), false, location));

        verify(manager).delete(deleted.getUniqueId());
    }

    @Test
    void testIslandDeleteWithNoIslandIsSafe() {
        listener.onIslandDelete(new IslandDeleteEvent(null, UUID.randomUUID(), false, location));

        verify(manager, never()).delete(any());
    }
}
