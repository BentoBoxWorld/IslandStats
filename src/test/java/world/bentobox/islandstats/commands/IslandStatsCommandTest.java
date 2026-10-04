package world.bentobox.islandstats.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;

import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.islandstats.CommonTestSetup;
import world.bentobox.islandstats.IslandStats;

/**
 * Tests the player stats command.
 */
class IslandStatsCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand parent;
    @Mock
    private User user;
    @Mock
    private IslandStats addon;

    private IslandStatsCommand command;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        when(addon.getPlugin()).thenReturn(plugin);
        when(parent.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(parent.getWorld()).thenReturn(world);
        when(parent.getPermissionPrefix()).thenReturn("bskyblock.");
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getWorld()).thenReturn(world);
        mockedUtil.when(() -> Util.sameWorld(world, world)).thenReturn(true);
        command = new IslandStatsCommand(addon, parent);
    }

    @Test
    void testSetup() {
        assertEquals("bskyblock.island.stats", command.getPermission());
        assertTrue(command.isOnlyPlayer());
        assertEquals("islandstats.commands.player.description", command.getDescription());
    }

    @Test
    void testNoIsland() {
        when(im.getIsland(world, user)).thenReturn(null);

        assertFalse(command.canExecute(user, "stats", List.of()));

        verify(user).sendMessage("general.errors.no-island");
    }

    @Test
    void testWrongWorld() {
        World other = mock(World.class);
        when(user.getWorld()).thenReturn(other);
        mockedUtil.when(() -> Util.sameWorld(world, other)).thenReturn(false);

        assertFalse(command.canExecute(user, "stats", List.of()));

        verify(user).sendMessage("general.errors.wrong-world");
        verify(im, never()).getIsland(any(World.class), any(User.class));
    }

    @Test
    void testHasIsland() {
        when(im.getIsland(world, user)).thenReturn(island);

        assertTrue(command.canExecute(user, "stats", List.of()));
    }

    @Test
    void testArgumentsShowHelp() {
        assertFalse(command.canExecute(user, "stats", List.of("extra")));

        verify(im, never()).getIsland(any(World.class), any(User.class));
    }
}
