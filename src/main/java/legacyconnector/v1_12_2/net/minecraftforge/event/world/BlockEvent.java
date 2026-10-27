package legacyconnector.v1_12_2.net.minecraftforge.event.world;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Base of Forge's block events. */
public class BlockEvent extends Event {
    private final World world;
    private final BlockPos pos;
    private final IBlockState state;

    public BlockEvent(World world, BlockPos pos, IBlockState state) {
        this.world = world;
        this.pos = pos;
        this.state = state;
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getPos() {
        return pos;
    }

    public IBlockState getState() {
        return state;
    }

    /** A player breaks a block; canceling it keeps the block. */
    @legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
    public static class BreakEvent extends BlockEvent {
        private final EntityPlayer player;
        private int exp;

        public BreakEvent(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
            super(world, pos, state);
            this.player = player;
        }

        public EntityPlayer getPlayer() {
            return player;
        }

        public int getExpToDrop() {
            return isCanceled() ? 0 : exp;
        }

        public void setExpToDrop(int exp) {
            this.exp = exp;
        }
    }

    /** The items a broken block drops; handlers change the list. */
    public static class HarvestDropsEvent extends BlockEvent {
        private final int fortuneLevel;
        private final java.util.List<ItemStack> drops;
        private final boolean isSilkTouching;
        private float dropChance;
        private final EntityPlayer harvester;

        public HarvestDropsEvent(World world, BlockPos pos, IBlockState state, int fortuneLevel, float dropChance, java.util.List<ItemStack> drops,
                EntityPlayer harvester, boolean isSilkTouching) {
            super(world, pos, state);
            this.fortuneLevel = fortuneLevel;
            this.dropChance = dropChance;
            this.drops = drops;
            this.isSilkTouching = isSilkTouching;
            this.harvester = harvester;
        }

        public int getFortuneLevel() {
            return fortuneLevel;
        }

        public java.util.List<ItemStack> getDrops() {
            return drops;
        }

        public boolean isSilkTouching() {
            return isSilkTouching;
        }

        public float getDropChance() {
            return dropChance;
        }

        public void setDropChance(float dropChance) {
            this.dropChance = dropChance;
        }

        public EntityPlayer getHarvester() {
            return harvester;
        }
    }
}
