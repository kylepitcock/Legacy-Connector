package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.BlockSlab;

/** The item of a slab: placing it on a half slab of the same kind makes the double slab. */
public class ItemSlab extends ItemBlock {
    private final boolean field_150948_b;
    private final BlockSlab field_150949_c;
    private final BlockSlab field_150947_d;

    public ItemSlab(Block block, BlockSlab single, BlockSlab full, boolean isFull) {
        super(block);
        this.field_150949_c = single;
        this.field_150947_d = full;
        this.field_150948_b = isFull;
    }

    @Override
    public boolean onItemUse(ItemStack stack, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (field_150949_c == null || field_150948_b) return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        if (stack.mcc$count() == 0) return false;
        if (!player.canPlayerEdit(x, y, z, side, stack)) return false;
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        int kind = meta & 7;
        boolean upper = (meta & 8) != 0;
        if ((side == 1 && !upper || side == 0 && upper) && block == field_150949_c && kind == stack.getItemDamage()) {
            makeDouble(stack, world, x, y, z, kind);
            return true;
        }
        return func_150946_a(stack, player, world, x, y, z, side) || super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    /** Puts the double slab where the half slab of this kind is (unless something stands in the way) and uses up one item. */
    private void makeDouble(ItemStack stack, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int kind) {
        if (world.checkNoEntityCollision(field_150947_d.getCollisionBoundingBoxFromPool(world, x, y, z)) && world.setBlock(x, y, z, field_150947_d, kind, 3)) {
            Block.SoundType sound = field_150947_d.stepSound;
            world.playSoundEffect(x + 0.5F, y + 0.5F, z + 0.5F, sound.getPlaceSound(), (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            ItemStack.mcc$set$stackSize(stack, ItemStack.mcc$get$stackSize(stack) - 1);
        }
    }

    /** Tries the block next to the clicked face: a half slab of this kind there is turned into the double one. */
    private boolean func_150946_a(ItemStack stack, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int side) {
        switch (side) {
            case 0 -> y--;
            case 1 -> y++;
            case 2 -> z--;
            case 3 -> z++;
            case 4 -> x--;
            case 5 -> x++;
            default -> { }
        }
        Block block = world.getBlock(x, y, z);
        int kind = world.getBlockMetadata(x, y, z) & 7;
        if (block == field_150949_c && kind == stack.getItemDamage()) {
            makeDouble(stack, world, x, y, z, kind);
            return true;
        }
        return false;
    }

    @Override
    public boolean func_150936_a(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int side,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, ItemStack stack) {
        if (field_150949_c == null || field_150948_b) return super.func_150936_a(world, x, y, z, side, player, stack);
        int meta = world.getBlockMetadata(x, y, z);
        boolean upper = (meta & 8) != 0;
        if ((side == 1 && !upper || side == 0 && upper) && world.getBlock(x, y, z) == field_150949_c && (meta & 7) == stack.getItemDamage()) return true;
        int nx = x;
        int ny = y;
        int nz = z;
        switch (side) {
            case 0 -> ny--;
            case 1 -> ny++;
            case 2 -> nz--;
            case 3 -> nz++;
            case 4 -> nx--;
            case 5 -> nx++;
            default -> { }
        }
        return world.getBlock(nx, ny, nz) == field_150949_c && (world.getBlockMetadata(nx, ny, nz) & 7) == stack.getItemDamage()
                || super.func_150936_a(world, x, y, z, side, player, stack);
    }

    public ItemSlab(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info, Block block) {
        super(info, block);
        this.field_150949_c = null;
        this.field_150947_d = null;
        this.field_150948_b = false;
    }
}
