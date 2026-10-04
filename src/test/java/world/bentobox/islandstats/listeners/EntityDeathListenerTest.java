package world.bentobox.islandstats.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.google.common.collect.ImmutableSet;

import world.bentobox.islandstats.CommonTestSetup;
import world.bentobox.islandstats.IslandStats;
import world.bentobox.islandstats.Settings;
import world.bentobox.islandstats.data.IslandStat;
import world.bentobox.islandstats.data.IslandStatsManager;

/**
 * Tests which mob deaths are counted, and against which stat.
 */
class EntityDeathListenerTest extends CommonTestSetup {

    @Mock
    private IslandStats addon;
    @Mock
    private IslandStatsManager manager;
    @Mock
    private LivingEntity zombie;
    @Mock
    private Player killer;

    private Settings settings;
    private EntityDeathListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        settings = new Settings();
        when(addon.getSettings()).thenReturn(settings);
        when(addon.getManager()).thenReturn(manager);
        when(addon.getIslands()).thenReturn(im);
        when(addon.inGameWorld(world)).thenReturn(true);
        when(im.getProtectedIslandAt(location)).thenReturn(Optional.of(island));

        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        when(zombie.getWorld()).thenReturn(world);
        when(zombie.getLocation()).thenReturn(location);

        when(killer.getUniqueId()).thenReturn(uuid);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid));

        listener = new EntityDeathListener(addon);
    }

    private EntityDeathEvent deathOf(LivingEntity entity) {
        return new EntityDeathEvent(entity, mock(DamageSource.class), new ArrayList<>());
    }

    @Test
    void testFarmDeathCountsOnlyAsDeath() {
        listener.onEntityDeath(deathOf(zombie));

        verify(manager).increment(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE);
        verify(manager, never()).increment(island, IslandStat.KILL_ENTITY, EntityType.ZOMBIE);
    }

    @Test
    void testPlayerKillCountsAsBoth() {
        when(zombie.getKiller()).thenReturn(killer);

        listener.onEntityDeath(deathOf(zombie));

        verify(manager).increment(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE);
        verify(manager).increment(island, IslandStat.KILL_ENTITY, EntityType.ZOMBIE);
    }

    @Test
    void testVisitorKillCountsByDefault() {
        when(killer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(zombie.getKiller()).thenReturn(killer);

        listener.onEntityDeath(deathOf(zombie));

        verify(manager).increment(island, IslandStat.KILL_ENTITY, EntityType.ZOMBIE);
    }

    @Test
    void testVisitorKillNotCountedWhenTurnedOff() {
        settings.setCountVisitorKills(false);
        when(killer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(zombie.getKiller()).thenReturn(killer);

        listener.onEntityDeath(deathOf(zombie));

        verify(manager).increment(island, IslandStat.ENTITY_DEATH, EntityType.ZOMBIE);
        verify(manager, never()).increment(island, IslandStat.KILL_ENTITY, EntityType.ZOMBIE);
    }

    @Test
    void testPlayerDeathIgnored() {
        when(mockPlayer.getLocation()).thenReturn(location);

        listener.onEntityDeath(deathOf(mockPlayer));

        verify(manager, never()).increment(any(), any(), any());
    }

    @Test
    void testIgnoredEntityType() {
        LivingEntity stand = mock(LivingEntity.class);
        when(stand.getType()).thenReturn(EntityType.ARMOR_STAND);
        when(stand.getWorld()).thenReturn(world);
        when(stand.getLocation()).thenReturn(location);

        listener.onEntityDeath(deathOf(stand));

        verify(manager, never()).increment(any(), any(), any());
    }

    @Test
    void testIgnoredListReloads() {
        settings.setIgnoredEntities(List.of("zombie", "NOT_A_MOB"));
        listener.loadIgnored();

        listener.onEntityDeath(deathOf(zombie));

        verify(manager, never()).increment(any(), any(), any());
        verify(addon).logWarning("Unknown entity type in ignored-entities: NOT_A_MOB");
    }

    @Test
    void testNotInGameWorld() {
        when(addon.inGameWorld(world)).thenReturn(false);

        listener.onEntityDeath(deathOf(zombie));

        verify(manager, never()).increment(any(), any(), any());
    }

    @Test
    void testNotOnAnIsland() {
        when(im.getProtectedIslandAt(location)).thenReturn(Optional.empty());

        listener.onEntityDeath(deathOf(zombie));

        verify(manager, never()).increment(any(), any(), any());
    }
}
