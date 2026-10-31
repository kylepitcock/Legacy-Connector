package org.kylepitcock.legacyconnector.devcheck;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipFile;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Offline check of the modern -> legacy item/block identity round trip that Thaumcraft 4 (1.7.10) relies on (issue #109):
 * reads the vanilla Block/Item + metadata keys ConfigAspects registers aspects for (and its ore dictionary names), then asks
 * whether a player holding the modern item, or scanning the placed modern block, reaches the same legacy key. Also checks that
 * every modern vanilla item and block has a legacy numeric id that resolves back (Item.getItemById(Item.getIdFromItem(x)) == x).
 *
 * <p>Usage: {@code ./gradlew checkVanillaAspects -PlegacyJar=Thaumcraft-1.7.10-4.2.3.5.jar}. Runs against vanilla registries
 * only; no game is launched.</p>
 */
public final class CheckVanillaAspects {
    private record Reg(String owner, String field, int meta, boolean anyMeta) {}

    private record Key(Item item, int meta) {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("usage: <fields.csv> <thaumcraft jar>");
        Map<String, String> srg = readFields(Path.of(args[0]));
        List<Reg> regs = new ArrayList<>();
        Set<String> oreNames = new LinkedHashSet<>();
        try (ZipFile zip = new ZipFile(args[1])) {
            var entry = zip.getEntry("thaumcraft/common/config/ConfigAspects.class");
            if (entry == null) throw new IllegalArgumentException("no thaumcraft/common/config/ConfigAspects.class in " + args[1]);
            try (InputStream in = zip.getInputStream(entry)) {
                scan(in.readAllBytes(), regs, oreNames);
            }
        }
        DevBootstrap.boot();

        // Thaumcraft's objectTags keys: Arrays.asList(stack.getItem(), stack.getItemDamage())
        Set<Key> keys = new HashSet<>();
        List<String> unresolved = new ArrayList<>();
        Map<Reg, List<Integer>> metasOf = new HashMap<>();
        for (Reg r : regs) {
            Object obj = legacyObject(r, srg);
            if (obj == null) {
                unresolved.add(r.owner() + "." + r.field() + " (" + srg.get(r.field()) + ")");
                continue;
            }
            List<Integer> metas = new ArrayList<>();
            if (r.anyMeta()) for (int m = 0; m < 16; m++) metas.add(m);
            else metas.add(r.meta());
            metasOf.put(r, metas);
            for (int m : metas) {
                ItemStack s = stack(obj, m);
                if (s != null && s.getItem() != null) keys.add(new Key(s.getItem(), s.getItemDamage()));
            }
        }
        int oreKeys = 0;
        for (String ore : oreNames) {
            for (ItemStack s : OreDictionary.getOres(ore)) {
                if (s != null && s.getItem() != null && keys.add(new Key(s.getItem(), s.getItemDamage()))) oreKeys++;
            }
        }

        // Every distinct modern item/state a registration stands for.
        Map<String, Object[]> targets = new TreeMap<>();
        for (Reg r : regs) {
            Object obj = legacyObject(r, srg);
            if (obj == null) continue;
            List<Integer> metas = new ArrayList<>(metasOf.get(r));
            if (r.meta() == Short.MAX_VALUE) for (int m = 0; m < 16; m++) metas.add(m);
            for (int m : metas) {
                if (m == Short.MAX_VALUE) continue;
                ItemStack s = stack(obj, m);
                if (s == null || s.mcc$modern().isEmpty()) continue;
                var modern = s.mcc$modern().getItem();
                String id = BuiltInRegistries.ITEM.getKey(modern).toString();
                targets.putIfAbsent(id, new Object[] {modern, r.owner() + "." + srg.getOrDefault(r.field(), r.field()) + ":" + m});
            }
        }

        int held = 0, heldOk = 0, placed = 0, placedOldOk = 0, placedNewOk = 0;
        List<String> heldFail = new ArrayList<>(), placedOldFail = new ArrayList<>(), placedNewFail = new ArrayList<>();
        for (var e : targets.entrySet()) {
            var modern = (net.minecraft.world.item.Item) e.getValue()[0];
            String label = e.getKey() + " (" + e.getValue()[1] + ")";
            held++;
            ItemStack h = ItemStack.mcc$wrap(new net.minecraft.world.item.ItemStack(modern));
            if (h != null && has(keys, h.getItem(), h.getItemDamage())) heldOk++;
            else heldFail.add(label + " -> " + describe(h));
            if (modern instanceof BlockItem bi) {
                placed++;
                BlockState state = bi.getBlock().defaultBlockState();
                ItemStack before = pickBefore(state);
                ItemStack after = pickAfter(state);
                if (before != null && has(keys, before.getItem(), before.getItemDamage())) placedOldOk++;
                else placedOldFail.add(label + " -> " + describe(before));
                if (after != null && has(keys, after.getItem(), after.getItemDamage())) placedNewOk++;
                else placedNewFail.add(label + " -> " + describe(after));
            }
        }

        // Numeric ids: every modern vanilla item and block must have one that resolves back (the Thaumometer's scan packet).
        int items = 0, itemIdOk = 0, blocks = 0, blockIdOk = 0;
        List<String> idFail = new ArrayList<>();
        for (var modern : BuiltInRegistries.ITEM) {
            if (modern == net.minecraft.world.item.Items.AIR || !BuiltInRegistries.ITEM.getKey(modern).getNamespace().equals("minecraft")) continue;
            items++;
            ItemStack s = ItemStack.mcc$wrap(new net.minecraft.world.item.ItemStack(modern));
            Item legacy = s == null ? null : s.getItem();
            int id = Item.getIdFromItem(legacy);
            if (legacy != null && id > 0 && Item.getItemById(id) == legacy) itemIdOk++;
            else idFail.add("item " + BuiltInRegistries.ITEM.getKey(modern) + " -> " + legacy + " id " + id);
        }
        for (var modern : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(modern).getNamespace().equals("minecraft") || modern.asItem() == net.minecraft.world.item.Items.AIR) continue;
            blocks++;
            Block legacy = BlockBridge.legacyBlock(modern.defaultBlockState());
            int id = Block.getIdFromBlock(legacy);
            if (id >= 0 && Block.getBlockById(id) == legacy && Item.getItemFromBlock(legacy) != null) blockIdOk++;
            else idFail.add("block " + BuiltInRegistries.BLOCK.getKey(modern) + " -> " + legacy + " id " + id);
        }

        System.out.println("== Thaumcraft ConfigAspects vanilla registrations ==");
        System.out.println("registrations: " + regs.size() + " (unresolved fields: " + unresolved.size() + "), ore names: " + oreNames.size()
                + ", legacy keys: " + keys.size() + " (" + oreKeys + " from the ore dictionary)");
        for (String u : unresolved) System.out.println("  unresolved " + u);
        System.out.println("held modern item -> legacy key with aspects: " + heldOk + "/" + held);
        for (String f : heldFail) System.out.println("  HELD MISS " + f);
        System.out.println("placed block pick, old base getPickBlock: " + placedOldOk + "/" + placed);
        System.out.println("placed block pick, modern pick (new): " + placedNewOk + "/" + placed);
        for (String f : placedOldFail) System.out.println("  PICK MISS (old) " + f);
        for (String f : placedNewFail) System.out.println("  PICK MISS (new) " + f);
        System.out.println("== numeric ids ==");
        System.out.println("modern vanilla items with a legacy id that resolves back: " + itemIdOk + "/" + items);
        System.out.println("modern vanilla blocks with a legacy id that resolves back: " + blockIdOk + "/" + blocks);
        for (String f : idFail) System.out.println("  ID FAIL " + f);
    }

    private static String describe(ItemStack s) {
        if (s == null) return "null";
        return s.getItem() + ":" + s.getItemDamage();
    }

    /** Thaumcraft's ThaumcraftCraftingManager.getObjectTags lookup, without recipe-generated tags. */
    private static boolean has(Set<Key> keys, Item item, int meta) {
        if (item == null) return false;
        if (keys.contains(new Key(item, meta)) || keys.contains(new Key(item, Short.MAX_VALUE))) return true;
        if (meta == Short.MAX_VALUE) for (int m = 0; m < 16; m++) if (keys.contains(new Key(item, m))) return true;
        return false;
    }

    /** The old Block.getPickBlock: the block's own item with damageDropped of the world metadata. */
    private static ItemStack pickBefore(BlockState state) {
        Block lb = BlockBridge.legacyBlock(state);
        Item item = Item.getItemFromBlock(lb);
        return item == null ? null : new ItemStack(item, 1, lb.damageDropped(BlockBridge.legacyMeta(state)));
    }

    /** The new Block.getPickBlock (Forge 1.7.10: getItem, then the damage of the item's block), for a state with no world around. */
    private static ItemStack pickAfter(BlockState state) {
        Block lb = BlockBridge.legacyBlock(state);
        int meta = BlockBridge.legacyMeta(state);
        Item item;
        if (overrides(lb, "getItem")) {
            item = lb.getItem(null, 0, 0, 0);
        } else if (lb.mcc$vanilla != null) {
            ItemStack p = legacyPick(state);
            item = p == null ? null : p.getItem();
        } else {
            item = Item.getItemFromBlock(lb);
        }
        if (item == null) return null;
        Block db = item instanceof ItemBlock ? Block.getBlockFromItem(item) : lb;
        if (db == null) db = lb;
        int damage;
        if (overrides(db, "getDamageValue") || db.mcc$vanilla == null) {
            damage = db.damageDropped(meta);
        } else {
            ItemStack p = legacyPick(db == lb ? state : BlockBridge.toModern(db, meta));
            damage = p == null ? db.damageDropped(meta) : p.getItemDamage();
        }
        return new ItemStack(item, 1, damage);
    }

    /** BlockBridge.legacyPick without a world, looked up reflectively so this check also runs against older builds (it then reports nothing new). */
    private static ItemStack legacyPick(BlockState state) {
        try {
            var m = BlockBridge.class.getMethod("legacyPick", BlockState.class, net.minecraft.world.level.LevelReader.class, net.minecraft.core.BlockPos.class);
            return (ItemStack) m.invoke(null, state, null, null);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean overrides(Block b, String name) {
        for (Class<?> c = b.getClass(); c != Block.class && c != null; c = c.getSuperclass()) {
            for (var m : c.getDeclaredMethods()) if (m.getName().equals(name) && m.getParameterCount() == 4) return true;
        }
        return false;
    }

    private static ItemStack stack(Object obj, int meta) {
        if (obj instanceof Block b) return Item.getItemFromBlock(b) == null ? null : new ItemStack(b, 1, meta);
        return new ItemStack((Item) obj, 1, meta);
    }

    private static Object legacyObject(Reg r, Map<String, String> srg) {
        String name = srg.get(r.field());
        if (name == null) return null;
        try {
            Class<?> c = Class.forName("legacyconnector.v1_7_10.net.minecraft.init." + (r.owner().equals("Blocks") ? "Blocks" : "Items"));
            return c.getField(name).get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** Collects ItemStack(Blocks/Items.X[, count[, meta]]) constructions and registerObjectTag(String, ...) ore names. */
    private static void scan(byte[] bytes, List<Reg> regs, Set<String> oreNames) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);
        for (MethodNode mn : cn.methods) {
            FieldInsnNode source = null;
            List<Object> ints = new ArrayList<>();
            String lastString = null;
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                int op = insn.getOpcode();
                if (insn instanceof FieldInsnNode f && op == Opcodes.GETSTATIC
                        && (f.owner.equals("net/minecraft/init/Blocks") || f.owner.equals("net/minecraft/init/Items"))) {
                    source = f;
                    ints.clear();
                } else if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) {
                    ints.add(op - Opcodes.ICONST_0);
                } else if (insn instanceof IntInsnNode i && (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH)) {
                    ints.add(i.operand);
                } else if (insn instanceof LdcInsnNode l) {
                    if (l.cst instanceof Integer n) ints.add(n);
                    else if (l.cst instanceof String s) lastString = s;
                } else if (op == Opcodes.ILOAD) {
                    ints.add("var");
                } else if (insn instanceof MethodInsnNode m) {
                    if (m.owner.equals("net/minecraft/item/ItemStack") && m.name.equals("<init>") && source != null) {
                        boolean withMeta = m.desc.endsWith("II)V");
                        Object meta = withMeta && !ints.isEmpty() ? ints.get(ints.size() - 1) : Integer.valueOf(0);
                        String owner = source.owner.substring(source.owner.lastIndexOf('/') + 1);
                        if (meta instanceof Integer n) regs.add(new Reg(owner, source.name, n, false));
                        else regs.add(new Reg(owner, source.name, 0, true));
                        source = null;
                    } else if (m.owner.equals("thaumcraft/api/ThaumcraftApi") && m.name.equals("registerObjectTag")
                            && m.desc.startsWith("(Ljava/lang/String;") && lastString != null) {
                        oreNames.add(lastString);
                    }
                }
            }
        }
    }

    private static Map<String, String> readFields(Path csv) throws java.io.IOException {
        Map<String, String> out = new HashMap<>();
        for (String line : Files.readAllLines(csv)) {
            String[] p = line.split(",", 3);
            if (p.length >= 2 && p[0].startsWith("field_")) out.put(p[0], p[1]);
        }
        return out;
    }
}
