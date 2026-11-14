package org.kylepitcock.legacyconnector.vb1_7_3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.item.BlockItem;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.core.synth.Synth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The vanilla blocks and items of Beta 1.7.3 as legacy views of the modern ones. Their ids come from the game's own static initialisers
 * ({@code vanilla-ids.tsv}, see {@code tools/extract_beta_vanilla_ids.py}); {@code Block.BLOCKS} / {@code Item.ITEMS} hold a view per id
 * before any mod runs, so mods find their slots taken as in the original. Fields such as {@code Block.STONE} are not declared by the shim
 * (their types are mostly synthesized vanilla subclasses): {@link #resolveStatic} serves them, reusing the view of that id when its class
 * fits. Metadata variants (wool colours, dyes, logs, slabs) map to the flattened modern items.
 */
public final class VanillaB173 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static final String SHIM = "legacyconnector.vb1_7_3.";
    private static final String SYNTH = "legacyconnector_synth.vb1_7_3.";

    /** field -> {kind, id, legacy type} */
    private record Row(boolean block, String field, int id, String type) {}

    private static final Map<String, Row> BLOCK_FIELDS = new HashMap<>();
    private static final Map<String, Row> ITEM_FIELDS = new HashMap<>();
    private static final Map<Integer, String> MODERN_BY_ID = new HashMap<>();
    private static boolean initialized;

    /** b1.7.3 field name -> modern id path, where the names differ. */
    private static final Map<String, String> BLOCK_NAMES = Map.ofEntries(
            Map.entry("PLANKS", "oak_planks"), Map.entry("SAPLING", "oak_sapling"), Map.entry("FLOWING_WATER", "water"),
            Map.entry("FLOWING_LAVA", "lava"), Map.entry("LOG", "oak_log"), Map.entry("LEAVES", "oak_leaves"), Map.entry("BED", "red_bed"),
            Map.entry("GRASS", "short_grass"), Map.entry("WOOL", "white_wool"), Map.entry("ROSE", "poppy"),
            Map.entry("DOUBLE_SLAB", "smooth_stone"), Map.entry("SLAB", "smooth_stone_slab"), Map.entry("WOODEN_STAIRS", "oak_stairs"),
            Map.entry("LIT_FURNACE", "furnace"), Map.entry("SIGN", "oak_sign"), Map.entry("DOOR", "oak_door"), Map.entry("WALL_SIGN", "oak_wall_sign"),
            Map.entry("WOODEN_PRESSURE_PLATE", "oak_pressure_plate"), Map.entry("LIT_REDSTONE_ORE", "redstone_ore"),
            Map.entry("LIT_REDSTONE_TORCH", "redstone_torch"), Map.entry("BUTTON", "stone_button"), Map.entry("FENCE", "oak_fence"),
            Map.entry("PUMPKIN", "carved_pumpkin"), Map.entry("POWERED_REPEATER", "repeater"), Map.entry("LOCKED_CHEST", "chest"),
            Map.entry("TRAPDOOR", "oak_trapdoor"));
    private static final Map<String, String> ITEM_NAMES = Map.ofEntries(
            Map.entry("SEEDS", "wheat_seeds"), Map.entry("CHAIN_HELMET", "chainmail_helmet"), Map.entry("CHAIN_CHESTPLATE", "chainmail_chestplate"),
            Map.entry("CHAIN_LEGGINGS", "chainmail_leggings"), Map.entry("CHAIN_BOOTS", "chainmail_boots"), Map.entry("RAW_PORKCHOP", "porkchop"),
            Map.entry("SIGN", "oak_sign"), Map.entry("WOODEN_DOOR", "oak_door"), Map.entry("BOAT", "oak_boat"), Map.entry("CLAY", "clay_ball"),
            Map.entry("RAW_FISH", "cod"), Map.entry("COOKED_FISH", "cooked_cod"), Map.entry("DYE", "ink_sac"), Map.entry("BED", "red_bed"),
            Map.entry("MAP", "filled_map"), Map.entry("SLIMEBALL", "slime_ball"),
            Map.entry("RECORD_THIRTEEN", "music_disc_13"), Map.entry("RECORD_CAT", "music_disc_cat"));

    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
            "purple", "blue", "brown", "green", "red", "black"};
    private static final String[] DYES = {"ink_sac", "red_dye", "green_dye", "cocoa_beans", "lapis_lazuli", "purple_dye", "cyan_dye",
            "light_gray_dye", "gray_dye", "pink_dye", "lime_dye", "yellow_dye", "light_blue_dye", "magenta_dye", "orange_dye", "bone_meal"};
    private static final String[] WOODS = {"oak", "spruce", "birch"};
    private static final String[] SLABS = {"smooth_stone_slab", "sandstone_slab", "oak_slab", "cobblestone_slab"};

    private VanillaB173() {}

    private static final class CubeFlags {
        static final Map<Integer,Boolean> VALUES=metadata("vanilla-block-full-cubes.tsv",Boolean::parseBoolean);
    }
    private static final class TextureIndices {
        static final Map<Integer,Integer> VALUES=metadata("vanilla-block-texture-indices.tsv",Integer::parseInt);
    }
    private static <T> Map<Integer,T> metadata(String name,java.util.function.Function<String,T> parse){
        var values=new HashMap<Integer,T>();
        try(var in=VanillaB173.class.getResourceAsStream("/legacyconnector/b1.7.3/"+name)){
            if(in==null)throw new IllegalStateException(name+" missing from the runtime");
            var reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
            for(String line;(line=reader.readLine())!=null;){
                if(line.isBlank()||line.startsWith("#"))continue;
                var fields=line.split("\t");values.put(Integer.parseInt(fields[0]),parse.apply(fields[1]));
            }
        }catch(IOException error){throw new IllegalStateException("cannot read original Beta metadata: "+name,error);}
        return Map.copyOf(values);
    }
    /** Original per-id API results, including full-render blocks with partial collision such as soul sand. */
    public static Boolean fullCube(int id){return CubeFlags.VALUES.get(id);}
    /** Original default terrain-sheet index; vanilla views must not all inherit grass tile zero. */
    public static int textureIndex(int id){return TextureIndices.VALUES.getOrDefault(id,0);}

    /** Fills {@code Block.BLOCKS} and {@code Item.ITEMS} with views of the vanilla content. Once, before the first mod runs. */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        load();
        int blocks = 0, items = 0;
        for (Row row : BLOCK_FIELDS.values()) {
            if (Block.BLOCKS[row.id()] != null) continue;
            Block view = blockView(row, Block.class);
            if (view == null) continue;
            Block.BLOCKS[row.id()] = view;
            blocks++;
            var modern = view.mcc$modern;
            if (modern != null) {
                boolean opaque = modern.defaultBlockState().canOcclude();
                Block.BLOCKS_OPAQUE[row.id()] = opaque;
                Block.BLOCKS_LIGHT_OPACITY[row.id()] = opaque ? 255 : 0;
                Block.BLOCKS_ALLOW_VISION[row.id()] = !opaque;
                Block.BLOCKS_LIGHT_LUMINANCE[row.id()] = modern.defaultBlockState().getLightEmission();
                Block.BLOCKS_RANDOM_TICK[row.id()] = modern.defaultBlockState().isRandomlyTicking();
            }
            // the b1.7.3 Block initialiser gave every block an item of its id
            if (Item.ITEMS[row.id()] == null) {
                BlockItem item = (BlockItem) Synth.blank(BlockItem.class.getName());
                if (item != null) {
                    item.mcc$initVanilla(row.id(), 64, 0, view.getTranslationKey());
                    item.mcc$initVanillaBlock(row.id());
                    item.mcc$modern = view.mcc$modern == null ? null : view.mcc$modern.asItem();
                    Item.ITEMS[row.id()] = item;
                }
            }
        }
        for (Row row : ITEM_FIELDS.values()) {
            if (Item.ITEMS[row.id()] != null) continue;
            Item view = itemView(row, Item.class);
            if (view == null) continue;
            Item.ITEMS[row.id()] = view;
            items++;
        }
        LOG.info("b1.7.3 vanilla content: {} blocks and {} items as views of the modern ones", blocks, items);
    }

    private static void load() {
        try (InputStream in = VanillaB173.class.getResourceAsStream("/legacyconnector/b1.7.3/vanilla-ids.tsv")) {
            if (in == null) throw new IllegalStateException("vanilla-ids.tsv missing from the runtime");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String l = r.readLine(); l != null; l = r.readLine()) {
                if (l.isEmpty() || l.charAt(0) == '#') continue;
                String[] p = l.split("\t");
                Row row = new Row(p[0].equals("block"), p[1], Integer.parseInt(p[2]), p[3].replace('/', '.'));
                (row.block() ? BLOCK_FIELDS : ITEM_FIELDS).put(row.field(), row);
                String modern = row.block() ? BLOCK_NAMES.getOrDefault(row.field(), row.field().toLowerCase(Locale.ROOT))
                        : ITEM_NAMES.getOrDefault(row.field(), row.field().toLowerCase(Locale.ROOT));
                // an item field wins for the shared ids of block items only where no block claimed the id
                MODERN_BY_ID.putIfAbsent(row.id(), modern);
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read vanilla-ids.tsv", e);
        }
    }

    /** A view of a vanilla block, of the legacy class the field names (synthesized when a mod uses it) or plain {@code Block}. */
    private static Block blockView(Row row, Class<?> minimum) {
        Object o = instantiate(row.type(), minimum);
        if (!(o instanceof Block b)) return null;
        String key = "tile." + camel(row.field());
        var modern = modernBlock(row.id());
        b.mcc$initVanilla(row.id(), textureIndex(row.id()), materialOf(modern), 0.0F, null, key);
        b.mcc$modern = modern;
        if (modern != null) {
            b.hardness = modern.defaultDestroyTime();
            b.resistance = modern.getExplosionResistance() * 5.0F;
        }
        return b;
    }

    private static Item itemView(Row row, Class<?> minimum) {
        Item complete=toolView(row);
        if(complete==null)complete=armorView(row);
        Object o = complete!=null?complete:instantiate(row.type(), minimum);
        if (!(o instanceof Item i)) return null;
        if(!minimum.isInstance(i))return null;
        var modern = modernItem(row.id(), 0);
        i.mcc$initVanilla(row.id(), modern == null ? 64 : modern.getDefaultMaxStackSize(),
                complete!=null?complete.getMaxDamage():modern == null ? 0 : modern.getDefaultInstance().getMaxDamage(), "item." + camel(row.field()));
        i.mcc$modern = modern;
        // Original renderers draw a vanilla item from its gui/items.png sprite (Item.SNOWBALL.getTextureId(0)); without the index they get cell 0.
        if (modern != null && i.getTextureId(0) == 0) {
            int sprite = AssetsB173.vanillaItemSprite(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(modern).getPath());
            if (sprite > 0) i.setTextureId(sprite);
        }
        return i;
    }
    private static Item armorView(Row row){
        if(!row.type().equals("net.minecraft.item.ArmorItem"))return null;
        int separator=row.field().indexOf('_');
        int texture=switch(row.field().substring(0,separator)){
            case "LEATHER"->0;case "CHAIN"->1;case "IRON"->2;case "DIAMOND"->3;case "GOLDEN"->4;
            default->throw new IllegalStateException("Unknown vanilla armor material "+row.field());
        };
        int slot=switch(row.field().substring(separator+1)){
            case "HELMET"->0;case "CHESTPLATE"->1;case "LEGGINGS"->2;case "BOOTS"->3;
            default->throw new IllegalStateException("Unknown vanilla armor slot "+row.field());
        };
        return legacyconnector.vb1_7_3.net.minecraft.item.ArmorItem.mcc$vanilla(row.id(),texture==4?1:texture,texture,slot);
    }
    private static Item toolView(Row row){
        if(!row.type().equals("net.minecraft.item.PickaxeItem")&&!row.type().equals("net.minecraft.item.AxeItem")&&!row.type().equals("net.minecraft.item.ShovelItem"))return null;
        var material=switch(row.field().substring(0,row.field().indexOf('_'))){
            case "WOODEN"->legacyconnector.vb1_7_3.net.minecraft.item.ToolMaterial.WOOD;
            case "STONE"->legacyconnector.vb1_7_3.net.minecraft.item.ToolMaterial.STONE;
            case "IRON"->legacyconnector.vb1_7_3.net.minecraft.item.ToolMaterial.IRON;
            case "DIAMOND"->legacyconnector.vb1_7_3.net.minecraft.item.ToolMaterial.DIAMOND;
            case "GOLDEN"->legacyconnector.vb1_7_3.net.minecraft.item.ToolMaterial.GOLD;
            default->throw new IllegalStateException("Unknown vanilla tool material "+row.field());
        };
        return switch(row.type()){
            case "net.minecraft.item.PickaxeItem"->legacyconnector.vb1_7_3.net.minecraft.item.PickaxeItem.mcc$vanilla(row.id(),material);
            case "net.minecraft.item.AxeItem"->legacyconnector.vb1_7_3.net.minecraft.item.AxeItem.mcc$vanilla(row.id(),material);
            default->legacyconnector.vb1_7_3.net.minecraft.item.ShovelItem.mcc$vanilla(row.id(),material);
        };
    }

    /** An instance of the legacy class (the synthesized one when it exists), else of {@code minimum}, without running a constructor. */
    private static Object instantiate(String legacyType, Class<?> minimum) {
        String rel = legacyType;
        Object o = Synth.blank(SYNTH + rel);
        if (o == null) o = Synth.blank(SHIM + rel);
        if (o == null || !minimum.isInstance(o)) o = Synth.blank(minimum.getName());
        return o;
    }

    /**
     * {@code Block.STONE}, {@code Item.IRON_INGOT}, ...: the view of that id when its class is the field's type, else a view of the
     * field's type (synthesized vanilla subclass) that replaces it in the id table.
     */
    public static Object resolveStatic(String key, Class<?> accessor) {
        if (!accessor.getName().startsWith(SYNTH)) return null;
        String owner=key.substring(0,key.lastIndexOf('.'));
        boolean blocks = owner.equals("net.minecraft.block.Block");
        boolean items = owner.equals("net.minecraft.item.Item");
        if(!blocks&&!items){
            Class<?> inheritedOwner=null;
            for(String prefix:new String[]{SHIM,SYNTH})try{inheritedOwner=Class.forName(prefix+owner,false,accessor.getClassLoader());break;}catch(ClassNotFoundException ignored){}
            if(inheritedOwner==null)return null;
            blocks=Block.class.isAssignableFrom(inheritedOwner);items=Item.class.isAssignableFrom(inheritedOwner);
        }
        if (!blocks && !items) return null;
        init();
        String field = key.substring(key.lastIndexOf('.') + 1);
        Row row = (blocks ? BLOCK_FIELDS : ITEM_FIELDS).get(field);
        if (row == null) return null;
        Class<?> type = null;
        for (var m : accessor.getDeclaredMethods()) if (m.getName().equals("sget$" + field)) type = m.getReturnType();
        if (type == null) return null;
        synchronized (VanillaB173.class) {
            if (blocks) {
                Block existing = Block.BLOCKS[row.id()];
                if (type.isInstance(existing)) return existing;
                Block view = blockView(row, type);
                if (view == null || !type.isInstance(view)) return null;
                if (existing == null || existing.mcc$owner == null) Block.BLOCKS[row.id()] = view;
                return view;
            }
            Item existing = Item.ITEMS[row.id()];
            if (type.isInstance(existing)) return existing;
            Item view = itemView(row, type);
            if (view == null || !type.isInstance(view)) return null;
            if (existing == null || existing.mcc$owner == null) Item.ITEMS[row.id()] = view;
            return view;
        }
    }

    /**
     * The b1.7.3 material of a modern block, judged from what it is (fluid, plant, leaves, ...) and how it sounds. Mods ask the materials
     * of their neighbours ({@code world.getBlockMaterial(x, y, z).isSolid()}), so vanilla blocks need one.
     */
    public static Material materialOf(net.minecraft.world.level.block.Block block) {
        if (block == null) return Material.STONE;
        var state = block.defaultBlockState();
        if (state.isAir()) return Material.AIR;
        var fluid = state.getFluidState();
        // Tags are not bound while the vanilla views are initialized during mod loading.
        if (!fluid.isEmpty()) return fluid.getType() == net.minecraft.world.level.material.Fluids.LAVA
                || fluid.getType() == net.minecraft.world.level.material.Fluids.FLOWING_LAVA ? Material.LAVA : Material.WATER;
        if (block instanceof net.minecraft.world.level.block.BaseFireBlock) return Material.FIRE;
        if (block instanceof net.minecraft.world.level.block.NetherPortalBlock) return Material.NETHER_PORTAL;
        if (block instanceof net.minecraft.world.level.block.LeavesBlock) return Material.LEAVES;
        if (block instanceof net.minecraft.world.level.block.CactusBlock) return Material.CACTUS;
        if (block instanceof net.minecraft.world.level.block.TntBlock) return Material.TNT;
        if (block instanceof net.minecraft.world.level.block.CakeBlock) return Material.CAKE;
        if (block instanceof net.minecraft.world.level.block.WebBlock) return Material.COBWEB;
        if (block instanceof net.minecraft.world.level.block.IceBlock) return Material.ICE;
        if (block instanceof net.minecraft.world.level.block.SnowLayerBlock) return Material.SNOW_LAYER;
        if (block instanceof net.minecraft.world.level.block.BushBlock || block instanceof net.minecraft.world.level.block.SugarCaneBlock) return Material.PLANT;
        if (block instanceof net.minecraft.world.level.block.TorchBlock || block instanceof net.minecraft.world.level.block.RedStoneWireBlock
                || block instanceof net.minecraft.world.level.block.LeverBlock || block instanceof net.minecraft.world.level.block.ButtonBlock
                || block instanceof net.minecraft.world.level.block.BaseRailBlock || block instanceof net.minecraft.world.level.block.RepeaterBlock
                || block instanceof net.minecraft.world.level.block.LadderBlock) {
            return Material.PISTON_BREAKABLE;
        }
        if (block instanceof net.minecraft.world.level.block.TransparentBlock || block instanceof net.minecraft.world.level.block.StainedGlassBlock) return Material.GLASS;
        if (block instanceof net.minecraft.world.level.block.piston.PistonBaseBlock) return Material.PISTON;
        var sound = state.getSoundType();
        if (sound == net.minecraft.world.level.block.SoundType.WOOD) return Material.WOOD;
        if (sound == net.minecraft.world.level.block.SoundType.WOOL) return Material.WOOL;
        if (sound == net.minecraft.world.level.block.SoundType.SAND) return Material.SAND;
        if (sound == net.minecraft.world.level.block.SoundType.GRASS) return Material.SOLID_ORGANIC;
        if (sound == net.minecraft.world.level.block.SoundType.GRAVEL) return block == net.minecraft.world.level.block.Blocks.CLAY ? Material.CLAY : Material.SOIL;
        if (sound == net.minecraft.world.level.block.SoundType.SNOW) return Material.SNOW_BLOCK;
        if (sound == net.minecraft.world.level.block.SoundType.METAL) return Material.METAL;
        if (sound == net.minecraft.world.level.block.SoundType.GLASS) return Material.GLASS;
        if (block == net.minecraft.world.level.block.Blocks.SPONGE) return Material.SPONGE;
        return Material.STONE;
    }

    public static boolean isVanillaId(int id) {
        return MODERN_BY_ID.containsKey(id);
    }

    /** The modern block of a vanilla block id (its default state's block for metadata variants). */
    public static net.minecraft.world.level.block.Block modernBlock(int id) {
        String path = MODERN_BY_ID.get(id);
        if (path == null) return null;
        ResourceLocation rl = ResourceLocation.withDefaultNamespace(path);
        return BuiltInRegistries.BLOCK.containsKey(rl) ? BuiltInRegistries.BLOCK.get(rl) : null;
    }

    /** The modern item of a vanilla item or block id with its damage/metadata (wool and dye colours, log woods, slabs, charcoal). */
    public static net.minecraft.world.item.Item modernItem(int id, int damage) {
        String path = switch (id) {
            case 35 -> COLORS[damage & 15] + "_wool";
            case 351 -> DYES[damage & 15];
            case 17 -> WOODS[Math.floorMod(damage, 3)] + "_log";
            case 18 -> WOODS[Math.floorMod(damage, 3)] + "_leaves";
            case 6 -> WOODS[Math.floorMod(damage, 3)] + "_sapling";
            case 44 -> SLABS[damage & 3];
            case 43 -> damage == 0 ? "smooth_stone" : SLABS[damage & 3];
            case 31 -> damage == 2 ? "fern" : damage == 0 ? "dead_bush" : "short_grass";
            case 263 -> damage == 1 ? "charcoal" : "coal";
            default -> MODERN_BY_ID.get(id);
        };
        if (path == null) return null;
        ResourceLocation rl = ResourceLocation.withDefaultNamespace(path);
        if (!BuiltInRegistries.ITEM.containsKey(rl)) return null;
        var item = BuiltInRegistries.ITEM.get(rl);
        return item == net.minecraft.world.item.Items.AIR ? null : item;
    }

    private static String camel(String field) {
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : field.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == '_') {
                up = true;
            } else {
                sb.append(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        return sb.toString();
    }
}
