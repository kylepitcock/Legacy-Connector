package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** A pressure plate: pressed by everything, by mobs, or by players only. */
public class BlockPressurePlate extends BlockBasePressurePlate {
    public enum Sensitivity {
        everything, mobs, players
    }

    private final Sensitivity sensitivity;

    public BlockPressurePlate(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.sensitivity = "minecraft:stone_pressure_plate".equals(info.legacyName()) ? Sensitivity.mobs : Sensitivity.everything;
    }

    public BlockPressurePlate(String texture, Material material, Sensitivity sensitivity) {
        super(texture, material);
        this.sensitivity = sensitivity == null ? Sensitivity.everything : sensitivity;
    }

    @Override
    protected int getMetaFromPower(int power) {
        return power > 0 ? 1 : 0;
    }

    @Override
    protected int getPowerFromMeta(int meta) {
        return meta == 1 ? 15 : 0;
    }

    /** 15 while an entity of the plate's kind (any, living, players) that triggers plates stands on it, else 0. */
    @Override
    protected int getPlateState(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        var box = getSensitiveAABB(x, y, z);
        // no switch: EnumHelper may add constants after javac built its switch map
        java.util.List<?> entities;
        if (sensitivity == Sensitivity.mobs) entities = world.getEntitiesWithinAABB(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase.class, box);
        else if (sensitivity == Sensitivity.players) entities = world.getEntitiesWithinAABB(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class, box);
        else entities = world.getEntitiesWithinAABBExcludingEntity(null, box);
        if (entities != null) {
            for (Object o : entities) {
                if (o instanceof legacyconnector.v1_7_10.net.minecraft.entity.Entity e && !e.doesEntityNotTriggerPressurePlate()) return 15;
            }
        }
        return 0;
    }

    public Sensitivity mcc$sensitivity() {
        return sensitivity;
    }
}
