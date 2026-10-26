package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityList;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A spawn egg: its damage is the legacy entity id of a mob registered with an egg ({@code EntityList.entityEggs}). */
public class ItemMonsterPlacer extends Item {
    public ItemMonsterPlacer(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemMonsterPlacer() {
        super();
        setHasSubtypes(true);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabMisc);
    }

    /** Spawns the egg mob {@code id} at the position (random yaw, egg spawn data, living sound); null for ids without an egg. */
    public static Entity spawnCreature(World world, int id, double x, double y, double z) {
        if (world == null || !EntityList.entityEggs.containsKey(id)) return null;
        Entity entity = EntityList.createEntityByID(id, world);
        if (entity instanceof EntityLiving living) {
            float yaw = legacyconnector.v1_7_10.net.minecraft.util.MathHelper.wrapAngleTo180_float(world.rand.nextFloat() * 360.0F);
            entity.setLocationAndAngles(x, y, z, yaw, 0.0F);
            legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase.mcc$set$rotationYawHead(living, yaw);
            living.onSpawnWithEgg(null);
            world.spawnEntityInWorld(entity);
            living.playLivingSound();
        }
        return entity;
    }

    /** Spawns the egg's mob against the clicked face (half a block up on fences). */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (mcc$vanilla != null) return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        if (world.isRemote) return true;
        legacyconnector.v1_7_10.net.minecraft.block.Block block = world.getBlock(x, y, z);
        int[][] offsets = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
        if (side >= 0 && side < 6) {
            x += offsets[side][0];
            y += offsets[side][1];
            z += offsets[side][2];
        }
        double offset = side == 1 && block.getRenderType() == 11 ? 0.5D : 0.0D;
        Entity entity = spawnCreature(world, stack.getItemDamage(), x + 0.5D, y + offset, z + 0.5D);
        if (entity != null) {
            if (entity instanceof EntityLiving living && stack.hasDisplayName()) living.setCustomNameTag(stack.getDisplayName());
            if (!legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) {
                stack.mcc$setCount(stack.mcc$count() - 1);
            }
        }
        return true;
    }
}
