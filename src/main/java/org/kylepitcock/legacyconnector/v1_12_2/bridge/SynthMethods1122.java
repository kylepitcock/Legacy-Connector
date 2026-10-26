package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.kylepitcock.legacyconnector.core.synth.Synth;

/**
 * Methods of 1.12.2 vanilla classes that only the synthesizer provides, answered with their real behaviour through
 * {@link Synth#addInstanceMethod} / {@link Synth#addStaticMethod}: the names and metadata of vanilla block enums
 * ({@code BlockPlanks.EnumType.DARK_OAK.getName()} is "dark_oak", its metadata 5) and Forge model math.
 */
public final class SynthMethods1122 {
    private SynthMethods1122() {}

    /** A vanilla enum constant: its IStringSerializable name, unlocalized name and metadata (declaration index). */
    private record Constant(String name, String unlocalized, int meta) {}

    /** legacy class (dotted, with $) -> constant name -> info, in declaration order. */
    private static final Map<String, Map<String, Constant>> ENUMS = new HashMap<>();
    private static final Map<String, List<String>> ORDER = new HashMap<>();

    /** Each entry is "CONSTANT name [unlocalizedName]"; the metadata is the entry's index. */
    private static void define(String legacyClass, String... entries) {
        Map<String, Constant> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < entries.length; i++) {
            String[] p = entries[i].split(" ");
            m.put(p[0], new Constant(p[1], p.length > 2 ? p[2] : p[1], i));
        }
        ENUMS.put(legacyClass, m);
        ORDER.put(legacyClass, List.copyOf(m.keySet()));
    }

    static {
        String b = "net.minecraft.block.";
        define(b + "BlockPlanks$EnumType", "OAK oak", "SPRUCE spruce", "BIRCH birch", "JUNGLE jungle", "ACACIA acacia", "DARK_OAK dark_oak big_oak");
        define(b + "BlockStone$EnumType", "STONE stone", "GRANITE granite", "GRANITE_SMOOTH smooth_granite graniteSmooth", "DIORITE diorite",
                "DIORITE_SMOOTH smooth_diorite dioriteSmooth", "ANDESITE andesite", "ANDESITE_SMOOTH smooth_andesite andesiteSmooth");
        define(b + "BlockStoneBrick$EnumType", "DEFAULT stonebrick default", "MOSSY mossy_stonebrick mossy", "CRACKED cracked_stonebrick cracked",
                "CHISELED chiseled_stonebrick chiseled");
        define(b + "BlockSlab$EnumBlockHalf", "TOP top", "BOTTOM bottom");
        define(b + "BlockSandStone$EnumType", "DEFAULT sandstone default", "CHISELED chiseled_sandstone chiseled", "SMOOTH smooth_sandstone smooth");
        define(b + "BlockRedSandstone$EnumType", "DEFAULT red_sandstone default", "CHISELED chiseled_red_sandstone chiseled",
                "SMOOTH smooth_red_sandstone smooth");
        define(b + "BlockSand$EnumType", "SAND sand default", "RED_SAND red_sand red");
        define(b + "BlockQuartz$EnumType", "DEFAULT default", "CHISELED chiseled", "LINES_Y lines_y lines", "LINES_X lines_x lines", "LINES_Z lines_z lines");
        define(b + "BlockLog$EnumAxis", "X x", "Y y", "Z z", "NONE none");
        define(b + "BlockDoublePlant$EnumPlantType", "SUNFLOWER sunflower", "SYRINGA syringa", "GRASS double_grass grass", "FERN double_fern fern",
                "ROSE double_rose rose", "PAEONIA paeonia");
        define(b + "BlockDoublePlant$EnumBlockHalf", "UPPER upper", "LOWER lower");
        define(b + "BlockDoor$EnumHingePosition", "LEFT left", "RIGHT right");
        define(b + "BlockDoor$EnumDoorHalf", "UPPER upper", "LOWER lower");
        define(b + "BlockTrapDoor$DoorHalf", "TOP top", "BOTTOM bottom");
        define(b + "BlockBed$EnumPartType", "HEAD head", "FOOT foot");
        define(b + "BlockDirt$DirtType", "DIRT dirt default", "COARSE_DIRT coarse_dirt coarse", "PODZOL podzol");
        define(b + "BlockTallGrass$EnumType", "DEAD_BUSH dead_bush", "GRASS tall_grass", "FERN fern");
        define(b + "BlockFlowerPot$EnumFlowerType", "EMPTY empty", "POPPY rose", "BLUE_ORCHID blue_orchid", "ALLIUM allium", "HOUSTONIA houstonia",
                "RED_TULIP red_tulip", "ORANGE_TULIP orange_tulip", "WHITE_TULIP white_tulip", "PINK_TULIP pink_tulip", "OXEYE_DAISY oxeye_daisy",
                "DANDELION dandelion", "OAK_SAPLING oak_sapling", "SPRUCE_SAPLING spruce_sapling", "BIRCH_SAPLING birch_sapling",
                "JUNGLE_SAPLING jungle_sapling", "ACACIA_SAPLING acacia_sapling", "DARK_OAK_SAPLING dark_oak_sapling", "MUSHROOM_RED mushroom_red",
                "MUSHROOM_BROWN mushroom_brown", "DEAD_BUSH dead_bush", "FERN fern", "CACTUS cactus");
        define(b + "BlockStoneSlab$EnumType", "STONE stone", "SAND sandstone sand", "WOOD wood_old wood", "COBBLESTONE cobblestone cobble",
                "BRICK brick", "SMOOTHBRICK stone_brick smoothStoneBrick", "NETHERBRICK nether_brick netherBrick", "QUARTZ quartz");
        define(b + "BlockStoneSlabNew$EnumType", "RED_SANDSTONE red_sandstone");
        define(b + "BlockPrismarine$EnumType", "ROUGH prismarine rough", "BRICKS prismarine_bricks bricks", "DARK dark_prismarine dark");
        define(b + "BlockWall$EnumType", "NORMAL cobblestone normal", "MOSSY mossy_cobblestone mossy");
        define(b + "BlockPistonExtension$EnumPistonType", "DEFAULT normal", "STICKY sticky");
        define(b + "BlockRedstoneComparator$Mode", "COMPARE compare", "SUBTRACT subtract");
        define(b + "BlockRailBase$EnumRailDirection", "NORTH_SOUTH north_south", "EAST_WEST east_west", "ASCENDING_EAST ascending_east",
                "ASCENDING_WEST ascending_west", "ASCENDING_NORTH ascending_north", "ASCENDING_SOUTH ascending_south", "SOUTH_EAST south_east",
                "SOUTH_WEST south_west", "NORTH_WEST north_west", "NORTH_EAST north_east");
    }

    /** Declaration order of a vanilla enum's constants (legacy dotted name), or null when unknown. */
    public static List<String> enumOrder(String legacyClass) {
        return ORDER.get(legacyClass);
    }

    private static final Map<String, String> PLANK_COLORS = Map.of("OAK", "WOOD", "SPRUCE", "OBSIDIAN", "BIRCH", "SAND", "JUNGLE", "DIRT",
            "ACACIA", "ADOBE", "DARK_OAK", "BROWN");

    public static void register() {
        ENUMS.forEach((legacyClass, constants) -> {
            Synth.InstanceMethod name = (self, args) -> {
                Constant c = constant(constants, self);
                return c == null ? Synth.NONE : c.name();
            };
            Synth.InstanceMethod unlocalized = (self, args) -> {
                Constant c = constant(constants, self);
                return c == null ? Synth.NONE : c.unlocalized();
            };
            Synth.InstanceMethod meta = (self, args) -> {
                Constant c = constant(constants, self);
                return c == null ? Synth.NONE : c.meta();
            };
            Synth.addInstanceMethod(legacyClass + ".getName", name);
            Synth.addInstanceMethod(legacyClass + ".toString", name);
            Synth.addInstanceMethod(legacyClass + ".getUnlocalizedName", unlocalized);
            Synth.addInstanceMethod(legacyClass + ".getTranslationKey", unlocalized);
            Synth.addInstanceMethod(legacyClass + ".getMetadata", meta);
            Synth.addInstanceMethod(legacyClass + ".getMeta", meta);
            List<String> order = ORDER.get(legacyClass);
            Synth.StaticMethod byMeta = args -> {
                int m = args.length > 0 ? Synth.toInt(args[0]) : 0;
                Class<?> type = callerSynthClass(legacyClass);
                if (type == null) return Synth.NONE;
                return Vanilla1122.enumConstant(type, order.get(m < 0 || m >= order.size() ? 0 : m));
            };
            Synth.addStaticMethod(legacyClass + ".byMetadata", byMeta);
        });
        Synth.addInstanceMethod("net.minecraft.block.BlockPlanks$EnumType.getMapColor", (self, args) -> {
            String color = self instanceof Enum<?> e ? PLANK_COLORS.get(e.name()) : null;
            if (color == null) return Synth.NONE;
            return legacyconnector.v1_12_2.net.minecraft.block.material.MapColor.class.getField(color).get(null);
        });

        // Forge's model math (TRSRTransformation): rotations about x, then y, then z
        Synth.addStaticMethod("net.minecraftforge.common.model.TRSRTransformation.quatFromXYZDegrees", args -> {
            if (!(args.length == 1 && args[0] instanceof javax.vecmath.Vector3f v)) return Synth.NONE;
            return quatFromXYZ((float) Math.toRadians(v.x), (float) Math.toRadians(v.y), (float) Math.toRadians(v.z));
        });
        Synth.addStaticMethod("net.minecraftforge.common.model.TRSRTransformation.quatFromXYZ", args -> {
            if (args.length == 1 && args[0] instanceof javax.vecmath.Vector3f v) return quatFromXYZ(v.x, v.y, v.z);
            if (args.length == 3) return quatFromXYZ(Synth.toFloat(args[0]), Synth.toFloat(args[1]), Synth.toFloat(args[2]));
            return Synth.NONE;
        });
    }

    private static javax.vecmath.Quat4f quatFromXYZ(float x, float y, float z) {
        javax.vecmath.Quat4f ret = new javax.vecmath.Quat4f(0, 0, 0, 1), t = new javax.vecmath.Quat4f();
        t.set((float) Math.sin(x / 2), 0, 0, (float) Math.cos(x / 2));
        ret.mul(t);
        t.set(0, (float) Math.sin(y / 2), 0, (float) Math.cos(y / 2));
        ret.mul(t);
        t.set(0, 0, (float) Math.sin(z / 2), (float) Math.cos(z / 2));
        ret.mul(t);
        return ret;
    }

    private static Constant constant(Map<String, Constant> constants, Object self) {
        return self instanceof Enum<?> e ? constants.get(e.name()) : null;
    }

    /** The synthesized 1.12.2 class whose static stub is running (it is on the stack, below {@code Synth.callStatic}). */
    private static Class<?> callerSynthClass(String legacyClass) {
        String name = "legacyconnector_synth.v1_12_2." + legacyClass;
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(s -> s.map(StackWalker.StackFrame::getDeclaringClass).filter(c -> c.getName().equals(name)).findFirst().orElse(null));
    }
}
