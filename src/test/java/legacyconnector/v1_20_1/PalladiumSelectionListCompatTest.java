package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PalladiumSelectionListCompatTest {
    @Test
    void unsetPreferencesKeepTheLegacyEnabledDefaults() {
        Object list = new Object();
        assertTrue(PalladiumSelectionListCompat.renderBackground(list));
        assertTrue(PalladiumSelectionListCompat.renderTopAndBottom(list));
        assertTrue(PalladiumSelectionListCompat.renderSelection(list));
    }

    @Test
    void eachLegacyPreferenceCanBeChangedIndependently() {
        Object list = new Object();

        PalladiumSelectionListCompat.setRenderBackground(list, false);
        assertFalse(PalladiumSelectionListCompat.renderBackground(list));
        assertTrue(PalladiumSelectionListCompat.renderTopAndBottom(list));
        assertTrue(PalladiumSelectionListCompat.renderSelection(list));

        PalladiumSelectionListCompat.setRenderTopAndBottom(list, false);
        assertFalse(PalladiumSelectionListCompat.renderBackground(list));
        assertFalse(PalladiumSelectionListCompat.renderTopAndBottom(list));
        assertTrue(PalladiumSelectionListCompat.renderSelection(list));

        PalladiumSelectionListCompat.setRenderSelection(list, false);
        assertFalse(PalladiumSelectionListCompat.renderBackground(list));
        assertFalse(PalladiumSelectionListCompat.renderTopAndBottom(list));
        assertFalse(PalladiumSelectionListCompat.renderSelection(list));

        PalladiumSelectionListCompat.setRenderSelection(list, true);
        assertTrue(PalladiumSelectionListCompat.renderSelection(list));
        assertFalse(PalladiumSelectionListCompat.renderBackground(list));
        assertFalse(PalladiumSelectionListCompat.renderTopAndBottom(list));
    }

    @Test
    void preferencesAreScopedToEachListInstance() {
        Object first = new Object();
        Object second = new Object();

        PalladiumSelectionListCompat.setRenderBackground(first, false);
        PalladiumSelectionListCompat.setRenderTopAndBottom(second, false);

        assertFalse(PalladiumSelectionListCompat.renderBackground(first));
        assertTrue(PalladiumSelectionListCompat.renderTopAndBottom(first));
        assertTrue(PalladiumSelectionListCompat.renderSelection(first));

        assertTrue(PalladiumSelectionListCompat.renderBackground(second));
        assertFalse(PalladiumSelectionListCompat.renderTopAndBottom(second));
        assertTrue(PalladiumSelectionListCompat.renderSelection(second));
    }
}
