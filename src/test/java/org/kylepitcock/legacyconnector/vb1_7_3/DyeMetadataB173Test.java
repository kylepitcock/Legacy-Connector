package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import legacyconnector.vb1_7_3.net.minecraft.block.WoolBlock;
import legacyconnector.vb1_7_3.net.minecraft.item.DyeItem;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

class DyeMetadataB173Test {
    @Test void everyWoolColorRoundTripsThroughDyeMetadata() {
        var seen = new HashSet<Integer>();
        for (int wool = 0; wool < 16; wool++) {
            int dye = WoolBlock.getItemMeta(wool);
            assertEquals(15 - wool, dye);
            assertEquals(wool, WoolBlock.getBlockMeta(dye));
            assertTrue(seen.add(dye));
            assertNotNull(DyeItem.names[dye]);
        }
        assertEquals("white", DyeItem.names[WoolBlock.getItemMeta(0)]);
        assertEquals("black", DyeItem.names[WoolBlock.getItemMeta(15)]);
        assertEquals(15, WoolBlock.getBlockMeta(16));
        assertEquals(0, WoolBlock.getItemMeta(-1));
    }

    @Test void betaTablesRetainOriginalKeysAndPalette() {
        assertEquals(16, DyeItem.names.length);
        assertEquals(16, new HashSet<>(java.util.List.of(DyeItem.names)).size());
        assertEquals("silver", DyeItem.names[7]);
        assertEquals("lightBlue", DyeItem.names[12]);
        assertEquals(16, DyeItem.colors.length);
        assertEquals(1973019, DyeItem.colors[0]);
        assertEquals(15790320, DyeItem.colors[15]);
        assertEquals(2651799, DyeItem.colors[7]);
    }

    @Test void dyeSubtypeNamesAndIconsUseEveryMetadataValue() {
        int[] mark = ContentB173.mark();
        try {
            var dye = new DyeItem(30000);
            dye.setTranslationKey("testDye");
            dye.setTextureId(0);
            var icons = new HashSet<Integer>();
            for (int meta = 0; meta < 16; meta++) {
                assertEquals("item.testDye." + DyeItem.names[meta], dye.getTranslationKey(new ItemStack(dye, 1, meta)));
                assertTrue(icons.add(dye.getTextureId(meta)));
            }
        } finally {
            ContentB173.release(mark);
        }
    }
}
