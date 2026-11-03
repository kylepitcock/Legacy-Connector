package legacyconnector.v1_7_10.net.minecraft.tileentity;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/**
 * The enchantment table's floating book: the animation state its renderer reads (page flip, book spread, rotation). A view of a
 * vanilla table copies the modern table's animation when it is made (the modern table runs it); a mod's own subclass (Arcane
 * Compendium's lectern) runs 1.7.10's animation in {@link #updateEntity()}.
 */
public class TileEntityEnchantmentTable extends TileEntity {
    public int tickCount;
    public float pageFlip;
    public float pageFlipPrev;
    public float field_145932_k;
    public float field_145929_l;
    public float bookSpread;
    public float bookSpreadPrev;
    public float bookRotation;
    public float bookRotationPrev;
    public float field_145924_q;
    private String field_145922_s;
    private final Random mcc$rand = new Random();
    private final net.minecraft.world.level.block.entity.EnchantingTableBlockEntity mcc$table;

    public TileEntityEnchantmentTable() {
        this.mcc$table = null;
    }

    /** View of a vanilla enchanting table. */
    public TileEntityEnchantmentTable(net.minecraft.world.level.block.entity.EnchantingTableBlockEntity table) {
        this.mcc$table = table;
        this.tickCount = table.time;
        this.pageFlip = table.flip;
        this.pageFlipPrev = table.oFlip;
        this.field_145932_k = table.flipT;
        this.field_145929_l = table.flipA;
        this.bookSpread = table.open;
        this.bookSpreadPrev = table.oOpen;
        this.bookRotation = table.rot;
        this.bookRotationPrev = table.oRot;
        this.field_145924_q = table.tRot;
        if (table.hasCustomName()) this.field_145922_s = table.getCustomName().getString();
    }

    @Override
    public boolean canUpdate() {
        return mcc$table == null;
    }

    @Override
    public void updateEntity() {
        if (mcc$table != null) return;
        super.updateEntity();
        bookSpreadPrev = bookSpread;
        bookRotationPrev = bookRotation;
        EntityPlayer player = worldObj == null ? null : worldObj.getClosestPlayer(xCoord + 0.5F, yCoord + 0.5F, zCoord + 0.5F, 3.0D);
        if (player != null) {
            double dx = player.mcc$x() - (xCoord + 0.5F);
            double dz = player.mcc$z() - (zCoord + 0.5F);
            field_145924_q = (float) Math.atan2(dz, dx);
            bookSpread += 0.1F;
            if (bookSpread < 0.5F || mcc$rand.nextInt(40) == 0) {
                float old = field_145932_k;
                do {
                    field_145932_k += mcc$rand.nextInt(4) - mcc$rand.nextInt(4);
                } while (old == field_145932_k);
            }
        } else {
            field_145924_q += 0.02F;
            bookSpread -= 0.1F;
        }
        while (bookRotation >= (float) Math.PI) bookRotation -= (float) Math.PI * 2.0F;
        while (bookRotation < -(float) Math.PI) bookRotation += (float) Math.PI * 2.0F;
        while (field_145924_q >= (float) Math.PI) field_145924_q -= (float) Math.PI * 2.0F;
        while (field_145924_q < -(float) Math.PI) field_145924_q += (float) Math.PI * 2.0F;
        float diff = field_145924_q - bookRotation;
        while (diff >= (float) Math.PI) diff -= (float) Math.PI * 2.0F;
        while (diff < -(float) Math.PI) diff += (float) Math.PI * 2.0F;
        bookRotation += diff * 0.4F;
        bookSpread = Math.max(0.0F, Math.min(1.0F, bookSpread));
        tickCount++;
        pageFlipPrev = pageFlip;
        float f = Math.max(-0.2F, Math.min(0.2F, (field_145932_k - pageFlip) * 0.4F));
        field_145929_l += (f - field_145929_l) * 0.9F;
        pageFlip += field_145929_l;
    }

    public String func_145919_a() {
        return hasCustomInventoryName() ? field_145922_s : "container.enchant";
    }

    public boolean hasCustomInventoryName() {
        return field_145922_s != null && !field_145922_s.isEmpty();
    }

    public void func_145920_a(String name) {
        this.field_145922_s = name;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (hasCustomInventoryName()) tag.setString("CustomName", field_145922_s);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("CustomName", 8)) field_145922_s = tag.getString("CustomName");
    }
}
