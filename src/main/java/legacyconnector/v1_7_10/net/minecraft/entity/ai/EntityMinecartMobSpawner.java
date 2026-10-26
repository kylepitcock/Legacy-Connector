package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The spawner minecart. Constructing this exact class makes a vanilla spawner cart, which spawns by the modern spawner logic; a
 * mod's subclass only shows the spawner (the spawn logic of the shim belongs to spawner blocks).
 */
public class EntityMinecartMobSpawner extends EntityMinecart {
    public EntityMinecartMobSpawner(World world) {
        super(world);
    }

    public EntityMinecartMobSpawner(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public int getMinecartType() {
        return 4;
    }

    @Override
    public Block getDefaultDisplayTile() {
        return Blocks.mob_spawner;
    }
}
