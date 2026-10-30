package legacyconnector.v1_12_2.net.minecraft.item;

/** 1.12.2 compass: its "angle" model property points at the world spawn (spinning where there is none, as in the Nether). */
public class ItemCompass extends Item {
    public ItemCompass() {
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.TOOLS);
        addPropertyOverride(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("angle"), (stack, world, entity) -> {
            if (entity == null || entity.mcc$entity == null) return 0.0F;
            var e = entity.mcc$entity;
            var level = e.level();
            if (!level.dimensionType().natural()) return (float) Math.random();
            var spawn = level.getSharedSpawnPos();
            double yaw = net.minecraft.util.Mth.positiveModulo(e.getYRot() / 360.0D, 1.0D);
            double toSpawn = Math.atan2(spawn.getZ() + 0.5D - e.getZ(), spawn.getX() + 0.5D - e.getX()) / (Math.PI * 2D);
            return (float) net.minecraft.util.Mth.positiveModulo(0.5D - (yaw - 0.25D - toSpawn), 1.0D);
        });
    }
}
