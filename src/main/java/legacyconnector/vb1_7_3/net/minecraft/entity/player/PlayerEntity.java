package legacyconnector.vb1_7_3.net.minecraft.entity.player;

import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.vb1_7_3.MenusB173;

/** Beta 1.7.3 {@code EntityPlayer}: inventory and name. The layer's player is a view of the modern one ({@code PlayerViewB173}). */
public abstract class PlayerEntity extends LivingEntity {
    public PlayerInventory inventory = new PlayerInventory(this);
    public legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler playerScreenHandler;
    public legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler currentScreenHandler;
    public legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen mcc$inventoryScreen;
    public String name;
    public String playerCapeUrl;
    public double capeX,capeY,capeZ,prevCapeX,prevCapeY,prevCapeZ;
    public float stepBobbingAmount,prevStepBobbingAmount;
    public int dimensionId;
    public boolean handSwinging;
    public boolean sleeping;
    public int sleepTimer;
    public float sleepOffsetX,sleepOffsetY,sleepOffsetZ;
    public legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3i sleepingPos;
    private float mcc$awakeEyeHeight=1.62F;
    /** Capture before invoking original callbacks, which may replace the offset as part of entering bed. */
    public void mcc$rememberAwakeEyeHeight(){if(!sleeping)mcc$awakeEyeHeight=standingEyeHeight;}
    public void mcc$restoreAwakeEyeHeight(){standingEyeHeight=mcc$awakeEyeHeight;}
    public void mcc$syncSleeping(boolean nativeSleeping){
        if(sleeping&&!nativeSleeping)mcc$restoreAwakeEyeHeight();
        else if(!sleeping){mcc$rememberAwakeEyeHeight();if(nativeSleeping)standingEyeHeight=.2F;}
        sleeping=nativeSleeping;
    }
    public boolean isSleeping(){return sleeping;}
    public boolean isFullyAsleep(){return sleeping&&sleepTimer>=100;}
    public int getSleepTimer(){return sleepTimer;}
    public float getSleepingRotation(){
        if(sleepingPos==null)return 0;
        return switch(world.getBlockMeta(sleepingPos.x,sleepingPos.y,sleepingPos.z)&3){case 0->90F;case 1->0F;case 2->270F;case 3->180F;default->0F;};
    }
    public SleepAttemptResult trySleep(int x,int y,int z){return org.kylepitcock.legacyconnector.vb1_7_3.BedsB173.trySleep(this,x,y,z);}
    public void wakeUp(boolean immediately,boolean updateWorld,boolean setSpawn){org.kylepitcock.legacyconnector.vb1_7_3.BedsB173.wake(this,immediately,updateWorld,setSpawn);}

    public PlayerEntity(World world) {
        super(world);
        this.standingEyeHeight = 1.62F;
    }

    public ItemStack getHand() {
        return inventory.getSelectedItem();
    }

    @Override
    public ItemStack getHeldItem() {
        return inventory.getSelectedItem();
    }

    public void clearStackInHand(){inventory.setStack(inventory.selectedSlot,null);}
    public void sendMessage(String message) {org.kylepitcock.legacyconnector.vb1_7_3.PlayerMessagesB173.send(this,message);}
    @Override public double getSquaredDistance(double x,double y,double z){
        double baseline=super.getSquaredDistance(x,y,z);
        return this instanceof ClientPlayerEntity client?org.kylepitcock.legacyconnector.vb1_7_3.PlayerAbilitiesB173.distance(client,x,y,z,baseline):baseline;
    }
    @Override public double getSquaredDistance(legacyconnector.vb1_7_3.net.minecraft.entity.Entity other){
        // Beta's entity overload is independent of the coordinate-only PlayerAPI hook.
        double dx=x-other.x,dy=y-other.y,dz=z-other.z;return dx*dx+dy*dy+dz*dz;
    }
    @Override public void heal(int amount){
        if(this instanceof ClientPlayerEntity client)org.kylepitcock.legacyconnector.vb1_7_3.PlayerAbilitiesB173.heal(client,amount,()->mcc$healBaseline(amount));
        else mcc$healBaseline(amount);
    }
    private void mcc$healBaseline(int amount){if(mcc$modern instanceof net.minecraft.world.entity.LivingEntity living){living.heal(amount);health=Math.round(living.getHealth());}else super.heal(amount);}
    @Override public boolean checkWaterCollisions(){return this instanceof ClientPlayerEntity client?org.kylepitcock.legacyconnector.vb1_7_3.PlayerAbilitiesB173.water(client,()->super.checkWaterCollisions()):super.checkWaterCollisions();}
    public void incrementStat(legacyconnector.vb1_7_3.net.minecraft.stat.Stat stat,int amount){if(mcc$modern instanceof net.minecraft.server.level.ServerPlayer player)org.kylepitcock.legacyconnector.vb1_7_3.AdvancementsB173.increment(player,stat,amount);}
    public void incrementStat(legacyconnector.vb1_7_3.net.minecraft.stat.Stat stat){incrementStat(stat,1);}
    public void increaseStat(legacyconnector.vb1_7_3.net.minecraft.stat.Stat stat,int amount){incrementStat(stat,amount);}
    public void increaseStat(legacyconnector.vb1_7_3.net.minecraft.stat.Stat stat){incrementStat(stat,1);}
    public void swingHand(){handSwinging=true;if(mcc$modern instanceof net.minecraft.world.entity.LivingEntity living)living.swing(net.minecraft.world.InteractionHand.MAIN_HAND);}
    @Override public float getEyeHeight() { return 0.12F; }
    public void sendPickup(legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity, int count) {
        if (mcc$modern instanceof net.minecraft.world.entity.player.Player p && entity.mcc$modern != null) p.take(entity.mcc$modern,count);
    }

    /** MCP {@code displayGUIChest}: a chest screen over the inventory (a modern chest menu). */
    public void openChestScreen(Inventory inventory) {
        MenusB173.openChest(this, inventory);
    }

    /** MCP {@code closeScreen}. */
    protected void closeHandledScreen() {
        MenusB173.close(this);
    }

    /** MCP {@code dropPlayerItem}. */
    public void dropItem(ItemStack stack) {
        dropItem(stack, false);
    }

    /** MCP {@code dropPlayerItemWithRandomChoice}: the modern player throws the stack (scattered when {@code scatter}). */
    public void dropItem(ItemStack stack, boolean scatter) {
        MenusB173.drop(this, stack, scatter);
    }
}
