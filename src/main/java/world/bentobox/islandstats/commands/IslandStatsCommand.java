package world.bentobox.islandstats.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.islandstats.IslandStats;
import world.bentobox.islandstats.panels.StatsDialog;

/**
 * {@code /<gamemode> stats} - show the player's island stats in a dialog.
 *
 * @author tastybento
 */
public class IslandStatsCommand extends CompositeCommand {

    private final IslandStats addon;
    private Island island;

    public IslandStatsCommand(IslandStats addon, CompositeCommand parent) {
        super(addon, parent, "stats", "islandstats");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("island.stats");
        setDescription("islandstats.commands.player.description");
        setOnlyPlayer(true);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        // Without this, /is stats typed in another game mode's world shows the stats of an
        // island the player is not looking at, if they have one in both
        if (!Util.sameWorld(getWorld(), user.getWorld())) {
            user.sendMessage("general.errors.wrong-world");
            return false;
        }
        island = getIslands().getIsland(getWorld(), user);
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        new StatsDialog(addon, island, addon.getIslandName(island)).showSummary(user);
        return true;
    }
}
