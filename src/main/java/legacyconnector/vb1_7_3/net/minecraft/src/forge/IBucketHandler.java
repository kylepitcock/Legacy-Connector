package legacyconnector.vb1_7_3.net.minecraft.src.forge;

import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

public interface IBucketHandler {
    ItemStack fillCustomBucket(World world,int x,int y,int z);
}
