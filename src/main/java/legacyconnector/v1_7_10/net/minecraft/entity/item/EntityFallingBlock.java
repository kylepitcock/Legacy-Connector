package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A block that fell off its position (sand, gravel); mods use it for their own falling blocks. */
public class EntityFallingBlock extends Entity {
    private Block block;
    public int field_145814_a;
    public int field_145812_b;
    public boolean field_145813_c = true;

    public EntityFallingBlock(World world) {
        super(world);
    }

    public EntityFallingBlock(World world, double x, double y, double z, Block block) {
        this(world, x, y, z, block, 0);
    }

    public EntityFallingBlock(World world, double x, double y, double z, Block block, int meta) {
        super(world);
        this.block = block;
        this.field_145814_a = meta;
        setSize(0.98F, 0.98F);
        setPosition(x, y, z);
    }

    public Block func_145805_f() {
        return block;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
