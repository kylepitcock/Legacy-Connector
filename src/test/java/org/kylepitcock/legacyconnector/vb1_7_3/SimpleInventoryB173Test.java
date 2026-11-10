package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.inventory.SimpleInventory;
import legacyconnector.vb1_7_3.net.minecraft.inventory.InventoryListener;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

class SimpleInventoryB173Test {
    @Test void namedStorageClampsToVirtualLimitAndSplitsOrReturnsOriginalStack(){
        var inventory=new SimpleInventory("Original fixture",2){@Override public int getMaxCountPerStack(){return 16;}};
        assertEquals(2,inventory.size());assertEquals("Original fixture",inventory.getName());assertTrue(inventory.canPlayerUse(null));
        var stack=new ItemStack(264,23,7);inventory.setStack(1,stack);assertSame(stack,inventory.getStack(1));assertEquals(16,stack.count);
        var split=inventory.removeStack(1,5);assertEquals(5,split.count);assertEquals(7,split.getDamage());assertEquals(11,stack.count);
        assertSame(stack,inventory.removeStack(1,11));assertNull(inventory.getStack(1));assertNull(inventory.removeStack(1,1));
        assertThrows(ArrayIndexOutOfBoundsException.class,()->inventory.getStack(2));
    }
    @Test void dirtyListenersObserveCompletedMutationsAndEmptyRemovalDoesNotNotify() throws Exception {
        var inventory=new SimpleInventory("Listener fixture",1);var changes=new java.util.ArrayList<Integer>();
        var field=SimpleInventory.class.getDeclaredField("listeners");field.setAccessible(true);
        field.set(inventory,java.util.List.<InventoryListener>of(value->{assertSame(inventory,value);changes.add(value.getStack(0)==null?0:value.getStack(0).count);}));
        inventory.setStack(0,new ItemStack(264,4,0));inventory.removeStack(0,1);inventory.removeStack(0,3);inventory.removeStack(0,1);
        assertEquals(java.util.List.of(4,3,0),changes);
    }
}
