package world.bentobox.islandstats.panels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.EntityType;
import org.eclipse.jdt.annotation.NonNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import world.bentobox.bentobox.api.dialogs.DialogBuilder;
import world.bentobox.bentobox.api.dialogs.DialogButton;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.islandstats.IslandStats;
import world.bentobox.islandstats.data.IslandStat;

/**
 * The island stats pages, shown as modal dialogs.
 * <p>
 * The first page is a summary with the total for each stat and a button to open it. A stat page
 * lists the counts for each mob, highest first, a page at a time. Mob names are sent as
 * translatable components so each player sees them in their own client language.
 *
 * @author tastybento
 */
public class StatsDialog {

    private static final String DIALOG = "islandstats.dialog.";
    private static final String MOB = "[mob]";
    private static final String STAT = "[stat]";

    private final IslandStats addon;
    private final Island island;
    private final String islandName;

    /**
     * @param addon      the addon
     * @param island     the island whose stats are shown
     * @param islandName the name to show in the title
     */
    public StatsDialog(@NonNull IslandStats addon, @NonNull Island island, @NonNull String islandName) {
        this.addon = addon;
        this.island = island;
        this.islandName = islandName;
    }

    /**
     * Show the summary page.
     *
     * @param user the player to show it to
     */
    public void showSummary(@NonNull User user) {
        summary(user).build().show(user);
    }

    /**
     * Lay out the summary page. Separate from {@link #showSummary(User)} so the layout can be
     * tested without a server to build the dialog.
     */
    DialogBuilder summary(User user) {
        DialogBuilder builder = new DialogBuilder().title(user, DIALOG + "title", TextVariables.NAME, islandName);
        for (IslandStat stat : IslandStat.values()) {
            builder.body(user, DIALOG + "summary-line", STAT, user.getTranslation(stat.getNameReference()),
                    TextVariables.NUMBER, format(addon.getManager().getTotal(island, stat)));
            builder.button(new DialogButton(text(user, stat.getNameReference()),
                    text(user, stat.getDescriptionReference()), u -> showStat(u, stat, 0)));
        }
        builder.button(DialogButton.of(user, DIALOG + "buttons.close", null));
        return builder;
    }

    /**
     * Show one page of a stat.
     *
     * @param user the player to show it to
     * @param stat the statistic
     * @param page the page, counting from 0. Out of range pages are clamped
     */
    public void showStat(@NonNull User user, @NonNull IslandStat stat, int page) {
        statPage(user, stat, page).build().show(user);
    }

    /**
     * Lay out one page of a stat.
     */
    DialogBuilder statPage(User user, IslandStat stat, int page) {
        List<Map.Entry<String, Long>> entries = addon.getManager().getSorted(island, stat);
        int pageSize = addon.getSettings().getPageSize();
        int pages = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        int current = Math.clamp(page, 0, pages - 1);

        DialogBuilder builder = new DialogBuilder().title(user, DIALOG + "stat-title", STAT,
                user.getTranslation(stat.getNameReference()));
        if (entries.isEmpty()) {
            builder.body(user, DIALOG + "empty");
        } else {
            if (pages > 1) {
                builder.body(user, DIALOG + "page", TextVariables.NUMBER, String.valueOf(current + 1),
                        "[total]", String.valueOf(pages));
            }
            int from = current * pageSize;
            List<Component> lines = new ArrayList<>();
            for (Map.Entry<String, Long> entry : entries.subList(from, Math.min(entries.size(), from + pageSize))) {
                lines.add(entryLine(user, entry.getKey(), entry.getValue()));
            }
            // One body block keeps the list tight rather than spacing each line out
            builder.body(Component.join(JoinConfiguration.newlines(), lines));
        }
        if (current > 0) {
            builder.button(DialogButton.of(user, DIALOG + "buttons.previous", u -> showStat(u, stat, current - 1)));
        }
        if (current < pages - 1) {
            builder.button(DialogButton.of(user, DIALOG + "buttons.next", u -> showStat(u, stat, current + 1)));
        }
        builder.button(DialogButton.of(user, DIALOG + "buttons.back", this::showSummary));
        builder.button(DialogButton.of(user, DIALOG + "buttons.close", null));
        return builder;
    }

    /**
     * Send the stats as chat lines. Used for the console, which cannot see a dialog.
     *
     * @param user the user to send them to
     */
    public void sendChat(@NonNull User user) {
        user.sendMessage("islandstats.chat.header", TextVariables.NAME, islandName);
        for (IslandStat stat : IslandStat.values()) {
            user.sendMessage("islandstats.chat.stat-header", STAT, user.getTranslation(stat.getNameReference()),
                    TextVariables.NUMBER, format(addon.getManager().getTotal(island, stat)));
            for (Map.Entry<String, Long> entry : addon.getManager().getSorted(island, stat)) {
                user.sendMessage("islandstats.chat.entry", MOB, Util.prettifyText(entry.getKey()),
                        TextVariables.NUMBER, format(entry.getValue()));
            }
        }
    }

    /**
     * Build one mob line. The locale entry is translated with the count filled in, and then the
     * mob placeholder is swapped for a translatable component, which a plain string cannot carry.
     */
    private Component entryLine(User user, String key, long count) {
        Component line = text(user, DIALOG + "entry", TextVariables.NUMBER, format(count));
        return line.replaceText(b -> b.matchLiteral(MOB).once().replacement(mobName(key)));
    }

    /**
     * @param key a stored entity type name
     * @return the mob name as a translatable component, or the prettified key if the type is no
     *         longer known to the server
     */
    static Component mobName(String key) {
        try {
            return Component.translatable(EntityType.valueOf(key).translationKey());
        } catch (IllegalArgumentException e) {
            return Component.text(Util.prettifyText(key));
        }
    }

    private static Component text(User user, String reference, String... variables) {
        return Util.parseMiniMessageOrLegacy(user.getTranslation(reference, variables));
    }

    private static String format(long number) {
        return String.format("%,d", number);
    }
}
