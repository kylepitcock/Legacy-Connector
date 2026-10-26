package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.FillBucketEvent;

/**
 * A bucket carrying a fluid block ({@code Blocks.air} when empty), ported from 1.7.10: right-click fills an empty bucket from a
 * still water or lava source (or whatever a {@link FillBucketEvent} handler gives) and empties a full one into the world.
 */
public class ItemBucket extends Item {
    private Block isFull;

    public ItemBucket(Block containedBlock) {
        this.isFull = containedBlock;
        this.maxStackSize = 1;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabMisc);
    }

    public ItemBucket(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public Block mcc$contained() {
        if (isFull == null && mcc$vanilla != null) {
            // vanilla wrappers: the block their 1.7.10 counterpart was constructed with
            String name = mcc$vanilla.legacyName();
            if (name.endsWith("water_bucket")) isFull = Blocks.flowing_water;
            else if (name.endsWith("lava_bucket")) isFull = Blocks.flowing_lava;
            else isFull = Blocks.air;
        }
        return isFull;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        boolean empty = mcc$contained() == Blocks.air;
        MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, empty);
        if (hit == null) return stack;
        FillBucketEvent event = new FillBucketEvent(player, stack, world, hit);
        if (MinecraftForge.EVENT_BUS.post(event)) return stack;
        boolean creative = PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities);
        if (event.getResult() == Event.Result.ALLOW) {
            if (creative) return stack;
            stack.mcc$setCount(stack.mcc$count() - 1);
            if (stack.mcc$count() <= 0) return event.result;
            if (!player.inventory.addItemStackToInventory(event.result)) player.dropPlayerItemWithRandomChoice(event.result, false);
            return stack;
        }
        if (hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return stack;
        int x = hit.blockX;
        int y = hit.blockY;
        int z = hit.blockZ;
        if (!world.canMineBlock(player, x, y, z)) return stack;
        if (empty) {
            if (!player.canPlayerEdit(x, y, z, hit.sideHit, stack)) return stack;
            Material material = world.getBlock(x, y, z).getMaterial();
            int meta = world.getBlockMetadata(x, y, z);
            if (material == Material.water && meta == 0) {
                world.setBlockToAir(x, y, z);
                return func_150910_a(stack, player, Items.water_bucket);
            }
            if (material == Material.lava && meta == 0) {
                world.setBlockToAir(x, y, z);
                return func_150910_a(stack, player, Items.lava_bucket);
            }
        } else {
            switch (hit.sideHit) {
                case 0 -> --y;
                case 1 -> ++y;
                case 2 -> --z;
                case 3 -> ++z;
                case 4 -> --x;
                case 5 -> ++x;
                default -> {
                }
            }
            if (!player.canPlayerEdit(x, y, z, hit.sideHit, stack)) return stack;
            if (tryPlaceContainedLiquid(world, x, y, z) && !creative) return new ItemStack(Items.bucket);
        }
        return stack;
    }

    /** Swaps one empty bucket for a filled one (kept in the inventory, or dropped when it is full). */
    private ItemStack func_150910_a(ItemStack stack, EntityPlayer player, Item filled) {
        if (PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) return stack;
        stack.mcc$setCount(stack.mcc$count() - 1);
        if (stack.mcc$count() <= 0) return new ItemStack(filled);
        if (!player.inventory.addItemStackToInventory(new ItemStack(filled))) player.dropPlayerItemWithRandomChoice(new ItemStack(filled, 1, 0), false);
        return stack;
    }

    /**
     * Places the bucket's fluid at the position when it is air or a non-solid block (which is broken and dropped first); water
     * evaporates with a fizz in the Nether. False when the bucket is empty or the position is solid.
     */
    public boolean tryPlaceContainedLiquid(World world, int x, int y, int z) {
        Block contained = mcc$contained();
        if (contained == null || contained == Blocks.air) return false;
        Material material = world.getBlock(x, y, z).getMaterial();
        boolean notSolid = !material.isSolid();
        if (!world.isAirBlock(x, y, z) && !notSolid) return false;
        if (World.mcc$get$provider(world).isHellWorld && contained == Blocks.flowing_water) {
            world.playSoundEffect(x + 0.5F, y + 0.5F, z + 0.5F, "random.fizz", 0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
            for (int i = 0; i < 8; ++i) world.spawnParticle("largesmoke", x + Math.random(), y + Math.random(), z + Math.random(), 0.0D, 0.0D, 0.0D);
        } else {
            if (!world.isRemote && notSolid && !material.isLiquid()) world.breakBlock(x, y, z, true);
            world.setBlock(x, y, z, contained, 0, 3);
        }
        return true;
    }
}
