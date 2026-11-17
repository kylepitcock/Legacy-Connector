package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.Vec3;

/**
 * Common-side helpers of gap round 10 (1.20.1 mods on 1.21.1). Reached from {@code mc-calls.tsv}, {@code mc-fields.tsv} and {@code mc-bridges.tsv}.
 */
public final class Round10Compat {
    private Round10Compat() {}

    // ---- AbstractHurtingProjectile.xPower / yPower / zPower -----------------------------------------------------------------------------------

    /** The acceleration vector a mod wrote last, per projectile (a field of three doubles in 1.20.1; the object is gone in 1.21.1). */
    private static final Map<AbstractHurtingProjectile, double[]> POWER = new WeakHashMap<>();

    private static synchronized double[] power(AbstractHurtingProjectile p) {
        return POWER.computeIfAbsent(p, k -> {
            Vec3 d = k.getDeltaMovement();
            double len = d.length();
            return len < 1.0E-7 ? new double[3] : new double[] {d.x / len * k.accelerationPower, d.y / len * k.accelerationPower, d.z / len * k.accelerationPower};
        });
    }

    /**
     * {@code xPower/yPower/zPower} read: 1.20.1 added this vector to the motion every tick; 1.21.1 accelerates along the current motion by
     * {@code accelerationPower}. A vector a mod wrote is answered as written, otherwise it is derived from the motion direction and the power.
     */
    public static double getPower(AbstractHurtingProjectile p, int axis) {
        return power(p)[axis];
    }

    /**
     * {@code xPower/yPower/zPower} written. APPROXIMATION: {@code accelerationPower} becomes the length of the vector and the motion is turned to the vector's
     * direction (keeping its speed, or taking the vector's length when the projectile stands still), because 1.21.1 only accelerates along the motion.
     */
    public static void setPower(AbstractHurtingProjectile p, int axis, double value) {
        double[] v = power(p);
        v[axis] = value;
        double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        p.accelerationPower = len;
        if (len < 1.0E-7) return;
        double speed = p.getDeltaMovement().length();
        if (speed < 1.0E-7) speed = len;
        p.setDeltaMovement(new Vec3(v[0] / len * speed, v[1] / len * speed, v[2] / len * speed));
        p.hasImpulse = true;
    }

    public static double getXPower(AbstractHurtingProjectile p) {
        return getPower(p, 0);
    }

    public static double getYPower(AbstractHurtingProjectile p) {
        return getPower(p, 1);
    }

    public static double getZPower(AbstractHurtingProjectile p) {
        return getPower(p, 2);
    }

    public static void setXPower(AbstractHurtingProjectile p, double value) {
        setPower(p, 0, value);
    }

    public static void setYPower(AbstractHurtingProjectile p, double value) {
        setPower(p, 1, value);
    }

    public static void setZPower(AbstractHurtingProjectile p, double value) {
        setPower(p, 2, value);
    }

    // ---- PathType.getDanger -------------------------------------------------------------------------------------------------------------------

    /**
     * {@code BlockPathTypes.getDanger()} (Forge): the type a path finder treats as "dangerous to stand next to", null for a safe one. APPROXIMATION: derived
     * from the 1.21.1 constants (the danger variant of fire, powder snow, trapdoor and other damaging types; a type already a danger or damage type answers
     * itself), because 1.21.1 keeps no such link.
     */
    public static net.minecraft.world.level.pathfinder.PathType pathDanger(net.minecraft.world.level.pathfinder.PathType type) {
        if (type == null) return null;
        return switch (type) {
            case LAVA, DAMAGE_FIRE -> net.minecraft.world.level.pathfinder.PathType.DANGER_FIRE;
            case DAMAGE_OTHER, DAMAGE_CAUTIOUS -> net.minecraft.world.level.pathfinder.PathType.DANGER_OTHER;
            case POWDER_SNOW -> net.minecraft.world.level.pathfinder.PathType.DANGER_POWDER_SNOW;
            case TRAPDOOR -> net.minecraft.world.level.pathfinder.PathType.DANGER_TRAPDOOR;
            case DANGER_FIRE, DANGER_OTHER, DANGER_POWDER_SNOW, DANGER_TRAPDOOR -> type;
            default -> null;
        };
    }

    // ---- small constructors and factories -----------------------------------------------------------------------------------------------------

    /** {@code SmithingTemplateItem.createArmorTrimTemplate(ResourceKey<TrimPattern>)}: 1.21.1 takes feature flags after it (none here). */
    public static net.minecraft.world.item.SmithingTemplateItem armorTrimTemplate(net.minecraft.resources.ResourceKey<net.minecraft.world.item.armortrim.TrimPattern> pattern) {
        return net.minecraft.world.item.SmithingTemplateItem.createArmorTrimTemplate(pattern);
    }

    /** {@code new EntitySelectorParser(reader)}: selectors are allowed (the 1.21.1 constructor has the flag the 1.20.1 one fixed to true). */
    public static net.minecraft.commands.arguments.selector.EntitySelectorParser newSelectorParser(com.mojang.brigadier.StringReader reader) {
        return new net.minecraft.commands.arguments.selector.EntitySelectorParser(reader, true);
    }

    /** {@code new FilePackResources(id, file, isBuiltin)}: 1.21.1 describes the pack with a {@code PackLocationInfo} and reads the zip through a shared access. */
    public static net.minecraft.server.packs.FilePackResources newFilePack(String id, java.io.File file, boolean builtin) {
        net.minecraft.server.packs.PackLocationInfo info = new net.minecraft.server.packs.PackLocationInfo(id, net.minecraft.network.chat.Component.literal(id),
                builtin ? net.minecraft.server.packs.repository.PackSource.BUILT_IN : net.minecraft.server.packs.repository.PackSource.DEFAULT, java.util.Optional.empty());
        return new net.minecraft.server.packs.FilePackResources(info, new net.minecraft.server.packs.FilePackResources.SharedZipFileAccess(file), "");
    }
}
