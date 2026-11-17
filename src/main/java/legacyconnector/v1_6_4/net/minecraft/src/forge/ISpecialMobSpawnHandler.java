package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface ISpecialMobSpawnHandler {
    /** A mob was placed by the spawner; true takes over its special initialisation (the game's own is skipped). */
    boolean onSpecialEntitySpawn(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, legacyconnector.v1_7_10.net.minecraft.world.World world, float x, float y, float z);
}
