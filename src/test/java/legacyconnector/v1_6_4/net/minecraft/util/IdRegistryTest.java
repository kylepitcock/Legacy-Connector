package legacyconnector.v1_6_4.net.minecraft.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_6_4.net.minecraft.util.IdRegistry.Kind;
import org.junit.jupiter.api.Test;

class IdRegistryTest {
    private static final IdRegistry.Resolver NAMES = new IdRegistry.Resolver() {
        public Object block(String n) { return "block:" + n; }
        public Object item(String n) { return "item:" + n; }
        public Object biome(String f) { return "biome:" + f; }
        public Object potion(String f) { return "potion:" + f; }
        public Object enchantment(String f) { return "ench:" + f; }
    };

    private static IdRegistry fresh(List<String> log) {
        return IdRegistry.vanilla(NAMES).logTo(log::add);
    }

    @Test
    void vanillaCounts() {
        VanillaIds v = VanillaIds.get();
        assertEquals(164, v.blocks.size()); // 0..159 and 170..173
        assertEquals(170, v.items.size()); // 256..408, 417..421, 2256..2267
        assertEquals(23, v.biomes.size());
        assertEquals(23, v.potions.size());
        assertEquals(22, v.enchantments.size());
        assertEquals(54, v.entities.size());
    }

    @Test
    void knownIds() {
        VanillaIds v = VanillaIds.get();
        assertEquals(1, v.blockByField("stone").id());
        assertEquals("flowing_water", v.blockByField("waterMoving").name1710());
        assertEquals(98, v.blockId("minecraft:stonebrick"));
        assertEquals(173, v.blockId("coal_block"));
        assertEquals(-1, v.blockId("stained_glass"));
        assertEquals(265, v.itemByField("ingotIron").id());
        assertEquals("iron_ingot", v.itemByField("ingotIron").name1710());
        assertEquals(2256, v.itemId("record_13"));
        assertEquals(421, v.itemId("name_tag"));
        assertEquals(50, v.blockId("torch"));
    }

    @Test
    void idsAreUniquePerTable() {
        VanillaIds v = VanillaIds.get();
        for (List<VanillaIds.Entry> l : List.of(v.blocks, v.items, v.biomes, v.potions, v.enchantments, v.entities)) {
            Set<Integer> ids = new HashSet<>();
            Set<String> n164 = new HashSet<>();
            for (VanillaIds.Entry e : l) {
                assertTrue(ids.add(e.id()), "duplicate id " + e.id());
                assertTrue(n164.add(e.name164()), "duplicate name " + e.name164());
            }
        }
        // item ids and non-reserved 1.7.10 names are unique too
        Set<String> names = new HashSet<>();
        for (VanillaIds.Entry e : v.items) assertTrue(names.add(e.name1710()), e.name1710());
    }

    @Test
    void vanillaPrefilled() {
        IdRegistry r = fresh(new ArrayList<>());
        assertEquals("block:stone", r.block(1));
        assertEquals("item:stone", r.item(1));
        assertEquals("item:iron_ingot", r.item(265));
        assertEquals(265, r.itemId(r.item(265)));
        assertEquals("biome:plains", r.biome(1));
        assertNull(r.block(95)); // reserved, nothing in 1.7.10
        assertTrue(r.isTaken(Kind.BLOCK, 95));
        assertTrue(r.isTaken(Kind.ITEM, 2267));
        assertNull(r.block(160));
        assertEquals("ench:fortune", r.get(Kind.ENCHANTMENT, 35));
    }

    @Test
    void requestedIdIsHonouredAndFreeIdUsedOtherwise() {
        List<String> log = new ArrayList<>();
        IdRegistry r = fresh(log);
        assertEquals(2000, r.claimBlock("modA:ore", 2000, "modA"));
        assertEquals("modA:ore", r.block(2000));
        assertEquals(2000, r.blockId("modA:ore"));
        // re-claim is stable
        assertEquals(2000, r.claimBlock("modA:ore", 3000, "modA"));
        // no request: free id from the top
        assertEquals(4095, r.claimBlock("modB:x", -1, "modB"));
        assertEquals(4094, r.claimBlock("modB:y", -1, "modB"));
        assertTrue(log.isEmpty());
        // items
        assertEquals(5000, r.claimItem("modA:gem", 5000, "modA"));
        assertEquals(31999, r.claimItem("modB:gem", -1, "modB"));
    }

    @Test
    void conflictsMoveTheObjectAndLog() {
        List<String> log = new ArrayList<>();
        IdRegistry r = fresh(log);
        r.claimBlock("modA:a", 1000, "modA");
        int moved = r.claimBlock("modB:b", 1000, "modB");
        assertTrue(moved != 1000 && moved > 0);
        assertEquals(1, log.size());
        assertTrue(log.get(0).contains("modB") && log.get(0).contains("1000") && log.get(0).contains("modA:a"));
        // vanilla id
        int m2 = r.claimBlock("modB:c", 1, "modB");
        assertTrue(m2 != 1);
        assertEquals("block:stone", r.block(1));
        assertEquals(2, log.size());
        // reserved vanilla id with no object (lockedChest)
        assertTrue(r.claimBlock("modB:d", 95, "modB") != 95);
        // out of range
        int m3 = r.claimItem("modB:i", 99999, "modB");
        assertTrue(m3 > 0 && m3 < Kind.ITEM.size);
        assertEquals(4, log.size());
    }

    @Test
    void blockAndItemShareTheLowRange() {
        List<String> log = new ArrayList<>();
        IdRegistry r = fresh(log);
        assertEquals(1500, r.claimBlock("modA:b", 1500, "modA"));
        // a plain item cannot take the ItemBlock slot of the block
        int it = r.claimItem("modA:i", 1500, "modA");
        assertTrue(it != 1500);
        assertEquals(1, log.size());
        // the block's own ItemBlock can
        assertTrue(r.claimBlockItem(1500, "modA:b_item"));
        assertEquals("modA:b_item", r.item(1500));
        assertEquals(1500, r.itemId("modA:b_item"));
        // an item claimed first blocks the same block id
        assertEquals(1600, r.claimItem("modC:i", 1600, "modC"));
        assertTrue(r.claimBlock("modC:b", 1600, "modC") != 1600);
        // an item above the block range is independent
        assertEquals(6000, r.claimItem("modC:j", 6000, "modC"));
    }

    @Test
    void vanillaBlockItemSlotsAreNotFreeForItems() {
        IdRegistry r = fresh(new ArrayList<>());
        assertTrue(r.claimItem("m:i", 4, "m") != 4);
        assertTrue(r.claimItem("m:i2", 256, "m") != 256);
    }

    @Test
    void biomePotionEnchantmentTables() {
        List<String> log = new ArrayList<>();
        IdRegistry r = fresh(log);
        assertEquals(40, r.claimBiome("m:biome", 40, "m"));
        assertEquals(255, r.claimBiome("m:biome2", 1, "m"));
        assertEquals(254, r.claimBiome("m:biome3", -1, "m"));
        assertEquals(24, r.claimPotion("m:pot", 24, "m"));
        assertEquals(31, r.claimPotion("m:pot2", -1, "m"));
        assertEquals(100, r.claimEnchantment("m:e", 100, "m"));
        assertTrue(r.claimEnchantment("m:e2", 16, "m") != 16);
        assertNotNull(r.biome(40));
        assertEquals(40, r.biomeId("m:biome"));
        assertEquals(-1, r.biomeId("unknown"));
    }

    @Test
    void tableFullReturnsMinusOne() {
        IdRegistry r = fresh(new ArrayList<>());
        int n = 0;
        while (r.claimPotion("p" + n, -1, "m") >= 0) n++;
        assertEquals(32 - 24, n); // ids 24..31 were free
    }
}
