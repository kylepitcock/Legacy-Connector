package legacyconnector.vb1_7_3.net.minecraft.inventory;

import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;

/** Beta InventoryLargeChest: a live view of both halves, never a copy of their stacks. */
public class DoubleInventory implements Inventory {
    private final String name;
    private final Inventory first,second;
    public DoubleInventory(String name,Inventory first,Inventory second){
        this.name=name;this.first=first==null?second:first;this.second=second==null?first:second;
    }
    @Override public int size(){return first.size()+second.size();}
    @Override public String getName(){return name;}
    private Inventory half(int slot){return slot<first.size()?first:second;}
    private int index(int slot){return slot<first.size()?slot:slot-first.size();}
    @Override public ItemStack getStack(int slot){return half(slot).getStack(index(slot));}
    @Override public ItemStack removeStack(int slot,int amount){return half(slot).removeStack(index(slot),amount);}
    @Override public void setStack(int slot,ItemStack stack){half(slot).setStack(index(slot),stack);}
    @Override public int getMaxCountPerStack(){return first.getMaxCountPerStack();}
    @Override public void markDirty(){first.markDirty();second.markDirty();}
    @Override public boolean canPlayerUse(PlayerEntity player){return first.canPlayerUse(player)&&second.canPlayerUse(player);}
}
