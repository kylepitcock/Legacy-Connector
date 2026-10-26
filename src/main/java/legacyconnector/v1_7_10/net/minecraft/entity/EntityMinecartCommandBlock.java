package legacyconnector.v1_7_10.net.minecraft.entity;

import io.netty.buffer.ByteBuf;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.command.server.CommandBlockLogic;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The command block minecart: a powered activator rail runs its command (at most every 4 ticks). Constructing this exact class
 * makes a vanilla command block cart; a mod's subclass runs the command through the legacy command logic.
 */
public class EntityMinecartCommandBlock extends EntityMinecart {
    private final CommandBlockLogic logic = new CommandBlockLogic() {
        @Override
        public void func_145756_e() {
        }

        @Override
        public int func_145751_f() {
            return 1;
        }

        @Override
        public void func_145757_a(ByteBuf buf) {
            buf.writeInt(getEntityId());
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates(MathHelper.floor_double(mcc$get$posX(EntityMinecartCommandBlock.this)),
                    MathHelper.floor_double(mcc$get$posY(EntityMinecartCommandBlock.this) + 0.5D), MathHelper.floor_double(mcc$get$posZ(EntityMinecartCommandBlock.this)));
        }

        @Override
        public World getEntityWorld() {
            return mcc$world();
        }
    };
    private int lastActivation;

    public EntityMinecartCommandBlock(World world) {
        super(world);
    }

    public EntityMinecartCommandBlock(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        logic.readDataFromNBT(tag);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        logic.writeDataToNBT(tag);
    }

    @Override
    public int getMinecartType() {
        return 6;
    }

    @Override
    public Block getDefaultDisplayTile() {
        return Blocks.command_block;
    }

    /** The cart's command state. */
    public CommandBlockLogic func_145822_e() {
        return logic;
    }

    @Override
    public void onActivatorRailPass(int x, int y, int z, boolean powered) {
        int ticks = mcc$get$ticksExisted(this);
        if (powered && ticks - lastActivation >= 4) {
            logic.func_145755_a(mcc$world());
            lastActivation = ticks;
        }
    }

    /** Only the vanilla cart has a command editing window; a click is taken either way. */
    @Override
    public boolean interactFirst(EntityPlayer player) {
        return true;
    }
}
