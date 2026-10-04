package world.bentobox.islandstats.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.islandstats.CommonTestSetup;
import world.bentobox.islandstats.IslandStats;
import world.bentobox.islandstats.data.IslandStatsManager;

/**
 * Tests the admin stats command.
 */
class AdminIslandStatsCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand parent;
    @Mock
    private User user;
    @Mock
    private IslandStats addon;
    @Mock
    private IslandStatsManager manager;
    @Mock
    private PlayersManager pm;

    private final UUID target = UUID.randomUUID();
    private AdminIslandStatsCommand command;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        when(plugin.getPlayers()).thenReturn(pm);
        when(addon.getPlugin()).thenReturn(plugin);
        when(addon.getManager()).thenReturn(manager);
        when(addon.getIslandName(any())).thenReturn("someone");
        when(parent.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(parent.getWorld()).thenReturn(world);
        when(parent.getPermissionPrefix()).thenReturn("bskyblock.");
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));

        when(pm.getUUID("someone")).thenReturn(target);
        when(pm.getName(target)).thenReturn("someone");
        when(im.getIsland(world, target)).thenReturn(island);
        command = new AdminIslandStatsCommand(addon, parent);
    }

    @Test
    void testSetup() {
        assertEquals("bskyblock.admin.stats", command.getPermission());
        assertFalse(command.isOnlyPlayer());
    }

    @Test
    void testBadArguments() {
        assertFalse(command.canExecute(user, "stats", List.of()));
        assertFalse(command.canExecute(user, "stats", List.of("someone", "wipe")));
        assertFalse(command.canExecute(user, "stats", List.of("someone", "reset", "now")));
    }

    @Test
    void testUnknownPlayer() {
        assertFalse(command.canExecute(user, "stats", List.of("nobody")));

        verify(user).sendMessage("general.errors.unknown-player", "[name]", "nobody");
    }

    @Test
    void testPlayerWithNoIsland() {
        when(im.getIsland(world, target)).thenReturn(null);

        assertFalse(command.canExecute(user, "stats", List.of("someone")));

        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    @Test
    void testConsoleGetsChat() {
        when(user.isPlayer()).thenReturn(false);
        when(manager.getSorted(any(), any())).thenReturn(List.of());

        assertTrue(command.canExecute(user, "stats", List.of("someone")));
        assertTrue(command.execute(user, "stats", List.of("someone")));

        verify(user).sendMessage("islandstats.chat.header", "[name]", "someone");
    }

    @Test
    void testExecuteWithoutCanExecute() {
        assertFalse(command.execute(user, "stats", List.of("someone")));
    }

    @Test
    void testResetCanExecute() {
        assertTrue(command.canExecute(user, "stats", List.of("someone", "RESET")));
    }

    @Test
    void testTabComplete() {
        assertEquals(Optional.of(List.of("reset")), command.tabComplete(user, "stats", List.of("someone", "r")));
        assertEquals(Optional.empty(), command.tabComplete(user, "stats", List.of("someone", "reset", "")));
    }
}
