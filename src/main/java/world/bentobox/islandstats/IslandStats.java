package world.bentobox.islandstats;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.islandstats.commands.AdminIslandStatsCommand;
import world.bentobox.islandstats.commands.IslandStatsCommand;
import world.bentobox.islandstats.data.IslandStatsManager;
import world.bentobox.islandstats.listeners.EntityDeathListener;
import world.bentobox.islandstats.listeners.IslandListener;

/**
 * IslandStats addon entry point.
 * <p>
 * Keeps statistics for each island rather than for each player - to start with, how many of each
 * mob has died on the island, and how many of those a player killed. The stats are stored in the
 * BentoBox database, one record per island, and shown with {@code /<gamemode> stats} as dialog
 * pages and through placeholders.
 *
 * @author tastybento
 */
public class IslandStats extends Addon {

    private Settings settings;
    private final Config<Settings> config = new Config<>(this, Settings.class);
    private final @NonNull List<GameModeAddon> gameModes = new ArrayList<>();
    private IslandStatsManager manager;
    private EntityDeathListener deathListener;
    private BukkitTask saveTask;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        loadSettings();
    }

    @Override
    public void onEnable() {
        if (getState() == State.DISABLED) {
            return;
        }
        manager = new IslandStatsManager(this);
        StatsPlaceholders placeholders = new StatsPlaceholders(this);
        gameModes.clear();
        getPlugin().getAddonsManager().getGameModeAddons().stream()
                .filter(gm -> !settings.getDisabledGameModes().contains(gm.getDescription().getName()))
                .forEach(gm -> {
                    gameModes.add(gm);
                    log("IslandStats hooking into " + gm.getDescription().getName());
                    gm.getPlayerCommand().ifPresent(c -> new IslandStatsCommand(this, c));
                    gm.getAdminCommand().ifPresent(c -> new AdminIslandStatsCommand(this, c));
                    placeholders.register(gm);
                });

        if (gameModes.isEmpty()) {
            logWarning("IslandStats is not hooked into any game mode, so it will do nothing.");
            return;
        }

        deathListener = new EntityDeathListener(this);
        registerListener(deathListener);
        registerListener(new IslandListener(this));
        startSaveTask();
    }

    @Override
    public void onReload() {
        loadSettings();
        if (settings == null) {
            stopSaveTask();
            return;
        }
        if (deathListener != null) {
            deathListener.loadIgnored();
            startSaveTask();
        }
    }

    @Override
    public void onDisable() {
        stopSaveTask();
        if (manager != null) {
            manager.close();
        }
    }

    /**
     * Start, or restart, the repeating task that saves changed stats.
     */
    private void startSaveTask() {
        stopSaveTask();
        long period = settings.getSaveInterval() * 60L * 20L;
        saveTask = Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> manager.saveDirty(), period, period);
    }

    private void stopSaveTask() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
    }

    private void loadSettings() {
        settings = config.loadConfigObject();
        if (settings == null) {
            logError("IslandStats settings could not load! Addon disabled.");
            setState(State.DISABLED);
            return;
        }
        config.saveConfigObject(settings);
    }

    /**
     * @return the addon settings, or null if they have not loaded
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * @return the island stats manager, or null if the addon is not enabled. Other addons can use
     *         this to read island stats.
     */
    public IslandStatsManager getManager() {
        return manager;
    }

    /**
     * @return the game modes this addon is hooked into
     */
    public List<GameModeAddon> getGameModes() {
        return gameModes;
    }

    /**
     * @param world world to check
     * @return true if IslandStats is counting in this world
     */
    public boolean inGameWorld(World world) {
        if (world == null) {
            return false;
        }
        for (GameModeAddon gm : gameModes) {
            if (gm.inWorld(world)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The name to show for an island: its own name if it has one, otherwise its owner's name.
     *
     * @param island the island
     * @return the display name, never null
     */
    @NonNull
    public String getIslandName(@NonNull Island island) {
        if (island.getName() != null && !island.getName().isBlank()) {
            return island.getName();
        }
        if (island.getOwner() != null) {
            String name = getPlayers().getName(island.getOwner());
            if (name != null) {
                return name;
            }
        }
        return "";
    }
}
