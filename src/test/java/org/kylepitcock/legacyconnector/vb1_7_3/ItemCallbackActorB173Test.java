package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ItemCallbackActorB173Test {
    @BeforeAll static void bootstrap() throws Exception {NativeStackComponentsB173Test.bootstrap();}
    @Test void productionItemScopeExposesPendingSelectedComponentsAndRestoresNestedActors(){
        var firstWorld=new World(){};var secondWorld=new World(){};
        var first=new ClientPlayerEntity(firstWorld){};var second=new ClientPlayerEntity(secondWorld){};
        var nativeStack=new net.minecraft.world.item.ItemStack(Items.MUSIC_DISC_CAT,2);
        nativeStack.set(DataComponents.CUSTOM_NAME,Component.literal("pending callback record"));
        var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("pending",61);
        nativeStack.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        var held=StacksB173.toLegacy(nativeStack);first.inventory.main[0]=held;
        var before=Minecraft.mcc$currentPlayerContext();
        try(var outer=ItemCallbacksB173.actorScope(firstWorld,first)){
            assertSame(first,Minecraft.mcc$currentPlayerContext());
            assertSame(held,first.inventory.getSelectedItem());
            var inserted=RecordsB173.insertionStack(firstWorld,held.itemId,Minecraft.mcc$currentPlayerContext());
            assertEquals(nativeStack.getComponents(),inserted.getComponents());assertEquals(1,inserted.getCount());
            assertEquals(2,held.count);assertSame(held,first.inventory.getSelectedItem());
            try(var inner=ItemCallbacksB173.actorScope(secondWorld,second)){
                assertSame(second,Minecraft.mcc$currentPlayerContext());
                assertFalse(RecordsB173.insertionStack(firstWorld,held.itemId,Minecraft.mcc$currentPlayerContext()).has(DataComponents.CUSTOM_NAME));
            }
            assertSame(first,Minecraft.mcc$currentPlayerContext());
        }
        assertSame(before,Minecraft.mcc$currentPlayerContext());
    }
    @Test void failedOriginalItemCallbackRestoresPreviousContext(){
        var world=new World(){};var first=new ClientPlayerEntity(world){};var second=new ClientPlayerEntity(world){};
        try(var outer=ItemCallbacksB173.actorScope(world,first)){
            assertThrows(IllegalStateException.class,()->{
                try(var inner=ItemCallbacksB173.actorScope(world,second)){throw new IllegalStateException("original callback failure");}
            });
            assertSame(first,Minecraft.mcc$currentPlayerContext());
        }
    }
}
