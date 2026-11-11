package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ChestSpritesB173Test {
    @Test void everyChestIndexYieldsAnOpaque16x16Sprite() {
        for (int index = 25; index <= 27; index++) {
            var sprite = ChestSpritesB173.sprite(index);
            assertEquals(16, sprite.getWidth());assertEquals(16, sprite.getHeight());
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) assertEquals(255, sprite.getRGB(x, y) >>> 24, index + "@" + x + "," + y);
        }
        assertNull(ChestSpritesB173.sprite(30));
    }
    @Test void spritesMatchBetaTerrainPixels() {
        int[][] expected = {{0x2B261F, 0xA76E1F}, {0x463E32, 0x926323}, {0x463E32, 0x614116}};
        for (int i = 0; i < 3; i++) {
            var sprite = ChestSpritesB173.sprite(25 + i);
            assertEquals(0xFF000000 | expected[i][0], sprite.getRGB(0, 0));
            assertEquals(0xFF000000 | expected[i][1], sprite.getRGB(8, 7));
        }
    }
}
