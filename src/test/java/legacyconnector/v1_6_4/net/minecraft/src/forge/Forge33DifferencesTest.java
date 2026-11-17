package legacyconnector.v1_6_4.net.minecraft.src.forge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.ModKeys125;

/** Forge 3.3 and ModLoader 1.2.5 behaviour the shared 1.7.10 code differs in, taken from the real sources, without a game. */
class Forge33DifferencesTest {
    @Test
    void applyArmorKeepsTheFractionOfAHeartAsCarryOverDamage() {
        Object entity = new Object();
        // Forge 3.3: damage += carry; carry = (int) damage % 25; return (int) (damage / 25)   (damage in 1/25 heart units)
        assertEquals(0, ArmorProperties.withCarry(entity, 10.0));   // carry 10
        assertEquals(0, ArmorProperties.withCarry(entity, 10.0));   // 20 -> carry 20
        assertEquals(1, ArmorProperties.withCarry(entity, 10.0));   // 30 -> returns 1, carry 5
        assertEquals(2, ArmorProperties.withCarry(entity, 45.0));   // 5 + 45 = 50 -> returns 2, carry 0
    }

    @Test
    void carryIsPerEntity() {
        Object a = new Object();
        Object b = new Object();
        assertEquals(0, ArmorProperties.withCarry(a, 20.0));
        assertEquals(0, ArmorProperties.withCarry(b, 20.0));
        assertEquals(1, ArmorProperties.withCarry(a, 20.0));   // 40 -> 1, carry 15
        assertEquals(1, ArmorProperties.withCarry(b, 20.0));
    }

    @Test
    void minecartCollisionHandlerOfForge33IsTheTypeTheMinecartCodeInstalls() {
        assertTrue(legacyconnector.v1_7_10.net.minecraftforge.common.IMinecartCollisionHandler.class.isAssignableFrom(IMinecartCollisionHandler.class));
    }

    @Test
    void modKeyFiresOnPressAndRepeatsOnlyWithAllowRepeatAndOnlyWithAScreenOpen() {
        AtomicBoolean down = new AtomicBoolean();
        AtomicInteger once = new AtomicInteger();
        AtomicInteger repeat = new AtomicInteger();
        ModKeys125.Key k1 = new ModKeys125.Key(once::incrementAndGet, false, down::get);
        ModKeys125.Key k2 = new ModKeys125.Key(repeat::incrementAndGet, true, down::get);
        down.set(true);
        k1.step(false);
        k2.step(false);                                    // no screen: the layer's key handler fires, not this
        assertEquals(0, once.get());
        k1.step(true);
        k2.step(true);                                     // held since before the screen opened: not a new press for k1
        assertEquals(0, once.get());
        assertEquals(1, repeat.get());
        k2.step(true);
        assertEquals(2, repeat.get());
        down.set(false);
        k1.step(true);
        k2.step(true);
        down.set(true);
        k1.step(true);                                     // a new press with a screen open
        assertEquals(1, once.get());
        k1.step(true);                                     // held: no repeat for a key without allowRepeat
        assertEquals(1, once.get());
    }

    @Test
    void achievementPagesAreTheShimsOwnAndNamesAreUnique() {
        AchievementPage page = new AchievementPage("Forge33Test");
        MinecraftForge.registerAchievementPage(page);
        assertSame(page, MinecraftForge.getAchievementPage("Forge33Test"));
        assertSame(page, legacyconnector.v1_7_10.net.minecraftforge.common.AchievementPage.getAchievementPage("Forge33Test"));
        RuntimeException e = assertThrows(RuntimeException.class, () -> MinecraftForge.registerAchievementPage(new AchievementPage("Forge33Test")));
        assertEquals("Duplicate achievement page name \"Forge33Test\"!", e.getMessage());
    }
}
