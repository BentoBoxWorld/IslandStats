package world.bentobox.islandstats.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import world.bentobox.bentobox.api.dialogs.DialogBuilder;
import world.bentobox.bentobox.api.dialogs.DialogButton;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.islandstats.CommonTestSetup;
import world.bentobox.islandstats.IslandStats;
import world.bentobox.islandstats.Settings;
import world.bentobox.islandstats.data.IslandStat;
import world.bentobox.islandstats.data.IslandStatsManager;

/**
 * Tests the stats dialog pages and the chat version used by the console.
 */
class StatsDialogTest extends CommonTestSetup {

    @Mock
    private IslandStats addon;
    @Mock
    private IslandStatsManager manager;

    private StatsDialog dialog;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(addon.getManager()).thenReturn(manager);
        when(addon.getSettings()).thenReturn(new Settings());
        when(manager.getTotal(any(), any())).thenReturn(3L);
        when(manager.getSorted(any(), any()))
                .thenReturn(List.of(Map.entry("ZOMBIE", 2L), Map.entry("GONE_IN_A_LATER_VERSION", 1L)));
        dialog = new StatsDialog(addon, island, "tastybento");
    }

    @Test
    void testMobNameIsTranslatable() {
        TranslatableComponent name = assertInstanceOf(TranslatableComponent.class, StatsDialog.mobName("ZOMBIE"));
        assertEquals("entity.minecraft.zombie", name.key());
    }

    @Test
    void testUnknownMobNameFallsBackToText() {
        TextComponent name = assertInstanceOf(TextComponent.class, StatsDialog.mobName("GONE_IN_A_LATER_VERSION"));
        assertEquals("Gone In A Later Version", name.content());
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> field(DialogBuilder builder, String name) throws ReflectiveOperationException {
        Field f = DialogBuilder.class.getDeclaredField(name);
        f.setAccessible(true);
        return (List<T>) f.get(builder);
    }

    private static List<String> buttonLabels(DialogBuilder builder) throws ReflectiveOperationException {
        return StatsDialogTest.<DialogButton>field(builder, "buttons").stream()
                .map(b -> PlainTextComponentSerializer.plainText().serialize(b.label())).toList();
    }

    @Test
    void testSummaryHasALineAndButtonPerStatPlusClose() throws ReflectiveOperationException {
        DialogBuilder builder = dialog.summary(User.getInstance(mockPlayer));

        assertEquals(IslandStat.values().length, field(builder, "body").size());
        assertEquals(List.of(IslandStat.KILL_ENTITY.getNameReference(), IslandStat.ENTITY_DEATH.getNameReference(),
                "islandstats.dialog.buttons.close"), buttonLabels(builder));
    }

    @Test
    void testSinglePageHasNoPaging() throws ReflectiveOperationException {
        DialogBuilder builder = dialog.statPage(User.getInstance(mockPlayer), IslandStat.ENTITY_DEATH, 0);

        // Just the mob list, no page line
        assertEquals(1, field(builder, "body").size());
        assertEquals(List.of("islandstats.dialog.buttons.back", "islandstats.dialog.buttons.close"),
                buttonLabels(builder));
    }

    @Test
    void testPaging() throws ReflectiveOperationException {
        Settings settings = new Settings();
        settings.setPageSize(1);
        when(addon.getSettings()).thenReturn(settings);
        User user = User.getInstance(mockPlayer);

        DialogBuilder first = dialog.statPage(user, IslandStat.ENTITY_DEATH, 0);
        // Page line and the mob list
        assertEquals(2, field(first, "body").size());
        assertEquals(List.of("islandstats.dialog.buttons.next", "islandstats.dialog.buttons.back",
                "islandstats.dialog.buttons.close"), buttonLabels(first));

        // Past the end is clamped to the last page
        DialogBuilder last = dialog.statPage(user, IslandStat.ENTITY_DEATH, 99);
        assertEquals(List.of("islandstats.dialog.buttons.previous", "islandstats.dialog.buttons.back",
                "islandstats.dialog.buttons.close"), buttonLabels(last));
    }

    @Test
    void testEmptyStat() throws ReflectiveOperationException {
        when(manager.getSorted(any(), any())).thenReturn(List.of());

        DialogBuilder builder = dialog.statPage(User.getInstance(mockPlayer), IslandStat.KILL_ENTITY, 0);

        assertEquals(1, field(builder, "body").size());
        verify(lm).get(any(), org.mockito.ArgumentMatchers.eq("islandstats.dialog.empty"));
    }

    @Test
    void testSendChat() {
        CommandSender console = mock(CommandSender.class);
        User user = User.getInstance(console);

        dialog.sendChat(user);

        verify(lm).get(any(), org.mockito.ArgumentMatchers.eq("islandstats.chat.header"));
        // Two mob lines for each stat
        verify(lm, times(4)).get(any(), org.mockito.ArgumentMatchers.eq("islandstats.chat.entry"));
    }
}
