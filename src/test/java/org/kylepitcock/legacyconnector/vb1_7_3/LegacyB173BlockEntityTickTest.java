package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class LegacyB173BlockEntityTickTest {
    public static final class Machine extends BlockEntity {
        int timer=1,eggs=1;
        @Override public void tick(){if(--timer==0)eggs--;}
        @Override public void writeNbt(NbtCompound tag){super.writeNbt(tag);tag.putInt("timer",timer);tag.putInt("eggs",eggs);}
        @Override public void readNbt(NbtCompound tag){super.readNbt(tag);timer=tag.getInt("timer");eggs=tag.getInt("eggs");}
    }
    static {BlockEntity.create(Machine.class,"portable_tick_commit_machine");}
    /** A storage-boundary fixture: clean chunks skip writes; dirty chunks save original NBT through the native tag bridge. */
    private static final class Storage {
        boolean dirty;CompoundTag disk;
        void save(Machine machine){if(!dirty)return;var tag=new NbtCompound();machine.writeNbt(tag);disk=NbtB173.toModern(tag);dirty=false;}
        Machine reload(){return (Machine)BlockEntity.createFromNbt(NbtB173.fromModern(disk.copy()));}
    }
    @Test void successfulTimerAndInventoryMutationIsSavedAfterPreviouslyCleanSnapshot(){
        var machine=new Machine();var storage=new Storage();storage.dirty=true;storage.save(machine);
        assertEquals(1,storage.reload().eggs);
        LegacyB173BlockEntity.tickAndCommit(machine::tick,()->storage.dirty=true);
        assertTrue(storage.dirty);storage.save(machine);
        var restarted=storage.reload();assertEquals(0,restarted.timer);assertEquals(0,restarted.eggs);
        restarted.tick();assertEquals(0,restarted.eggs,"Completed hatch cannot repeat after NBT reload");
    }
    @Test void throwingCallbackOrViewFlushNeverPublishesSuccessfulCommit(){
        var storage=new Storage();var machine=new Machine();storage.dirty=true;storage.save(machine);
        assertThrows(IllegalStateException.class,()->LegacyB173BlockEntity.tickAndCommit(()->{
            machine.tick();throw new IllegalStateException("callback or scope close failed");
        },()->storage.dirty=true));
        assertFalse(storage.dirty);assertEquals(1,storage.reload().eggs);
    }
    @Test void successfulCommitRunsAfterCallbackAndExactlyOnce(){
        var order=new java.util.ArrayList<String>();
        LegacyB173BlockEntity.tickAndCommit(()->order.add("original+flush"),()->order.add("chunk-dirty"));
        assertEquals(java.util.List.of("original+flush","chunk-dirty"),order);
    }
}
