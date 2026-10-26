package legacyconnector.v1_7_10.net.minecraft.tileentity;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.potion.Potion;
import legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/**
 * The beacon: every 80 ticks it counts the levels of its pyramid ({@code isBeaconBase} blocks) and gives nearby players its powers.
 * A mod's beacon (a subclass) runs 1.7.10's logic; a view of a vanilla beacon reads the modern one's levels and powers.
 */
public class TileEntityBeacon extends TileEntity implements IInventory {
    public static final Potion[][] effectsList = {{Potion.moveSpeed, Potion.digSpeed}, {Potion.resistance, Potion.jump}, {Potion.damageBoost},
            {Potion.regeneration}};
    private long field_146016_i;
    private float field_146014_j;
    private boolean isComplete;
    private int levels = -1;
    private int primaryEffect;
    private int secondaryEffect;
    private ItemStack payment;
    private String field_146008_p;
    private final BeaconBlockEntity mcc$beacon;

    public TileEntityBeacon() {
        this.mcc$beacon = null;
    }

    /** View of a vanilla beacon. */
    public TileEntityBeacon(BeaconBlockEntity beacon) {
        this.mcc$beacon = beacon;
    }

    @Override
    public void updateEntity() {
        if (mcc$beacon != null || worldObj == null) return;
        if (worldObj.getTotalWorldTime() % 80L == 0L) {
            func_146003_y();
            func_146000_x();
        }
    }

    /** Gives the powers to the players in range (10 blocks per level, plus 10). */
    private void func_146000_x() {
        if (!isComplete || levels <= 0 || worldObj.isRemote || primaryEffect <= 0) return;
        double range = levels * 10 + 10;
        int amplifier = levels >= 4 && primaryEffect == secondaryEffect ? 1 : 0;
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 1, zCoord + 1).expand(range, range, range);
        box.maxY = worldObj.getHeight();
        List<?> players = worldObj.getEntitiesWithinAABB(EntityPlayer.class, box);
        for (Object o : players) ((EntityPlayer) o).addPotionEffect(new PotionEffect(primaryEffect, 180, amplifier, true));
        if (levels >= 4 && primaryEffect != secondaryEffect && secondaryEffect > 0) {
            for (Object o : players) ((EntityPlayer) o).addPotionEffect(new PotionEffect(secondaryEffect, 180, 0, true));
        }
    }

    /** Counts the complete layers of beacon base blocks below (up to 4); none while the sky is blocked. */
    private void func_146003_y() {
        if (!worldObj.canBlockSeeTheSky(xCoord, yCoord + 1, zCoord)) {
            isComplete = false;
            levels = 0;
            return;
        }
        isComplete = true;
        levels = 0;
        for (int layer = 1; layer <= 4; levels = layer++) {
            int y = yCoord - layer;
            if (y < 0) break;
            boolean full = true;
            for (int x = xCoord - layer; x <= xCoord + layer && full; ++x) {
                for (int z = zCoord - layer; z <= zCoord + layer; ++z) {
                    Block block = worldObj.getBlock(x, y, z);
                    if (!block.isBeaconBase(worldObj, x, y, z, xCoord, yCoord, zCoord)) {
                        full = false;
                        break;
                    }
                }
            }
            if (!full) break;
        }
        if (levels == 0) isComplete = false;
    }

    /** The beam's brightness (0-1) this frame; it fades in while the beacon is complete. */
    public float shouldBeamRender() {
        boolean complete = mcc$beacon != null ? getLevels() > 0 : isComplete;
        if (!complete) return 0.0F;
        long now = worldObj != null ? worldObj.getTotalWorldTime() : 0L;
        int elapsed = (int) (now - field_146016_i);
        field_146016_i = now;
        if (elapsed > 1) {
            field_146014_j -= elapsed / 40.0F;
            if (field_146014_j < 0.0F) field_146014_j = 0.0F;
        }
        field_146014_j += 0.025F;
        if (field_146014_j > 1.0F) field_146014_j = 1.0F;
        return field_146014_j;
    }

    public int getPrimaryEffect() {
        if (mcc$beacon != null) return mcc$effect("primaryPower");
        return primaryEffect;
    }

    public int getSecondaryEffect() {
        if (mcc$beacon != null) return mcc$effect("secondaryPower");
        return secondaryEffect;
    }

    @SuppressWarnings("unchecked")
    private int mcc$effect(String field) {
        var f = VanillaOps.field(BeaconBlockEntity.class, field);
        try {
            Object value = f == null ? null : f.get(mcc$beacon);
            return value instanceof net.minecraft.core.Holder<?> h
                    ? org.kylepitcock.legacyconnector.v1_7_10.bridge.PotionBridge.idOf((net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) h) : 0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    /** The pyramid's complete layers (0-4). */
    public int getLevels() {
        if (mcc$beacon != null) return VanillaOps.getInt(mcc$beacon, BeaconBlockEntity.class, "levels", 0);
        return levels;
    }

    /** {@code func_146005_c}: sets the level count (the client learns it from the server). */
    public void func_146005_c(int levels) {
        this.levels = levels;
    }

    private static boolean mcc$isBeaconPower(int id) {
        for (int i = 0; i < effectsList.length; ++i) {
            for (Potion potion : effectsList[i]) if (potion.id == id) return true;
        }
        return false;
    }

    public void setPrimaryEffect(int id) {
        primaryEffect = mcc$isBeaconPower(id) ? id : 0;
    }

    public void setSecondaryEffect(int id) {
        secondaryEffect = mcc$isBeaconPower(id) ? id : 0;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        primaryEffect = tag.getInteger("Primary");
        secondaryEffect = tag.getInteger("Secondary");
        levels = tag.getInteger("Levels");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Primary", primaryEffect);
        tag.setInteger("Secondary", secondaryEffect);
        tag.setInteger("Levels", levels);
    }

    // ------------------------------------------------------------------ the payment slot

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? payment : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (slot != 0 || payment == null) return null;
        if (amount >= payment.mcc$count()) {
            ItemStack stack = payment;
            payment = null;
            return stack;
        }
        return payment.splitStack(amount);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (slot != 0 || payment == null) return null;
        ItemStack stack = payment;
        payment = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot == 0) payment = stack;
    }

    @Override
    public String getInventoryName() {
        return hasCustomInventoryName() ? field_146008_p : "container.beacon";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return field_146008_p != null && !field_146008_p.isEmpty();
    }

    /** Sets the beacon's custom name. */
    public void func_145999_a(String name) {
        this.field_146008_p = name;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack != null && stack.getItem() != null && stack.getItem().isBeaconPayment(stack);
    }
}
