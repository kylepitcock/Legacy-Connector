package legacyconnector.v1_12_2.net.minecraftforge.event.world;

import java.util.EnumSet;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Forge's block events. BreakEvent, PlaceEvent, HarvestDropsEvent, NeighborNotifyEvent and FarmlandTrampleEvent are posted from their
 * modern counterparts (see {@code WorldEvents1122}); cancelling them cancels the modern action.
 */
public class BlockEvent extends Event {
    private final World world;
    private final BlockPos pos;
    private final IBlockState state;

    public BlockEvent(World world, BlockPos pos, IBlockState state) {
        this.pos = pos;
        this.world = world;
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

    /** The drops of a broken block; handlers change the list and the chance each one drops. */
    public static class HarvestDropsEvent extends BlockEvent {
        private final int fortuneLevel;
        private final List<ItemStack> drops;
        private final boolean isSilkTouching;
        private float dropChance;
        private final EntityPlayer harvester;

        public HarvestDropsEvent(World world, BlockPos pos, IBlockState state, int fortuneLevel, float dropChance, List<ItemStack> drops,
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

        public List<ItemStack> getDrops() {
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

    @Cancelable
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

    @Cancelable
    public static class PlaceEvent extends BlockEvent {
        private final EntityPlayer player;
        private final Object blockSnapshot;
        private final IBlockState placedBlock;
        private final IBlockState placedAgainst;
        private final EnumHand hand;

        public PlaceEvent(Object blockSnapshot, IBlockState placedAgainst, EntityPlayer player, EnumHand hand) {
            this(null, null, null, blockSnapshot, placedAgainst, player, hand);
        }

        /** The modern placement as the legacy event: the world, position and placed state are known directly. */
        public PlaceEvent(World world, BlockPos pos, IBlockState placed, Object blockSnapshot, IBlockState placedAgainst, EntityPlayer player, EnumHand hand) {
            super(world, pos, placed);
            this.player = player;
            this.blockSnapshot = blockSnapshot;
            this.placedBlock = placed;
            this.placedAgainst = placedAgainst;
            this.hand = hand;
        }

        public EntityPlayer getPlayer() {
            return player;
        }

        public Object getBlockSnapshot() {
            return blockSnapshot;
        }

        public IBlockState getPlacedBlock() {
            return placedBlock;
        }

        public IBlockState getPlacedAgainst() {
            return placedAgainst;
        }

        public EnumHand getHand() {
            return hand;
        }
    }

    @Cancelable
    public static class MultiPlaceEvent extends PlaceEvent {
        private final List<Object> blockSnapshots;

        public MultiPlaceEvent(List<Object> blockSnapshots, IBlockState placedAgainst, EntityPlayer player, EnumHand hand) {
            super(blockSnapshots == null || blockSnapshots.isEmpty() ? null : blockSnapshots.get(0), placedAgainst, player, hand);
            this.blockSnapshots = blockSnapshots;
        }

        public List<Object> getReplacedBlockSnapshots() {
            return blockSnapshots;
        }
    }

    @Cancelable
    public static class NeighborNotifyEvent extends BlockEvent {
        private final EnumSet<EnumFacing> notifiedSides;
        private final boolean forceRedstoneUpdate;

        public NeighborNotifyEvent(World world, BlockPos pos, IBlockState state, EnumSet<EnumFacing> notifiedSides, boolean forceRedstoneUpdate) {
            super(world, pos, state);
            this.notifiedSides = notifiedSides;
            this.forceRedstoneUpdate = forceRedstoneUpdate;
        }

        public EnumSet<EnumFacing> getNotifiedSides() {
            return notifiedSides;
        }

        public boolean getForceRedstoneUpdate() {
            return forceRedstoneUpdate;
        }
    }

    @Cancelable
    public static class FarmlandTrampleEvent extends BlockEvent {
        private final Entity entity;
        private final float fallDistance;

        public FarmlandTrampleEvent(World world, BlockPos pos, IBlockState state, float fallDistance, Entity entity) {
            super(world, pos, state);
            this.entity = entity;
            this.fallDistance = fallDistance;
        }

        public Entity getEntity() {
            return entity;
        }

        public float getFallDistance() {
            return fallDistance;
        }
    }

    /** Crop growth (Forge's CropGrowEvent): Pre may deny or allow a growth step. */
    public static class CropGrowEvent extends BlockEvent {
        public CropGrowEvent(World world, BlockPos pos, IBlockState state) {
            super(world, pos, state);
        }

        @Event.HasResult
        public static class Pre extends CropGrowEvent {
            public Pre(World world, BlockPos pos, IBlockState state) {
                super(world, pos, state);
            }
        }

        public static class Post extends CropGrowEvent {
            private final IBlockState originalState;

            public Post(World world, BlockPos pos, IBlockState original, IBlockState state) {
                super(world, pos, state);
                this.originalState = original;
            }

            public IBlockState getOriginalState() {
                return originalState;
            }
        }
    }
}
