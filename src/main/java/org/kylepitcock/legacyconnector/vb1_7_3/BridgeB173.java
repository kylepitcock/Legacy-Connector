package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.item.BlockItem;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import org.kylepitcock.legacyconnector.core.LegacyLifecycle;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.pack.GeneratedTranslations;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns the blocks and items the b1.7.3 mods constructed into modern registry entries, once every mod has run (registries are open then:
 * {@code LegacyLifecycle}). ModLoader content has numeric ids and no names: the registry name comes from the translation key
 * ({@code setBlockName("oreTitanium")} is {@code mod:ore_titanium}), else the id ({@code mod:block_201}); the namespace is the mod's.
 * The display name ({@code ModLoader.AddName}) becomes a generated translation.
 */
public final class BridgeB173 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static final Map<String, Set<String>> USED = new LinkedHashMap<>();
    private static final Map<String, int[]> PER_MOD = new LinkedHashMap<>();

    private BridgeB173() {}

    public static synchronized void registerAll() {
        int replaced = 0;
        for (Block block : ContentB173.blocks()) {
            if (block.mcc$modern != null || !(block.mcc$owner instanceof LegacyModRecord mod) || Block.BLOCKS[block.id] != block) continue;
            if (LegacyLifecycle.isFailed(mod.modernId)) continue;
            if (VanillaB173.isVanillaId(block.id) && VanillaB173.modernBlock(block.id) != null) {
                // a mod's replacement of a vanilla block (same id): the vanilla block it replaced stays the modern one
                block.mcc$modern = VanillaB173.modernBlock(block.id);
                replaced++;
                continue;
            }
            try {
                register(block, mod);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Could not register b1.7.3 block {} of {}: {}", block, mod.legacyId, e.toString());
            }
        }
        for (Item item : ContentB173.items()) {
            if (item.mcc$modern != null || !(item.mcc$owner instanceof LegacyModRecord mod) || Item.ITEMS[item.id] != item) continue;
            if (LegacyLifecycle.isFailed(mod.modernId)) continue;
            if (VanillaB173.isVanillaId(item.id) && VanillaB173.modernItem(item.id, 0) != null) {
                // mods re-create vanilla items at their ids to change them (NetherCraft's tools): they stay the vanilla items
                item.mcc$modern = VanillaB173.modernItem(item.id, 0);
                replaced++;
                continue;
            }
            try {
                register(item, mod);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Could not register b1.7.3 item {} of {}: {}", item, mod.legacyId, e.toString());
            }
        }
        if (Boolean.getBoolean("legacyconnector.entitySelfTest")) ItemUseSelfTestB173.register();
        TileEntitiesB173.registerType();
        MobsB173.registerAll();
        AnimalsB173.registerAll();
        flammability();
        StringBuilder sb = new StringBuilder();
        PER_MOD.forEach((mod, n) -> sb.append(' ').append(mod).append('=').append(n[0]).append(" blocks/").append(n[1]).append(" items"));
        LOG.info("b1.7.3 content registered:{}{}", sb.length() == 0 ? " none" : sb,
                replaced > 0 ? "; " + replaced + " replacement(s) of vanilla blocks/items kept as the vanilla ones" : "");
    }

    /**
     * {@code BlockFire}'s tables (mods set them with ModLoader's reflection or Forge): how much a block encourages fire and how easily it
     * burns, the same two numbers as modern fire's {@code setFlammable(block, encouragement, flammability)}.
     */
    private static void flammability() {
        Block fire = Block.BLOCKS[51];
        if (!(fire instanceof legacyconnector.vb1_7_3.net.minecraft.block.FireBlock f) || f.burnChances == null) return;
        var modernFire = (net.minecraft.world.level.block.FireBlock) net.minecraft.world.level.block.Blocks.FIRE;
        int n = 0;
        for (int id = 0; id < 256; id++) {
            Block b = Block.BLOCKS[id];
            if (b == null || b.mcc$owner == null || b.mcc$modern == null || isVanillaReplacement(b.mcc$modern)) continue;
            int encourage = f.burnChances[id], burn = f.spreadChances[id];
            if (encourage <= 0 && burn <= 0) continue;
            modernFire.setFlammable(b.mcc$modern, encourage, burn);
            n++;
        }
        if (n > 0) LOG.info("b1.7.3: {} mod block(s) burn as their fire tables said", n);
    }

    private static void register(Block block, LegacyModRecord mod) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(mod.modernId, unique(mod.modernId, name(block.getTranslationKey(), "tile.", "block_" + block.id)));
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of();
        p.sound(block.soundGroup != null ? block.soundGroup.mcc$modern() : net.minecraft.world.level.block.SoundType.STONE);
        if (block.hardness < 0) p.strength(-1.0F, 3600000.0F);
        // b1.7.3 kept three times the setResistance value and divided by five for explosions, like 1.12
        else p.strength(block.hardness, block.resistance / 5.0F);
        int light = Block.BLOCKS_LIGHT_LUMINANCE[block.id];
        if (light > 0) p.lightLevel(s -> light);
        if (block.slipperiness != 0.6F) p.friction(block.slipperiness);
        Material m = block.material;
        boolean opaque = true;
        try {
            opaque = block.isOpaque();
        } catch (RuntimeException | LinkageError e) {
            // keeps the default
        }
        if (!opaque) p.noOcclusion();
        if (m != null) {
            if (!m.blocksMovement()) p.noCollission();
            if (m.isBurnable()) p.ignitedByLava();
            if (m.isReplaceable()) p.replaceable();
            if (!m.isHandHarvestable()) p.requiresCorrectToolForDrops();
            p.pushReaction(switch (m.getPistonBehavior()) {
                case 1 -> PushReaction.DESTROY;
                case 2 -> PushReaction.BLOCK;
                default -> PushReaction.NORMAL;
            });
        }
        if (Block.BLOCKS_RANDOM_TICK[block.id]) p.randomTicks();
        LegacyB173Block modern;
        if (Block.BLOCKS_WITH_ENTITY[block.id] || block instanceof legacyconnector.vb1_7_3.net.minecraft.block.BlockWithEntity) {
            var eb = new LegacyB173EntityBlock(block, p);
            TileEntitiesB173.addBlock(eb);
            modern = eb;
        } else {
            modern = new LegacyB173Block(block, p);
        }
        LegacyRegistrar.register(BuiltInRegistries.BLOCK, id, modern);
        block.mcc$modern = modern;
        translation("block." + id.getNamespace() + "." + id.getPath(), block, block.getTranslationKey());
        PER_MOD.computeIfAbsent(mod.legacyId, k -> new int[2])[0]++;
    }

    private static void register(Item item, LegacyModRecord mod) {
        net.minecraft.world.item.Item modern;
        ResourceLocation id;
        if (item instanceof BlockItem bi) {
            Block block = bi.mcc$blockId() >= 0 && bi.mcc$blockId() < 256 ? Block.BLOCKS[bi.mcc$blockId()] : null;
            if (block == null || block.mcc$modern == null) return;
            id = BuiltInRegistries.BLOCK.getKey(block.mcc$modern);
            LegacyB173BlockItem blockItem = new LegacyB173BlockItem(block.mcc$modern, new net.minecraft.world.item.Item.Properties());
            if (item.hasSubtypes()) blockItem.setNames(subtypeNames(item, id));
            modern = blockItem;
        } else {
            id = ResourceLocation.fromNamespaceAndPath(mod.modernId, unique(mod.modernId, name(item.getTranslationKey(), "item.", "item_" + item.id)));
            var p = new net.minecraft.world.item.Item.Properties();
            if (item.isDamageable()) p.durability(item.getMaxDamage());
            else p.stacksTo(Math.max(1, Math.min(99, item.getMaxCount())));
            modern = new LegacyB173Item(item, p);
            translation("item." + id.getNamespace() + "." + id.getPath(), item, item.getTranslationKey());
        }
        LegacyRegistrar.register(BuiltInRegistries.ITEM, id, modern);
        item.mcc$modern = modern;
        PER_MOD.computeIfAbsent(mod.legacyId, k -> new int[2])[1]++;
    }

    /**
     * Names of the subtypes of a block item ({@code getItemNameIS(stack)} per damage, localized as {@code ModLoader.AddName} did): translation
     * keys per metadata, null where the name is the block's own (or the legacy item throws for that damage).
     */
    private static String[] subtypeNames(Item item, ResourceLocation id) {
        String[] keys = new String[16];
        String base = null;
        for (int meta = 0; meta < 16; meta++) {
            String legacyKey;
            try {
                legacyKey = item.getTranslationKey(new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(item.id, 1, meta));
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            if (legacyKey == null) continue;
            String name = ContentB173.translate(legacyKey + ".name");
            if (name.equals(legacyKey + ".name")) continue;
            if (meta == 0) base = name;
            if (meta > 0 && name.equals(base)) continue;
            String key = "block." + id.getNamespace() + "." + id.getPath() + ".m" + meta;
            GeneratedTranslations.put("en_us", key, name);
            keys[meta] = key;
        }
        return keys;
    }

    /** The display name: ModLoader.AddName for the object, else a localization of its key, else the key made readable. */
    private static void translation(String modernKey, Object legacy, String legacyKey) {
        String name = ContentB173.name(legacy);
        if (name == null && legacyKey != null) {
            String t = ContentB173.translate(legacyKey + ".name");
            if (!t.equals(legacyKey + ".name")) name = t;
        }
        if (name == null && legacyKey != null) name = readable(legacyKey.substring(legacyKey.indexOf('.') + 1));
        if (name != null) GeneratedTranslations.put("en_us", modernKey, name);
    }

    /** True if the legacy content maps onto Minecraft's own block or item (a mod's replacement of vanilla content). */
    public static boolean isVanillaReplacement(Object modern) {
        ResourceLocation key = modern instanceof net.minecraft.world.level.block.Block b ? BuiltInRegistries.BLOCK.getKey(b)
                : modern instanceof net.minecraft.world.item.Item i ? BuiltInRegistries.ITEM.getKey(i) : null;
        return key != null && key.getNamespace().equals("minecraft");
    }

    /** {@code tile.oreTitanium} -> {@code ore_titanium}; no key -> the fallback. */
    static String name(String key, String prefix, String fallback) {
        if (key == null || key.length() <= prefix.length()) return fallback;
        String raw = key.startsWith(prefix) ? key.substring(prefix.length()) : key;
        StringBuilder sb = new StringBuilder();
        char prev = 0;
        for (char c : raw.toCharArray()) {
            if (Character.isUpperCase(c) && sb.length() > 0 && (Character.isLowerCase(prev) || Character.isDigit(prev))) sb.append('_');
            char l = Character.toLowerCase(c);
            sb.append((l >= 'a' && l <= 'z') || (l >= '0' && l <= '9') || l == '_' || l == '.' || l == '-' ? (l == '.' ? '_' : l) : '_');
            prev = c;
        }
        String s = sb.toString().replaceAll("_+", "_").replaceAll("^_|_$", "");
        return s.isEmpty() ? fallback : s;
    }

    private static String unique(String namespace, String name) {
        Set<String> used = USED.computeIfAbsent(namespace, k -> new HashSet<>());
        String n = name;
        for (int i = 2; !used.add(n) || BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(namespace, n))
                || BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath(namespace, n)); i++) {
            n = name + "_" + i;
        }
        return n;
    }

    private static String readable(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (i == 0) sb.append(Character.toUpperCase(c));
            else if (Character.isUpperCase(c) && Character.isLowerCase(camel.charAt(i - 1))) sb.append(' ').append(c);
            else sb.append(c);
        }
        return sb.toString();
    }
}
