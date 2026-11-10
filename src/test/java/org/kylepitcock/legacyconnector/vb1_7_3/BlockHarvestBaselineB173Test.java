package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.stat.Stat;
import legacyconnector.vb1_7_3.net.minecraft.stat.Stats;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlockHarvestBaselineB173Test {
    @BeforeAll static void bootstrap()throws Exception {NativeStackComponentsB173Test.bootstrap();Stats.initialize();}
    static class RecordingWorld extends World {final List<String> events=new ArrayList<>();}
    static class DropBlock extends Block {
        int drops,x,y,z,metadata;float chance;
        private DropBlock(){super(254,legacyconnector.vb1_7_3.net.minecraft.block.material.Material.STONE);}
        @Override public void dropStacks(World world,int x,int y,int z,int meta,float chance){
            ((RecordingWorld)world).events.add("drop");drops++;this.x=x;this.y=y;this.z=z;metadata=meta;this.chance=chance;
        }
    }
    static class DelegatingBlock extends DropBlock {
        @Override public void afterBreak(World world,PlayerEntity player,int x,int y,int z,int meta){super.afterBreak(world,player,x,y,z,meta);}
    }
    static class NativeStatsPlayer extends net.minecraft.server.level.ServerPlayer {
        int awards,total;net.minecraft.stats.Stat<?> last;
        private NativeStatsPlayer(){super(null,null,null,null);}
        @Override public void awardStat(net.minecraft.stats.Stat<?> stat,int amount){awards++;total+=amount;last=stat;}
    }
    static class OriginalPlayer extends PlayerEntity {
        final RecordingWorld world;final NativeStatsPlayer nativePlayer;Stat last;int requests;
        OriginalPlayer(RecordingWorld world,NativeStatsPlayer player){super(world);this.world=world;nativePlayer=player;}
        @Override public void increaseStat(Stat stat,int amount){world.events.add("stat");last=stat;requests++;StatisticsB173.increment(nativePlayer,stat,amount);}
    }
    private static <T>T blank(Class<T> type)throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);return type.cast(((sun.misc.Unsafe)field.get(null)).allocateInstance(type));
    }
    private static <T extends DropBlock>T block(Class<T> type)throws Exception {T block=blank(type);block.id=1;return block;}
    @Test void superclassHarvestDispatchesVirtualDropsOnceAfterStatWithExactCoordinatesAndMetadata()throws Exception {
        var block=block(DelegatingBlock.class);var world=new RecordingWorld();var nativePlayer=blank(NativeStatsPlayer.class);var player=new OriginalPlayer(world,nativePlayer);
        block.afterBreak(world,player,13,42,-7,11);
        assertEquals(List.of("stat","drop"),world.events);assertSame(Stats.MINE_BLOCK[1],player.last);assertEquals(1,player.requests);
        assertEquals(1,block.drops);assertEquals(13,block.x);assertEquals(42,block.y);assertEquals(-7,block.z);assertEquals(11,block.metadata);assertEquals(1F,block.chance);
        assertEquals(1,nativePlayer.awards);assertEquals(1,nativePlayer.total);
    }
    @Test void nativeOwnedHarvestCreditLeavesOneAwardAndOneDropWhileNonoverridesKeepNativeFallback()throws Exception {
        var block=block(DelegatingBlock.class);var world=new RecordingWorld();var nativePlayer=blank(NativeStatsPlayer.class);var player=new OriginalPlayer(world,nativePlayer);
        var nativeStat=StatisticsB173.nativeStat(Stats.MINE_BLOCK[1]);
        try(var credit=StatisticsB173.nativeAward(nativePlayer,nativeStat)){block.afterBreak(world,player,1,2,3,7);}
        assertEquals(0,nativePlayer.awards);nativePlayer.awardStat(nativeStat,1);
        assertEquals(1,nativePlayer.awards);assertEquals(1,nativePlayer.total);assertEquals(1,block.drops);
        var detection=LegacyB173Block.class.getDeclaredMethod("overrides",Block.class,String.class,Class[].class);detection.setAccessible(true);
        Class<?>[] signature={World.class,PlayerEntity.class,int.class,int.class,int.class,int.class};
        assertEquals(true,detection.invoke(null,block,"afterBreak",signature));
        assertEquals(false,detection.invoke(null,block(DropBlock.class),"afterBreak",signature));
    }
}
