package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ChestViewsB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        VanillaB173.init();
    }
    @Test void replacementChestReceivesOriginalLootAndLaterStandaloneWrites(){
        var nativeChest=new net.minecraft.world.level.block.entity.ChestBlockEntity(BlockPos.ZERO,Blocks.CHEST.defaultBlockState());
        var original=new legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity();
        BlockEntityViewsB173.attachChest(original,nativeChest);
        original.setStack(3,new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(264,4,0));
        assertEquals(net.minecraft.world.item.Items.DIAMOND,nativeChest.getItem(3).getItem());assertEquals(4,nativeChest.getItem(3).getCount());
        original.getStack(3).count=2;BlockEntityViewsB173.flush();assertEquals(2,nativeChest.getItem(3).getCount());
        original.setStack(3,null);assertTrue(nativeChest.getItem(3).isEmpty());
        BlockEntityViewsB173.flush();
    }
    @Test void existingNativeChestReadAndOneSlotWritePreserveOtherModernComponents(){
        var nativeChest=new net.minecraft.world.level.block.entity.ChestBlockEntity(BlockPos.ZERO,Blocks.CHEST.defaultBlockState());
        var custom=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,7);
        custom.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Keep native components"));
        nativeChest.setItem(1,custom);
        var view=(legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity)BlockEntityViewsB173.wrap(nativeChest);
        assertSame(view,BlockEntityViewsB173.wrap(nativeChest));assertEquals(7,view.getStack(1).count);
        view.setStack(2,new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(266,2,0));BlockEntityViewsB173.flush();
        assertSame(custom,nativeChest.getItem(1));assertEquals("Keep native components",nativeChest.getItem(1).getHoverName().getString());
        assertEquals(net.minecraft.world.item.Items.GOLD_INGOT,nativeChest.getItem(2).getItem());
    }
    @Test void retainedOriginalViewAndCountEditsPreserveNativeComponentsAcrossCallbacks(){
        var nativeChest=new net.minecraft.world.level.block.entity.ChestBlockEntity(BlockPos.ZERO,Blocks.CHEST.defaultBlockState());
        var custom=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,7);
        custom.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Still named"));
        var modernOnly=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_INGOT,3);
        nativeChest.setItem(1,custom);nativeChest.setItem(2,modernOnly);
        var view=(legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity)BlockEntityViewsB173.wrap(nativeChest);
        BlockEntityViewsB173.flush();
        view.setStack(3,new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(266,2,0));
        assertSame(custom,nativeChest.getItem(1));assertSame(modernOnly,nativeChest.getItem(2));
        view.getStack(1).count=4;view.markDirty();
        assertEquals(4,nativeChest.getItem(1).getCount());assertEquals("Still named",nativeChest.getItem(1).getHoverName().getString());
        assertSame(modernOnly,nativeChest.getItem(2));BlockEntityViewsB173.flush();
    }
    @Test void deferredOriginalChestReattachmentCopiesAllLootDespitePriorFlush(){
        var standIn=new net.minecraft.world.level.block.entity.ChestBlockEntity(BlockPos.ZERO,Blocks.CHEST.defaultBlockState());
        var original=new legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity();BlockEntityViewsB173.attachChest(original,standIn);
        original.setStack(5,new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(266,6,0));BlockEntityViewsB173.flush();
        var placed=new net.minecraft.world.level.block.entity.ChestBlockEntity(new BlockPos(50,220,2),Blocks.CHEST.defaultBlockState());
        BlockEntityViewsB173.attachChest(original,placed);BlockEntityViewsB173.flush();
        assertTrue(placed.getItem(5).is(net.minecraft.world.item.Items.GOLD_INGOT));assertEquals(6,placed.getItem(5).getCount());
    }
}
