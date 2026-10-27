package legacyconnector.v1_12_2.net.minecraft.block.properties;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;

public class PropertyEnum<T extends Enum<T> & IStringSerializable> extends PropertyHelper<T> {
    private final ImmutableSet<T> allowedValues;
    private final java.util.Map<String, T> nameToValue;

    protected PropertyEnum(String name, Class<T> valueClass, Collection<T> allowedValues) {
        super(name, valueClass);
        this.allowedValues = ImmutableSet.copyOf(allowedValues);
        ImmutableMap.Builder<String, T> b = ImmutableMap.builder();
        for (T t : allowedValues) b.put(t.getName() != null ? t.getName() : t.name().toLowerCase(java.util.Locale.ROOT), t);
        this.nameToValue = b.build();
    }

    /** The constants of an enum; a synthesized enum has none of its own, so they come from the legacy field list. */
    private static <T extends Enum<T>> T[] constants(Class<T> clazz) {
        T[] values = clazz.getEnumConstants();
        return values != null && values.length > 0 ? values : org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.enumValues(clazz);
    }

    public static <T extends Enum<T> & IStringSerializable> PropertyEnum<T> create(String name, Class<T> clazz) {
        return create(name, clazz, com.google.common.base.Predicates.alwaysTrue());
    }

    public static <T extends Enum<T> & IStringSerializable> PropertyEnum<T> create(String name, Class<T> clazz, Predicate<T> filter) {
        return create(name, clazz, Lists.newArrayList(com.google.common.collect.Collections2.filter(Lists.newArrayList(constants(clazz)), filter)));
    }

    public static <T extends Enum<T> & IStringSerializable> PropertyEnum<T> create(String name, Class<T> clazz, T... values) {
        return create(name, clazz, Lists.newArrayList(values));
    }

    public static <T extends Enum<T> & IStringSerializable> PropertyEnum<T> create(String name, Class<T> clazz, Collection<T> values) {
        return new PropertyEnum<>(name, clazz, values);
    }

    @Override
    public Collection<T> getAllowedValues() {
        return allowedValues;
    }

    @Override
    public com.google.common.base.Optional<T> parseValue(String value) {
        return com.google.common.base.Optional.fromNullable(nameToValue.get(value));
    }

    @Override
    public String getName(T value) {
        return value.getName() != null ? value.getName() : value.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static final java.util.Map<String, String> VANILLA = java.util.Map.ofEntries(
            java.util.Map.entry("BlockLog.LOG_AXIS", "net.minecraft.block.BlockLog$EnumAxis"),
            java.util.Map.entry("BlockRotatedPillar.AXIS", "net.minecraft.util.EnumFacing$Axis"),
            java.util.Map.entry("BlockPlanks.VARIANT", "net.minecraft.block.BlockPlanks$EnumType"),
            java.util.Map.entry("BlockOldLog.VARIANT", "net.minecraft.block.BlockPlanks$EnumType"),
            java.util.Map.entry("BlockNewLog.VARIANT", "net.minecraft.block.BlockPlanks$EnumType"),
            java.util.Map.entry("BlockOldLeaf.VARIANT", "net.minecraft.block.BlockPlanks$EnumType"),
            java.util.Map.entry("BlockNewLeaf.VARIANT", "net.minecraft.block.BlockPlanks$EnumType"),
            java.util.Map.entry("BlockStone.VARIANT", "net.minecraft.block.BlockStone$EnumType"),
            java.util.Map.entry("BlockDirt.VARIANT", "net.minecraft.block.BlockDirt$DirtType"),
            java.util.Map.entry("BlockSand.VARIANT", "net.minecraft.block.BlockSand$EnumType"),
            java.util.Map.entry("BlockStoneBrick.VARIANT", "net.minecraft.block.BlockStoneBrick$EnumType"),
            java.util.Map.entry("BlockSandStone.TYPE", "net.minecraft.block.BlockSandStone$EnumType"),
            java.util.Map.entry("BlockQuartz.VARIANT", "net.minecraft.block.BlockQuartz$EnumType"),
            java.util.Map.entry("BlockSlab.HALF", "net.minecraft.block.BlockSlab$EnumBlockHalf"),
            java.util.Map.entry("BlockStairs.HALF", "net.minecraft.block.BlockStairs$EnumHalf"),
            java.util.Map.entry("BlockStairs.SHAPE", "net.minecraft.block.BlockStairs$EnumShape"),
            java.util.Map.entry("BlockDoor.HALF", "net.minecraft.block.BlockDoor$EnumDoorHalf"),
            java.util.Map.entry("BlockDoor.HINGE", "net.minecraft.block.BlockDoor$EnumHingePosition"),
            java.util.Map.entry("BlockColored.COLOR", "net.minecraft.item.EnumDyeColor"),
            java.util.Map.entry("BlockRailBase.SHAPE", "net.minecraft.block.BlockRailBase$EnumRailDirection"),
            java.util.Map.entry("BlockStoneSlab.VARIANT", "net.minecraft.block.BlockStoneSlab$EnumType"),
            java.util.Map.entry("BlockWoodSlab.VARIANT", "net.minecraft.block.BlockPlanks$EnumType"),
            java.util.Map.entry("BlockSapling.TYPE", "net.minecraft.block.BlockPlanks$EnumType"),
            java.util.Map.entry("BlockBed.PART", "net.minecraft.block.BlockBed$EnumPartType"),
            java.util.Map.entry("BlockTallGrass.TYPE", "net.minecraft.block.BlockTallGrass$EnumType"),
            java.util.Map.entry("BlockDoublePlant.VARIANT", "net.minecraft.block.BlockDoublePlant$EnumPlantType"),
            java.util.Map.entry("BlockDoublePlant.HALF", "net.minecraft.block.BlockDoublePlant$EnumBlockHalf"),
            java.util.Map.entry("BlockTrapDoor.HALF", "net.minecraft.block.BlockTrapDoor$DoorHalf"),
            java.util.Map.entry("BlockPrismarine.VARIANT", "net.minecraft.block.BlockPrismarine$EnumType"),
            java.util.Map.entry("BlockWall.VARIANT", "net.minecraft.block.BlockWall$EnumType"),
            java.util.Map.entry("BlockPistonExtension.TYPE", "net.minecraft.block.BlockPistonExtension$EnumPistonType"),
            java.util.Map.entry("BlockPistonMoving.TYPE", "net.minecraft.block.BlockPistonExtension$EnumPistonType"),
            java.util.Map.entry("BlockRedstoneComparator.MODE", "net.minecraft.block.BlockRedstoneComparator$Mode"),
            java.util.Map.entry("BlockFlowerPot.CONTENTS", "net.minecraft.block.BlockFlowerPot$EnumFlowerType"),
            java.util.Map.entry("BlockRedSandstone.TYPE", "net.minecraft.block.BlockRedSandstone$EnumType"),
            java.util.Map.entry("BlockStoneSlabNew.VARIANT", "net.minecraft.block.BlockStoneSlabNew$EnumType"),
            java.util.Map.entry("BlockRail.SHAPE", "net.minecraft.block.BlockRailBase$EnumRailDirection"),
            java.util.Map.entry("BlockRailPowered.SHAPE", "net.minecraft.block.BlockRailBase$EnumRailDirection"),
            java.util.Map.entry("BlockRailDetector.SHAPE", "net.minecraft.block.BlockRailBase$EnumRailDirection"),
            java.util.Map.entry("BlockStainedGlass.COLOR", "net.minecraft.item.EnumDyeColor"),
            java.util.Map.entry("BlockStainedGlassPane.COLOR", "net.minecraft.item.EnumDyeColor"),
            java.util.Map.entry("BlockCarpet.COLOR", "net.minecraft.item.EnumDyeColor"),
            java.util.Map.entry("BlockConcretePowder.COLOR", "net.minecraft.item.EnumDyeColor"));

    /** Vanilla enum properties (BlockLog.LOG_AXIS, ...): over the constants of the enum class they hold, which the synthesizer made. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static PropertyEnum mcc$synthDefault(String key) {
        String field = key.substring(key.lastIndexOf('.', key.lastIndexOf('.') - 1) + 1);
        String enumClass = VANILLA.get(field);
        if (enumClass == null) return null;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (String prefix : new String[] {"legacyconnector_synth.v1_12_2.", "legacyconnector.v1_12_2."}) {
            try {
                Class<?> cls = Class.forName(prefix + enumClass, true, loader);
                if (!cls.isEnum() && cls.getSuperclass() != Enum.class) continue;
                return create(key.substring(key.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT).replace("log_axis", "axis"), (Class) cls);
            } catch (ClassNotFoundException | LinkageError | RuntimeException e) {
                // try the other root
            }
        }
        return null;
    }
}
