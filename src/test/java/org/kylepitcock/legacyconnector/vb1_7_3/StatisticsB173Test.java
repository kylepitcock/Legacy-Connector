package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.stat.Stat;
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
        // Beta sized the mined-block array like the block array (both 256); block ids now run to ID_LIMIT, so it keeps that relationship
        assertEquals(Block.BLOCKS.length,Stats.MINE_BLOCK.length);assertEquals(Block.ID_LIMIT,Stats.MINE_BLOCK.length);
        assertEquals(32000,Stats.CRAFTED.length);assertEquals(32000,Stats.USED.length);assertEquals(32000,Stats.BROKEN.length);
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
    @Test void expandedBlockIdsGetMinedStatsInTheirOwnFamilyWithNativeTargetsAndBetaIdsStayUntouched() throws Exception {
        // block ids above Beta's 255 (a mod whose config moved its blocks, the Block ID Expansion jarmod) index the mined-block array too
        int[] ids={Block.BETA_LIMIT+44,4000,Block.ID_LIMIT-1};
        // the mined family (0x1000000 + id) ends below the crafted family (0x1010000), so no expanded id can read another family's stat
        assertTrue(0x1000000+Block.ID_LIMIT-1<0x1010000);
        var old=new Block[ids.length];var idToStat=idToStat();var created=new java.util.ArrayList<Stat>();
        var mineAtBeta=Stats.MINE_BLOCK[1];var statsBefore=new java.util.ArrayList<>(Stats.ALL_STATS);
        try{
            for(int i=0;i<ids.length;i++){
                old[i]=Block.BLOCKS[ids[i]];
                var block=(Block)org.kylepitcock.legacyconnector.core.synth.Synth.blank(Block.class.getName());
                block.mcc$initVanilla(ids[i],0,legacyconnector.vb1_7_3.net.minecraft.block.material.Material.STONE,1,null,"fixture"+i);
                block.mcc$modern=net.minecraft.world.level.block.Blocks.DIRT;Block.BLOCKS[ids[i]]=block;
            }
            Stats.initialize();
            for(int id:ids){
                var mined=Stats.MINE_BLOCK[id];created.add(mined);
                assertNotNull(mined,"expanded block "+id+" tracks mined statistics");
                assertEquals(0x1000000+id,mined.id);assertEquals(id,((ItemOrBlockStat)mined).getItemOrBlockId());
                assertSame(mined,Stats.getStatById(0x1000000+id));
                assertNotSame(mined,Stats.getStatById(0x1020000+id),"the used-item family is a different stat");
                assertSame(net.minecraft.stats.Stats.BLOCK_MINED.get(net.minecraft.world.level.block.Blocks.DIRT),StatisticsB173.nativeStat(mined));
            }
            // the numeric ids of Beta's own range and its stat objects are the ones that were there before
            assertSame(mineAtBeta,Stats.MINE_BLOCK[1]);assertEquals(0x1000000+1,Stats.MINE_BLOCK[1].id);
            assertSame(net.minecraft.stats.Stats.BLOCK_MINED.get(net.minecraft.world.level.block.Blocks.STONE),StatisticsB173.nativeStat(Stats.MINE_BLOCK[1]));
            // an award of an expanded block's stat reaches the native player like a Beta block's
            var player=player();assertTrue(StatisticsB173.increment(player,Stats.MINE_BLOCK[ids[1]],2));
            assertEquals(1,player.awards);assertEquals(2,player.total);
            assertSame(net.minecraft.stats.Stats.BLOCK_MINED.get(net.minecraft.world.level.block.Blocks.DIRT),player.last);
            // registering again changes nothing
            var again=Stats.MINE_BLOCK[ids[2]];Stats.initialize();assertSame(again,Stats.MINE_BLOCK[ids[2]]);
        }finally{
            for(int i=0;i<ids.length;i++){Block.BLOCKS[ids[i]]=old[i];Stats.MINE_BLOCK[ids[i]]=null;idToStat.remove(0x1000000+ids[i]);}
            Stats.ALL_STATS.retainAll(statsBefore);Stats.BLOCK_MINED_STATS.retainAll(statsBefore);
        }
    }
    @SuppressWarnings("unchecked")
    private static java.util.Map<Integer,Stat> idToStat() throws Exception {
        var field=Stats.class.getDeclaredField("ID_TO_STAT");field.setAccessible(true);return (java.util.Map<Integer,Stat>)field.get(null);
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
