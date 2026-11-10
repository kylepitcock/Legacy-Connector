package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.stat.Stats;
import legacyconnector.vb1_7_3.net.minecraft.stat.ItemOrBlockStat;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StatisticsB173Test {
    private static class NativePlayer extends net.minecraft.server.level.ServerPlayer {
        int awards,total;net.minecraft.stats.Stat<?> last;
        private NativePlayer(){super(null,null,null,null);}
        @Override public void awardStat(net.minecraft.stats.Stat<?> stat,int amount){awards++;total+=amount;last=stat;}
    }
    @BeforeAll static void bootstrap() throws Exception {NativeStackComponentsB173Test.bootstrap();Stats.initialize();}
    private static NativePlayer player() throws Exception {
        var unsafe=Class.forName("sun.misc.Unsafe");var field=unsafe.getDeclaredField("theUnsafe");field.setAccessible(true);
        return (NativePlayer)unsafe.getMethod("allocateInstance",Class.class).invoke(field.get(null),NativePlayer.class);
    }
    @Test void originalArraySizesNumericFamiliesAliasesAndVanillaNativeTargetsRemainValid(){
        assertEquals(256,Stats.MINE_BLOCK.length);assertEquals(32000,Stats.CRAFTED.length);assertEquals(32000,Stats.USED.length);assertEquals(32000,Stats.BROKEN.length);
        assertEquals(0x1000000+1,Stats.MINE_BLOCK[1].id);assertEquals(0x1020000+257,Stats.USED[257].id);assertEquals(0x1030000+257,Stats.BROKEN[257].id);
        assertSame(Stats.MINE_BLOCK[3],Stats.MINE_BLOCK[2]);assertSame(Stats.MINE_BLOCK[61],Stats.MINE_BLOCK[62]);
        assertSame(net.minecraft.stats.Stats.BLOCK_MINED.get(net.minecraft.world.level.block.Blocks.STONE),StatisticsB173.nativeStat(Stats.MINE_BLOCK[1]));
        assertSame(net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.IRON_PICKAXE),StatisticsB173.nativeStat(Stats.USED[257]));
        assertNull(Stats.BROKEN[264]); // Diamonds are not damageable.
        assertEquals(257,((ItemOrBlockStat)Stats.USED[257]).getItemOrBlockId());assertSame(Stats.MINE_BLOCK[1],Stats.getStatById(0x1000000+1));
    }
    @Test void lateRegisteredContentAndRecipeOutputsPopulateIdempotently() throws Exception {
        var oldBlock=Block.BLOCKS[250];var oldItem=Item.ITEMS[31999];
        var recipesField=RecipesB173.class.getDeclaredField("RECIPES");recipesField.setAccessible(true);var recipes=(java.util.List<?>)recipesField.get(null);int recipeCount=recipes.size();
        try{
            var block=(Block)org.kylepitcock.legacyconnector.core.synth.Synth.blank(Block.class.getName());
            block.mcc$initVanilla(250,0,legacyconnector.vb1_7_3.net.minecraft.block.material.Material.STONE,1,null,"fixture");block.mcc$modern=net.minecraft.world.level.block.Blocks.DIRT;Block.BLOCKS[250]=block;
            var item=(Item)org.kylepitcock.legacyconnector.core.synth.Synth.blank(Item.class.getName());item.mcc$initVanilla(31999,1,37,"fixture");item.mcc$modern=net.minecraft.world.item.Items.IRON_PICKAXE;Item.ITEMS[31999]=item;
            RecipesB173.shapeless(new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(31999,1,0),new Object[0]);Stats.initialize();
            assertNotNull(Stats.MINE_BLOCK[250]);assertNotNull(Stats.USED[31999]);assertNotNull(Stats.BROKEN[31999]);assertNotNull(Stats.CRAFTED[31999]);
            assertEquals(0x1010000+31999,Stats.CRAFTED[31999].id);
            assertSame(net.minecraft.stats.Stats.ITEM_CRAFTED.get(net.minecraft.world.item.Items.IRON_PICKAXE),StatisticsB173.nativeStat(Stats.CRAFTED[31999]));
            var mined=Stats.MINE_BLOCK[250];var used=Stats.USED[31999];Stats.initialize();assertSame(mined,Stats.MINE_BLOCK[250]);assertSame(used,Stats.USED[31999]);
        }finally{Block.BLOCKS[250]=oldBlock;Item.ITEMS[31999]=oldItem;recipes.subList(recipeCount,recipes.size()).clear();}
    }
    @Test void nativeHarvestBoundarySuppressesOnlyTheExactActorsAlreadyOwnedAward() throws Exception {
        var first=player();var second=player();var nativeMine=StatisticsB173.nativeStat(Stats.MINE_BLOCK[1]);
        try(var harvest=StatisticsB173.nativeAward(first,nativeMine)){
            assertTrue(StatisticsB173.increment(first,Stats.MINE_BLOCK[1],1));assertEquals(0,first.awards);
            assertTrue(StatisticsB173.increment(second,Stats.MINE_BLOCK[1],1));assertEquals(1,second.awards);
            assertTrue(StatisticsB173.increment(first,Stats.USED[257],2));assertEquals(1,first.awards);assertEquals(2,first.total);
            try(var nested=StatisticsB173.nativeAward(second,nativeMine)){assertFalse(StatisticsB173.nativeOwns(first,nativeMine));}
            assertTrue(StatisticsB173.nativeOwns(first,nativeMine));
        }
        assertFalse(StatisticsB173.nativeOwns(first,nativeMine));StatisticsB173.increment(first,Stats.MINE_BLOCK[1],1);
        assertEquals(2,first.awards);assertEquals(3,first.total);
    }
    @Test void harvestCreditDeduplicatesOnlyOneNativeAwardAndRetainsAdditionalOriginalAmounts() throws Exception {
        var player=player();var nativeMine=StatisticsB173.nativeStat(Stats.MINE_BLOCK[1]);
        try(var harvest=StatisticsB173.nativeAward(player,nativeMine)){
            StatisticsB173.increment(player,Stats.MINE_BLOCK[1],3);StatisticsB173.increment(player,Stats.MINE_BLOCK[1],1);
        }
        player.awardStat(nativeMine,1);assertEquals(4,player.total);
    }
    @Test void aliasesStayInTheirNumericFamilyAndConsumeOnlyTheMatchingNativeHarvestCredit() throws Exception {
        assertNotNull(Stats.MINE_BLOCK[10]);assertNotNull(Stats.MINE_BLOCK[11]);
        assertNotSame(Stats.MINE_BLOCK[10],Stats.MINE_BLOCK[11],"Original lava self-link is not a flowing/still alias");
        assertSame(Stats.USED[2],Stats.USED[3]);assertNotSame(Stats.MINE_BLOCK[3],Stats.USED[3]);
        var mined=StatisticsB173.nativeStat(Stats.MINE_BLOCK[2]);var used=StatisticsB173.nativeStat(Stats.USED[2]);assertNotSame(mined,used);
        var player=player();try(var nativeHarvest=StatisticsB173.nativeAward(player,mined)){
            StatisticsB173.increment(player,Stats.MINE_BLOCK[2],1);assertEquals(0,player.awards);
            StatisticsB173.increment(player,Stats.USED[2],1);assertEquals(1,player.awards);assertSame(used,player.last);
        }
        player.awardStat(mined,1);assertEquals(2,player.awards);assertEquals(2,player.total);
    }
}
