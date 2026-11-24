package legacyconnector.v1_6_4.net.minecraft.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The 1.6.4 id tables over a registry without the game ({@link Ids#useForTest}): claims by constructor id, the mirrored
 * {@code blocksList}/{@code itemsList} arrays, ids of unclaimed objects, nulling an entry to replace a vanilla block, and the
 * "is this config id free" checks mods make before choosing an id.
 */
class IdsTest {
    private static final IdRegistry.Resolver NO_OBJECTS = new IdRegistry.Resolver() {
        public Object block(String n) { return null; }
        public Object item(String n) { return null; }
        public Object biome(String f) { return null; }
        public Object potion(String f) { return null; }
        public Object enchantment(String f) { return null; }
    };

    private static final class TestBlock extends Block {
        TestBlock() {
            super(Material.rock);
        }
    }

    private List<String> log;

    @BeforeEach
    void freshTables() {
        log = new ArrayList<>();
        IdRegistry r = IdRegistry.vanilla(NO_OBJECTS).logTo(log::add);
        Ids.useForTest(r, new Block[IdRegistry.Kind.BLOCK.size], new Item[IdRegistry.Kind.ITEM.size]);
    }

    @Test
    void savedStackNumbersOfBothFormatsResolveAgainstTheIdSpace() {
        Item mod = new Item();
        Ids.claimItem(mod, 7000 - 256); // new format: the number of the mod's own space
        assertEquals(7000, Ids.claimedItemId(mod));
        assertSame(mod, Ids.item(7000));
        assertEquals(-1, Ids.claimedItemId(new Item())); // unclaimed: the writer keeps the host's bridge id (the old format)
        assertNull(Ids.item(100)); // below 4096 and unclaimed: nothing (never the bridge)
    }

    @Test
    void aClaimedBlockIsAtItsRequestedIdInBothTables() {
        TestBlock b = new TestBlock();
        Ids.claimBlock(b, 500);
        assertEquals(500, Ids.blockId(b));
        assertSame(b, Ids.block(500));
        assertSame(b, Ids.blocksList()[500]);
        assertFalse(Ids.blockIdFree(500));
    }

    @Test
    void claimingTwiceKeepsTheFirstId() {
        TestBlock b = new TestBlock();
        Ids.claimBlock(b, 501);
        Ids.claimBlock(b, 502);
        assertEquals(501, Ids.blockId(b));
        assertTrue(Ids.blockIdFree(502));
    }

    @Test
    void aTakenIdMovesTheNewBlockAndLogs() {
        TestBlock first = new TestBlock();
        TestBlock second = new TestBlock();
        Ids.claimBlock(first, 600);
        Ids.claimBlock(second, 600);
        assertEquals(600, Ids.blockId(first));
        int moved = Ids.blockId(second);
        assertTrue(moved > 0 && moved != 600, "a free id was chosen: " + moved);
        assertSame(second, Ids.block(moved));
        assertFalse(log.isEmpty(), "the conflict is reported");
    }

    @Test
    void anUnclaimedBlockGetsTheNextFreeIdWhenAsked() {
        TestBlock b = new TestBlock();
        assertEquals(-1, Ids.claimedBlockId(b), "lookups never claim");
        int id = Ids.blockId(b);
        assertTrue(id > 0);
        assertEquals(id, Ids.claimedBlockId(b));
        assertSame(b, Ids.block(id));
    }

    @Test
    void nullAndOutOfRangeBlocksAreAir() {
        assertEquals(0, Ids.blockId(null));
        assertEquals(0, Ids.claimedBlockId(null));
        assertNull(Ids.block(0));
        assertNull(Ids.block(-1));
        assertNull(Ids.block(Ids.blockTableSize()));
        assertNull(Ids.block(Integer.MAX_VALUE));
    }

    @Test
    void anItemIsAt256PlusItsConstructorId() {
        Item item = new Item();
        Ids.claimItem(item, 3000 - 256);
        assertEquals(3000, Ids.itemId(item));
        assertSame(item, Ids.item(3000));
        assertSame(item, Ids.itemsList()[3000]);
        assertFalse(Ids.itemIdFree(3000));
        assertTrue(Ids.itemIdFree(3001));
    }

    @Test
    void itemIdsOutOfRangeAreNullAndNeverFree() {
        assertNull(Ids.item(-1));
        assertNull(Ids.item(Ids.itemTableSize()));
        assertFalse(Ids.itemIdFree(0));
        assertFalse(Ids.itemIdFree(-5));
        assertFalse(Ids.itemIdFree(Ids.itemTableSize()));
        assertFalse(Ids.blockIdFree(0));
        assertFalse(Ids.blockIdFree(Ids.blockTableSize()));
        assertEquals(0, Ids.itemId(null));
    }

    @Test
    void aModThatNullsABlocksListEntryFreesTheIdForItsOwnBlock() {
        TestBlock vanillaLike = new TestBlock();
        Ids.claimBlock(vanillaLike, 700);
        Ids.blocksList()[700] = null; // blocksList[700] = null before constructing the replacement
        assertTrue(Ids.blockIdFree(700), "a nulled entry counts as free");
        TestBlock replacement = new TestBlock();
        Ids.claimBlock(replacement, 700);
        assertEquals(700, Ids.blockId(replacement));
        assertSame(replacement, Ids.block(700));
    }

    @Test
    void typeFilterReturnsNullForForeignWrappers() {
        assertEquals("x", Ids.as(String.class, "x"));
        assertNull(Ids.as(Integer.class, "x"));
        assertNull(Ids.as(String.class, null));
    }
}
