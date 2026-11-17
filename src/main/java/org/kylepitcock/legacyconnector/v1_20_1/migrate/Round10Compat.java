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

    // ---- maps ---------------------------------------------------------------------------------------------------------------------------------

    /** {@code MapItemSavedData.load(tag)}: 1.21.1 reads the decorations' names through the registry provider (the one the layer remembers for this thread). */
    public static net.minecraft.world.level.saveddata.maps.MapItemSavedData mapLoad(net.minecraft.nbt.CompoundTag tag) {
        return net.minecraft.world.level.saveddata.maps.MapItemSavedData.load(tag, MigrateContext.provider());
    }

    /** {@code MapItemSavedData.getUpdatePacket(int mapId, player)}: map ids are {@code MapId} objects. */
    public static net.minecraft.network.protocol.Packet<?> mapUpdatePacket(net.minecraft.world.level.saveddata.maps.MapItemSavedData data, int mapId,
            net.minecraft.world.entity.player.Player player) {
        return data.getUpdatePacket(new net.minecraft.world.level.saveddata.maps.MapId(mapId), player);
    }

    /** {@code level.setMapData("map_5", data)}: the key was the saved-data name, {@code map_} followed by the id. */
    public static void setMapData(net.minecraft.world.level.Level level, String name, net.minecraft.world.level.saveddata.maps.MapItemSavedData data) {
        int id = 0;
        try {
            id = Integer.parseInt(name.startsWith("map_") ? name.substring(4) : name);
        } catch (NumberFormatException e) {
            // not a map name: id 0
        }
        level.setMapData(new net.minecraft.world.level.saveddata.maps.MapId(id), data);
    }

    /** {@code MapDecoration.getName()}: the optional name of the record, or null. */
    public static net.minecraft.network.chat.Component decorationName(net.minecraft.world.level.saveddata.maps.MapDecoration decoration) {
        return decoration.name().orElse(null);
    }

    /** {@code MapDecoration.getType()}: the legacy enum constant standing for the decoration's type (null for a type the 1.20.1 enum does not know). */
    public static LegacyMapDecorationType decorationType(net.minecraft.world.level.saveddata.maps.MapDecoration decoration) {
        for (LegacyMapDecorationType type : LegacyMapDecorationType.values()) {
            try {
                if (type.holder().equals(decoration.type())) return type;
            } catch (IllegalStateException e) {
                // a constant 1.21.1 does not have
            }
        }
        return null;
    }

    // ---- skulls -------------------------------------------------------------------------------------------------------------------------------

    /** {@code SkullBlockEntity.getOwnerProfile()}: the game profile of the owner (1.21.1 keeps a {@code ResolvableProfile}). */
    public static com.mojang.authlib.GameProfile skullOwner(net.minecraft.world.level.block.entity.SkullBlockEntity skull) {
        net.minecraft.world.item.component.ResolvableProfile profile = skull.getOwnerProfile();
        return profile == null ? null : profile.gameProfile();
    }

    /** {@code SkullBlockEntity.setOwner(GameProfile)}. */
    public static void skullSetOwner(net.minecraft.world.level.block.entity.SkullBlockEntity skull, com.mojang.authlib.GameProfile profile) {
        skull.setOwner(profile == null ? null : new net.minecraft.world.item.component.ResolvableProfile(profile));
    }

    // ---- stem grown blocks --------------------------------------------------------------------------------------------------------------------

    /** {@code StemGrownBlock.getStem()} (the class is {@code Block} now): the stem of the vanilla melon and pumpkin, null for another block. */
    public static net.minecraft.world.level.block.StemBlock stemOf(net.minecraft.world.level.block.Block block) {
        if (block == net.minecraft.world.level.block.Blocks.MELON) return (net.minecraft.world.level.block.StemBlock) net.minecraft.world.level.block.Blocks.MELON_STEM;
        if (block == net.minecraft.world.level.block.Blocks.PUMPKIN) return (net.minecraft.world.level.block.StemBlock) net.minecraft.world.level.block.Blocks.PUMPKIN_STEM;
        return null;
    }

    /** {@code StemGrownBlock.getAttachedStem()}. */
    public static net.minecraft.world.level.block.AttachedStemBlock attachedStemOf(net.minecraft.world.level.block.Block block) {
        if (block == net.minecraft.world.level.block.Blocks.MELON) return (net.minecraft.world.level.block.AttachedStemBlock) net.minecraft.world.level.block.Blocks.ATTACHED_MELON_STEM;
        if (block == net.minecraft.world.level.block.Blocks.PUMPKIN) return (net.minecraft.world.level.block.AttachedStemBlock) net.minecraft.world.level.block.Blocks.ATTACHED_PUMPKIN_STEM;
        return null;
    }

    // ---- combat, enchantments, model data, inventory ------------------------------------------------------------------------------------------

    /** {@code CombatRules.getDamageAfterAbsorb(damage, armor, toughness)} of 1.20.1: the original formula (1.21.1 also takes the entity and the damage source for the enchantments). */
    public static float damageAfterAbsorb(float damage, float armor, float toughness) {
        float f = 2.0F + toughness / 4.0F;
        float f1 = net.minecraft.util.Mth.clamp(armor - damage / f, armor * 0.2F, 20.0F);
        return damage * (1.0F - f1 / 25.0F);
    }

    /** {@code EnchantmentHelper.isEnchantmentCompatible(existing, candidate)}: no enchantment of the collection rejects the candidate. */
    public static boolean isEnchantmentCompatible(java.util.Collection<LegacyEnchantment> existing, LegacyEnchantment candidate) {
        for (LegacyEnchantment e : existing) {
            if (e != candidate && !candidate.isCompatibleWith(e)) return false;
        }
        return true;
    }

    /** {@code level.getModelDataManager()} (Forge's extension of {@code BlockAndTintGetter}): the client level's manager, null for other getters (they hold no model data). */
    public static net.neoforged.neoforge.client.model.data.ModelDataManager modelDataManager(net.minecraft.world.level.BlockGetter getter) {
        return getter instanceof net.minecraft.client.multiplayer.ClientLevel level ? level.getModelDataManager() : null;
    }

    public static net.neoforged.neoforge.client.model.data.ModelDataManager modelDataManager(net.minecraft.world.level.BlockAndTintGetter getter) {
        return modelDataManager((net.minecraft.world.level.BlockGetter) getter);
    }

    /** {@code InventoryMenu.TEXTURE_EMPTY_SLOTS}: the empty-slot icons of boots, leggings, chestplate and helmet. */
    public static net.minecraft.resources.ResourceLocation[] emptySlotTextures() {
        return new net.minecraft.resources.ResourceLocation[] {net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
                net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
                net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};
    }

    /** {@code ProjectileUtil.getMobArrow(shooter, arrow, velocity)}: 1.21.1 also passes the weapon (none here). */
    public static net.minecraft.world.entity.projectile.AbstractArrow mobArrow(net.minecraft.world.entity.LivingEntity shooter, net.minecraft.world.item.ItemStack arrow, float velocity) {
        return net.minecraft.world.entity.projectile.ProjectileUtil.getMobArrow(shooter, arrow, velocity, null);
    }
}
