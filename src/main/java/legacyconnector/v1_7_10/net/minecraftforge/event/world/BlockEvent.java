package legacyconnector.v1_7_10.net.minecraftforge.event.world;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;

public class BlockEvent extends Event {
    public final int x;
    public final int y;
    public final int z;
    public final World world;
    public final Block block;
    public final int blockMetadata;

    public BlockEvent(int x, int y, int z, World world, Block block, int blockMetadata) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.block = block;
        this.blockMetadata = blockMetadata;
    }

    @Cancelable
    public static class BreakEvent extends BlockEvent {
        private final EntityPlayer player;
        private int exp;

        public BreakEvent(int x, int y, int z, World world, Block block, int meta, EntityPlayer player) {
            super(x, y, z, world, block, meta);
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
        public final EntityPlayer player;
        public final ItemStack itemInHand;
        public final Block placedBlock;
        public final Block placedAgainst;

        public PlaceEvent(int x, int y, int z, World world, Block placedBlock, int meta, Block placedAgainst, EntityPlayer player, ItemStack itemInHand) {
            super(x, y, z, world, placedBlock, meta);
            this.player = player;
            this.itemInHand = itemInHand;
            this.placedBlock = placedBlock;
            this.placedAgainst = placedAgainst;
        }
    }

    /** A block is about to drop its items: handlers add, remove or replace them. */
    public static class HarvestDropsEvent extends BlockEvent {
        public final int fortuneLevel;
        public final java.util.ArrayList<ItemStack> drops;
        public final boolean isSilkTouching;
        public float dropChance;
        public final EntityPlayer harvester;

        public HarvestDropsEvent(int x, int y, int z, World world, Block block, int meta, int fortuneLevel, float dropChance,
                java.util.ArrayList<ItemStack> drops, EntityPlayer harvester, boolean isSilkTouching) {
            super(x, y, z, world, block, meta);
            this.fortuneLevel = fortuneLevel;
            this.dropChance = dropChance;
            this.drops = drops;
            this.isSilkTouching = isSilkTouching;
            this.harvester = harvester;
        }
    }
}
