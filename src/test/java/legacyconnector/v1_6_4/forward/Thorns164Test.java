package legacyconnector.v1_6_4.forward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** The thorns rules of 1.6.4: 15% per level to reflect, damage 1 to 4 up to level 10 and level - 10 above. */
class Thorns164Test {
    @Test
    void noLevelNeverReflects() {
        Random rand = new Random(1);
        for (int i = 0; i < 1000; i++) assertFalse(Thorns164.shouldHit(0, rand));
    }

    @Test
    void chanceGrowsWithTheLevel() {
        Random rand = new Random(42);
        int hitsLevel1 = 0, hitsLevel3 = 0;
        for (int i = 0; i < 20000; i++) {
            if (Thorns164.shouldHit(1, rand)) hitsLevel1++;
            if (Thorns164.shouldHit(3, rand)) hitsLevel3++;
        }
        assertEquals(0.15, hitsLevel1 / 20000.0, 0.02);
        assertEquals(0.45, hitsLevel3 / 20000.0, 0.02);
        assertTrue(Thorns164.shouldHit(7, new Random() {
            @Override
            public float nextFloat() {
                return 0.99F;   // 15% * 7 = 105%: always
            }
        }));
    }

    @Test
    void damageRangeAndHighLevelOffset() {
        Random rand = new Random(7);
        boolean sawOne = false, sawFour = false;
        for (int i = 0; i < 2000; i++) {
            int d = Thorns164.damage(3, rand);
            assertTrue(d >= 1 && d <= 4, "damage " + d);
            sawOne |= d == 1;
            sawFour |= d == 4;
        }
        assertTrue(sawOne && sawFour);
        assertEquals(1, Thorns164.damage(11, rand));
        assertEquals(15, Thorns164.damage(25, rand));
        assertTrue(Thorns164.damage(10, rand) <= 4);
    }
}
