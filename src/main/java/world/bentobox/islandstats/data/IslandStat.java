package world.bentobox.islandstats.data;

import java.util.Locale;

/**
 * The statistics kept for each island. Where there is a matching vanilla player statistic the
 * name is the same as {@link org.bukkit.Statistic}, so the island figure can be read as "the
 * sum of that statistic for everything that happened on the island".
 * <p>
 * The enum name is the key the counts are stored under in the database, so constants must not
 * be renamed once released. New statistics can be added freely.
 *
 * @author tastybento
 */
public enum IslandStat {
    /**
     * A mob was killed by a player on the island. Same meaning as
     * {@link org.bukkit.Statistic#KILL_ENTITY}, counted per entity type.
     */
    KILL_ENTITY,
    /**
     * A mob died on the island from any cause - players, mob farms, the environment. Counted per
     * entity type.
     */
    ENTITY_DEATH;

    /**
     * @return the key used for this stat in locale files and placeholders, e.g. {@code kill-entity}
     */
    public String getKey() {
        return name().toLowerCase(Locale.ENGLISH).replace('_', '-');
    }

    /**
     * @return the locale reference for this stat's display name
     */
    public String getNameReference() {
        return "islandstats.stats." + getKey() + ".name";
    }

    /**
     * @return the locale reference for this stat's description
     */
    public String getDescriptionReference() {
        return "islandstats.stats." + getKey() + ".description";
    }
}
