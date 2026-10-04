package world.bentobox.islandstats.commands;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.islandstats.IslandStats;
import world.bentobox.islandstats.panels.StatsDialog;

/**
 * {@code /<gamemode>admin stats <player> [reset]} - show, or erase, the stats of a player's
 * island. Players see the dialog; the console gets the stats as chat lines.
 *
 * @author tastybento
 */
public class AdminIslandStatsCommand extends ConfirmableCommand {

    private static final String RESET = "reset";

    private final IslandStats addon;
    private Island island;
    private String targetName;

    public AdminIslandStatsCommand(IslandStats addon, CompositeCommand parent) {
        super(addon, parent, "stats", "islandstats");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("admin.stats");
        setDescription("islandstats.commands.admin.description");
        setParametersHelp("islandstats.commands.admin.parameters");
        setOnlyPlayer(false);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.isEmpty() || args.size() > 2 || (args.size() == 2 && !RESET.equalsIgnoreCase(args.get(1)))) {
            showHelp(this, user);
            return false;
        }
        UUID target = getPlayers().getUUID(args.get(0));
        if (target == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        island = getIslands().getIsland(getWorld(), target);
        if (island == null) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        targetName = getPlayers().getName(target);
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() == 2) {
            Island toReset = island;
            String name = targetName;
            askConfirmation(user, user.getTranslation("islandstats.commands.admin.confirm-reset", TextVariables.NAME,
                    name), () -> {
                        addon.getManager().reset(toReset);
                        user.sendMessage("islandstats.commands.admin.reset-done", TextVariables.NAME, name);
                    });
            return true;
        }
        StatsDialog dialog = new StatsDialog(addon, island, addon.getIslandName(island));
        if (user.isPlayer()) {
            dialog.showSummary(user);
        } else {
            dialog.sendChat(user);
        }
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String last = args.isEmpty() ? "" : args.getLast();
        if (args.size() == 1) {
            List<String> names = Util.getOnlinePlayerList(user);
            return Optional.of(Util.tabLimit(names, last));
        }
        if (args.size() == 2) {
            return Optional.of(Util.tabLimit(List.of(RESET), last.toLowerCase(Locale.ENGLISH)));
        }
        return Optional.empty();
    }
}
