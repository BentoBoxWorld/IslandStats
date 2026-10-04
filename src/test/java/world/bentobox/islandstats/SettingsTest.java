package world.bentobox.islandstats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests the settings defaults and bounds.
 */
class SettingsTest {

    private final Settings settings = new Settings();

    @Test
    void testDefaults() {
        assertTrue(settings.getDisabledGameModes().isEmpty());
        assertTrue(settings.isCountVisitorKills());
        assertEquals(List.of("ARMOR_STAND"), settings.getIgnoredEntities());
        assertEquals(5, settings.getSaveInterval());
        assertEquals(15, settings.getPageSize());
    }

    @Test
    void testSetters() {
        settings.setDisabledGameModes(Set.of("BSkyBlock"));
        settings.setCountVisitorKills(false);
        settings.setIgnoredEntities(List.of("BAT"));
        assertEquals(Set.of("BSkyBlock"), settings.getDisabledGameModes());
        assertFalse(settings.isCountVisitorKills());
        assertEquals(List.of("BAT"), settings.getIgnoredEntities());
    }

    @Test
    void testSaveIntervalAtLeastOneMinute() {
        settings.setSaveInterval(0);
        assertEquals(1, settings.getSaveInterval());
        settings.setSaveInterval(10);
        assertEquals(10, settings.getSaveInterval());
    }

    @Test
    void testPageSizeClamped() {
        settings.setPageSize(0);
        assertEquals(1, settings.getPageSize());
        settings.setPageSize(500);
        assertEquals(50, settings.getPageSize());
    }
}
