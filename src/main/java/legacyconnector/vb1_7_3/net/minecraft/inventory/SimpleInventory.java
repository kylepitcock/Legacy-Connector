package legacyconnector.vb1_7_3.net.minecraft.inventory;

import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;

/** Original fixed-size named inventory, including virtual dirty callbacks after mutations. */
public class SimpleInventory implements Inventory {
    private String name;
    private int size;
    private ItemStack[] stacks;
    private List<InventoryListener> listeners;
    public SimpleInventory(String name,int size){this.name=name;this.size=size;stacks=new ItemStack[size];}
    @Override public ItemStack getStack(int slot){return stacks[slot];}
    @Override public ItemStack removeStack(int slot,int amount){
        var stack=stacks[slot];if(stack==null)return null;
        ItemStack removed;
        if(stack.count<=amount){removed=stack;stacks[slot]=null;}
        else{removed=stack.split(amount);if(stack.count==0)stacks[slot]=null;}
        markDirty();return removed;
    }
    @Override public void setStack(int slot,ItemStack stack){
        stacks[slot]=stack;if(stack!=null&&stack.count>getMaxCountPerStack())stack.count=getMaxCountPerStack();markDirty();
    }
    @Override public int size(){return size;}
    @Override public String getName(){return name;}
    @Override public int getMaxCountPerStack(){return 64;}
    @Override public void markDirty(){if(listeners!=null)for(int i=0;i<listeners.size();i++)listeners.get(i).onInventoryChanged(this);}
    @Override public boolean canPlayerUse(PlayerEntity player){return true;}
}
