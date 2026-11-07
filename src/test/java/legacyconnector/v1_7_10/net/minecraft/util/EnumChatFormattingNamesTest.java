package legacyconnector.v1_7_10.net.minecraft.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import org.junit.jupiter.api.Test;

/** {@code EnumChatFormatting.getValidValues}, which 1.6.4 mods reach as {@code func_96296_a} through the forward table. */
class EnumChatFormattingNamesTest {
    @Test
    void colorsOnlyLeavesOutStyles() {
        Collection<String> names = EnumChatFormatting.getValidValues(true, false);
        assertTrue(names.contains("red"));
        assertTrue(names.contains("aqua"));
        assertFalse(names.contains("bold"));
        // reset is neither a color nor a style, so it passes both filters (as in vanilla)
        assertTrue(names.contains("reset"));
        assertEquals(17, names.size());
    }

    @Test
    void stylesOnlyLeavesOutColors() {
        Collection<String> names = EnumChatFormatting.getValidValues(false, true);
        assertTrue(names.contains("bold"));
        assertTrue(names.contains("italic"));
        assertFalse(names.contains("red"));
    }

    @Test
    void neitherGivesOnlyWhatIsNeitherColorNorStyle() {
        // reset is neither a color nor a style: the only entry that survives both filters
        assertEquals(java.util.List.of("reset"), new java.util.ArrayList<>(EnumChatFormatting.getValidValues(false, false)));
    }
}
