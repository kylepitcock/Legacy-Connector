package legacyconnector.vb1_7_3.net.minecraft.block;

import java.util.Random;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta 1.7.3 {@code BlockLeaves}, the base of mods' leaves (Twilight Forest's oak, canopy and mangrove leaves): foliage tint by metadata
 * (1 spruce, 2 birch, otherwise the biome's foliage colour), the sprite by metadata and graphics level, decay and sapling drops.
 */
public class LeavesBlock extends TransparentBlock {
    private static final int SPRUCE = 0x619961, BIRCH = 0x80A755, DEFAULT = 0x48B518; // FoliageColors spruce, birch, default
    public int spriteIndex;
    public int[] decayRegion;

    protected LeavesBlock(int id, int textureId) {
        super(id, textureId, Material.LEAVES, false);
        spriteIndex = textureId;
        // Beta set the graphics level every frame from the options; native leaves render fancy (see-through sides)
        renderSides = true;
        setTickRandomly(true);
    }

    @Override public int getColor(int meta) {
        if ((meta & 1) == 1) return SPRUCE;
        if ((meta & 2) == 2) return BIRCH;
        return DEFAULT;
    }

    @Override public int getColorMultiplier(BlockView view, int x, int y, int z) {
        int meta = view.getBlockMeta(x, y, z);
        if ((meta & 1) == 1) return SPRUCE;
        if ((meta & 2) == 2) return BIRCH;
        return foliage(view, x, y, z);
    }

    /** The biome's foliage colour where the leaves stand, through the native tint lookup (blended like vanilla leaves). */
    private static int foliage(BlockView view, int x, int y, int z) {
        net.minecraft.world.level.BlockGetter getter = view instanceof org.kylepitcock.legacyconnector.vb1_7_3.BlockQueryB173 query ? query.mcc$getter()
                : view instanceof World world ? world.mcc$access() : null;
        var pos = new net.minecraft.core.BlockPos(x, y, z);
        if (getter instanceof net.minecraft.world.level.BlockAndTintGetter tint && !(getter instanceof net.minecraft.server.level.ServerLevel)
                && !(getter instanceof net.minecraft.server.level.WorldGenRegion) && net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
            return org.kylepitcock.legacyconnector.vb1_7_3.client.FoliageTintB173.color(tint, pos);
        if (getter instanceof net.minecraft.world.level.LevelReader reader) return reader.getBiome(pos).value().getFoliageColor();
        return DEFAULT;
    }

    @Override public void onBreak(World world, int x, int y, int z) {
        int span = 2;
        if (!world.isRegionLoaded(x - span, y - span, z - span, x + span, y + span, z + span)) return;
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            if (world.getBlockId(x + dx, y + dy, z + dz) != id) continue;
            int meta = world.getBlockMeta(x + dx, y + dy, z + dz);
            world.setBlockMetaWithoutNotifyingNeighbors(x + dx, y + dy, z + dz, meta | 8);
        }
    }

    @Override public void onTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote) return;
        int meta = world.getBlockMeta(x, y, z);
        if ((meta & 8) == 0) return;
        int reach = 4, span = reach + 1, size = 32, area = size * size, half = size / 2;
        if (decayRegion == null) decayRegion = new int[size * size * size];
        if (world.isRegionLoaded(x - span, y - span, z - span, x + span, y + span, z + span)) {
            for (int dx = -reach; dx <= reach; dx++) for (int dy = -reach; dy <= reach; dy++) for (int dz = -reach; dz <= reach; dz++) {
                int other = world.getBlockId(x + dx, y + dy, z + dz);
                decayRegion[(dx + half) * area + (dy + half) * size + dz + half] = other == 17 ? 0 : other == id ? -2 : -1;
            }
            for (int pass = 1; pass <= 4; pass++)
                for (int dx = -reach; dx <= reach; dx++) for (int dy = -reach; dy <= reach; dy++) for (int dz = -reach; dz <= reach; dz++) {
                    int at = (dx + half) * area + (dy + half) * size + dz + half;
                    if (decayRegion[at] != pass - 1) continue;
                    int[] neighbours = {at - area, at + area, at - size, at + size, at - 1, at + 1};
                    for (int n : neighbours) if (decayRegion[n] == -2) decayRegion[n] = pass;
                }
        }
        if (decayRegion[half * area + half * size + half] >= 0) world.setBlockMetaWithoutNotifyingNeighbors(x, y, z, meta & ~8);
        else breakLeaves(world, x, y, z);
    }

    /** Beta {@code removeLeaves}: drops what the leaves drop and clears the block. */
    public void breakLeaves(World world, int x, int y, int z) {
        dropStacks(world, x, y, z, world.getBlockMeta(x, y, z));
        world.setBlock(x, y, z, 0);
    }

    @Override public int getDroppedItemCount(Random random) { return random.nextInt(20) == 0 ? 1 : 0; }

    @Override public int getDroppedItemId(int meta, Random random) { return 6; }

    @Override public int getDroppedItemMeta(int meta) { return meta & 3; }

    @Override public void afterBreak(World world, PlayerEntity player, int x, int y, int z, int meta) {
        var hand = player.getHand();
        // Beta's shears (item 359) take the leaves themselves
        if (!world.isRemote && hand != null && hand.itemId == 359) {
            player.increaseStat(legacyconnector.vb1_7_3.net.minecraft.stat.Stats.MINE_BLOCK[id], 1);
            dropStack(world, x, y, z, new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(id, 1, meta & 3));
        } else super.afterBreak(world, player, x, y, z, meta);
    }

    @Override public boolean isOpaque() {
        // the view of the game's own leaves answers like the native block
        if (mcc$modern != null && !(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block)) return super.isOpaque();
        return !renderSides;
    }

    @Override public int getTexture(int side, int meta) { return (meta & 3) == 1 ? textureId + 80 : textureId; }

    /** Beta's fancy-graphics switch: fast graphics uses the opaque sprite next to the fancy one. */
    public void setFancyGraphics(boolean fancy) {
        renderSides = fancy;
        textureId = spriteIndex + (fancy ? 0 : 1);
    }
}
