package legacyconnector.v1_12_2.net.minecraft.block.properties;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PropertyInteger extends PropertyHelper<Integer> {
    private final ImmutableSet<Integer> allowedValues;

    protected PropertyInteger(String name, int min, int max) {
        super(name, Integer.class);
        Set<Integer> set = new java.util.LinkedHashSet<>();
        for (int i = min; i <= max; i++) set.add(i);
        this.allowedValues = ImmutableSet.copyOf(set);
    }

    public static PropertyInteger create(String name, int min, int max) {
        return new PropertyInteger(name, min, max);
    }

    @Override
    public Collection<Integer> getAllowedValues() {
        return allowedValues;
    }

    @Override
    public com.google.common.base.Optional<Integer> parseValue(String value) {
        try {
            Integer i = Integer.valueOf(value);
            return allowedValues.contains(i) ? com.google.common.base.Optional.of(i) : com.google.common.base.Optional.absent();
        } catch (NumberFormatException e) {
            return com.google.common.base.Optional.absent();
        }
    }

    @Override
    public String getName(Integer value) {
        return value.toString();
    }

    /** The ranges of 1.12.2's vanilla integer properties (class.FIELD -> name, min, max). */
    private static final java.util.Map<String, Object[]> VANILLA = new java.util.HashMap<>();

    static {
        Object[][] table = {{"BlockLiquid.LEVEL", "level", 0, 15}, {"BlockFluidBase.LEVEL", "level", 0, 15}, {"BlockFluidClassic.LEVEL", "level", 0, 15},
                {"BlockFluidFinite.LEVEL", "level", 0, 15}, {"BlockCrops.AGE", "age", 0, 7}, {"BlockCarrot.AGE", "age", 0, 7}, {"BlockPotato.AGE", "age", 0, 7},
                {"BlockBeetroot.BEETROOT_AGE", "age", 0, 3}, {"BlockNetherWart.AGE", "age", 0, 3}, {"BlockCactus.AGE", "age", 0, 15},
                {"BlockReed.AGE", "age", 0, 15}, {"BlockStem.AGE", "age", 0, 7}, {"BlockCocoa.AGE", "age", 0, 2}, {"BlockFarmland.MOISTURE", "moisture", 0, 7},
                {"BlockSnow.LAYERS", "layers", 1, 8}, {"BlockCauldron.LEVEL", "level", 0, 3}, {"BlockCake.BITES", "bites", 0, 6},
                {"BlockRedstoneWire.POWER", "power", 0, 15}, {"BlockRedstoneRepeater.DELAY", "delay", 1, 4}, {"BlockSapling.STAGE", "stage", 0, 1},
                {"BlockFire.AGE", "age", 0, 15}, {"BlockChorusFlower.AGE", "age", 0, 5}, {"BlockStandingSign.ROTATION", "rotation", 0, 15},
                {"BlockBanner.ROTATION", "rotation", 0, 15}, {"BlockPressurePlateWeighted.POWER", "power", 0, 15},
                {"BlockDaylightDetector.POWER", "power", 0, 15}, {"BlockFrostedIce.AGE", "age", 0, 3}, {"BlockAnvil.DAMAGE", "damage", 0, 2}};
        for (Object[] row : table) VANILLA.put((String) row[0], row);
    }

    /**
     * The vanilla property constants the synthesizer cannot know (BlockFarmland.MOISTURE, BlockCrops.AGE...): with their 1.12.2 name and
     * range, else named after the field over the widest range.
     */
    public static PropertyInteger mcc$synthDefault(String key) {
        String field = key.substring(key.lastIndexOf('.', key.lastIndexOf('.') - 1) + 1);
        Object[] row = VANILLA.get(field);
        if (row != null) return create((String) row[1], (Integer) row[2], (Integer) row[3]);
        return create(key.substring(key.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT), 0, 15);
    }
}
