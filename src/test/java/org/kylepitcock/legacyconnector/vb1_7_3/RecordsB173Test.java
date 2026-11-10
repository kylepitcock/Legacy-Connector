package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

class RecordsB173Test {
    @BeforeAll static void bootstrap() throws Exception {NativeStackComponentsB173Test.bootstrap();}
    @Test void originalSignaturesAreConcrete() throws Exception {
        var world=legacyconnector.vb1_7_3.net.minecraft.world.World.class;
        assertEquals(void.class,world.getMethod("worldEvent",int.class,int.class,int.class,int.class,int.class).getReturnType());
        world.getMethod("worldEvent",legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity.class,int.class,int.class,int.class,int.class,int.class);
        legacyconnector.vb1_7_3.net.minecraft.block.JukeboxBlock.class.getMethod("insertRecord",world,int.class,int.class,int.class,int.class);
        legacyconnector.vb1_7_3.net.minecraft.block.JukeboxBlock.class.getMethod("tryEjectRecord",world,int.class,int.class,int.class);
    }
    @Test void insertionCopiesOneRecordPreservingIdentityAndComponentsWithoutConsumingHand(){
        var input=new ItemStack(Items.MUSIC_DISC_CAT,3);input.set(DataComponents.CUSTOM_NAME,Component.literal("kept"));
        var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("original",42);
        input.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        var stored=new java.util.concurrent.atomic.AtomicReference<ItemStack>(ItemStack.EMPTY);
        assertTrue(RecordsB173.insert(input,stored.get(),stored::set));
        assertNotSame(input,stored.get());assertTrue(stored.get().is(Items.MUSIC_DISC_CAT));assertEquals(1,stored.get().getCount());
        assertEquals(3,input.getCount());assertEquals(input.getComponents(),stored.get().getComponents());
        assertFalse(RecordsB173.insert(new ItemStack(Items.MUSIC_DISC_13),stored.get(),s->fail("occupied overwritten")));
        assertFalse(RecordsB173.insert(new ItemStack(Items.DIAMOND),ItemStack.EMPTY,s->fail("nonrecord accepted")));
    }
    @Test void recordEventsUseSongRegistryIdsAndCurrentVersionEventNumbers(){
        assertEquals(new RecordsB173.Event(1010,73),RecordsB173.recordEvent(2257,id->{assertEquals(2257,id);return 73;}));
        assertEquals(new RecordsB173.Event(1011,0),RecordsB173.recordEvent(0,id->{throw new AssertionError();}));
        assertNull(RecordsB173.recordEvent(999,id->-1));
    }
    @Test void nativeOccupiedStateIsVisibleThroughOriginalMetadata(){
        var state=net.minecraft.world.level.block.Blocks.JUKEBOX.defaultBlockState();
        assertEquals(0,VanillaBlockStatesB173.legacyMeta(state));
        var occupied=VanillaBlockStatesB173.modernState(84,1,state);
        assertEquals(1,VanillaBlockStatesB173.legacyMeta(occupied));
        assertEquals(state,VanillaBlockStatesB173.modernState(84,0,occupied));
    }
    @Test void integerInsertionResolvesOnlyMatchingActiveWorldAndPendingSelectedStack(){
        var world=new legacyconnector.vb1_7_3.net.minecraft.world.World(){};
        var actor=new legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity(world){};
        var input=new ItemStack(Items.MUSIC_DISC_CAT,2);input.set(DataComponents.CUSTOM_NAME,Component.literal("Pending original stack"));
        var data=new net.minecraft.nbt.CompoundTag();data.putInt("fixture",19);
        input.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
        var legacy=StacksB173.toLegacy(input);actor.inventory.main[0]=legacy;actor.inventory.selectedSlot=0;
        var resolved=RecordsB173.insertionStack(world,legacy.itemId,actor);
        assertEquals(input.getComponents(),resolved.getComponents());assertEquals(1,resolved.getCount());assertEquals(2,legacy.count);
        actor.inventory.selectedSlot=1;
        assertFalse(RecordsB173.insertionStack(world,legacy.itemId,actor).has(DataComponents.CUSTOM_NAME));
        actor.inventory.selectedSlot=0;
        assertFalse(RecordsB173.insertionStack(new legacyconnector.vb1_7_3.net.minecraft.world.World(){},legacy.itemId,actor).has(DataComponents.CUSTOM_DATA));
        assertTrue(RecordsB173.insertionStack(world,2256,actor).is(Items.MUSIC_DISC_13));
    }
}
