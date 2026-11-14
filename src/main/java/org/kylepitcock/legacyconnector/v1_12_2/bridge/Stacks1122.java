package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockItem;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Item;

/** Converts between 1.12.2 stacks (item, count, metadata, tag) and modern ones (flattened item, count, components). */
public final class Stacks1122 {
    private Stacks1122() {}

    public static net.minecraft.world.item.ItemStack toModern(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
        Item item = stack.getItem();
        net.minecraft.world.item.Item modern = ItemBridge1122.modern(item, stack.getMetadata());
        // Forge's universal bucket (not a registered item): the modern bucket of the fluid in its tag
        if (modern == null && item instanceof legacyconnector.v1_12_2.net.minecraftforge.fluids.UniversalBucket) return FluidBridge1122.modernUniversalBucket(stack);
        if (modern == null) return net.minecraft.world.item.ItemStack.EMPTY;
        var out = new net.minecraft.world.item.ItemStack(modern, stack.getCount());
        if ((item.isDamageable() || item.mcc$vanillaModern != null && modern.components().has(DataComponents.MAX_DAMAGE)) && stack.getMetadata() > 0) {
            out.setDamageValue(stack.getMetadata());
        }
        // 1.12 kept a filled map's id in its metadata
        if (modern == net.minecraft.world.item.Items.FILLED_MAP) out.set(DataComponents.MAP_ID, new net.minecraft.world.level.saveddata.maps.MapId(stack.getMetadata()));
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            var data = tag.mcc$modern().copy();
            if (isPotionItem(modern)) potionToModern(data, out);
            displayToModern(data, out);
            if (item.mcc$vanillaModern != null) restoreComponents(data, out);
            else if (data.contains(COMPONENTS)) data.remove(COMPONENTS);
            if (!data.isEmpty()) out.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        for (Hook hook : HOOKS) out = hook.toModern(stack, out);
        return out;
    }

    /** Stack conversion for items of modern mods that stand in for a legacy library (Patchouli's guide book keeps its book as a data component). */
    public interface Hook {
        /** The modern stack for a legacy one; {@code converted} is the default conversion (the hook may return another stack). */
        net.minecraft.world.item.ItemStack toModern(ItemStack legacy, net.minecraft.world.item.ItemStack converted);

        /** Called with the legacy stack made for a modern one. */
        void fromModern(net.minecraft.world.item.ItemStack modern, ItemStack legacy);
    }

    private static final java.util.List<Hook> HOOKS = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addHook(Hook hook) {
        HOOKS.add(hook);
    }

    /**
     * 1.12's {@code display} tag (Name, Lore: strings with section-sign codes) as the modern custom name and lore components, so the name and
     * the extra tooltip lines of a stack a mod labelled in code (a written report, a renamed item) show. The tag itself stays for legacy code.
     */
    public static void displayToModern(net.minecraft.nbt.CompoundTag data, net.minecraft.world.item.ItemStack out) {
        if (!data.contains("display", 10)) return;
        var display = data.getCompound("display");
        if (display.contains("Name", 8)) out.set(DataComponents.CUSTOM_NAME, legacyText(display.getString("Name")));
        if (display.contains("Lore", 9)) {
            var list = display.getList("Lore", 8);
            java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
            for (int i = 0; i < list.size(); i++) lines.add(legacyText(list.getString(i)));
            if (!lines.isEmpty()) out.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lines));
        }
    }

    /** A string with section-sign formatting codes as a styled component. */
    public static net.minecraft.network.chat.Component legacyText(String text) {
        if (text.indexOf('§') < 0) return net.minecraft.network.chat.Component.literal(text);
        var root = net.minecraft.network.chat.Component.empty();
        net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                var f = net.minecraft.ChatFormatting.getByCode(Character.toLowerCase(text.charAt(++i)));
                if (f == null) continue;
                if (run.length() > 0) {
                    root.append(net.minecraft.network.chat.Component.literal(run.toString()).withStyle(style));
                    run.setLength(0);
                }
                style = f == net.minecraft.ChatFormatting.RESET ? net.minecraft.network.chat.Style.EMPTY
                        : f.isColor() ? net.minecraft.network.chat.Style.EMPTY.applyFormat(f) : style.applyFormat(f);
            } else {
                run.append(c);
            }
        }
        if (run.length() > 0) root.append(net.minecraft.network.chat.Component.literal(run.toString()).withStyle(style));
        return root;
    }

    private static boolean isPotionItem(net.minecraft.world.item.Item item) {
        return item == net.minecraft.world.item.Items.POTION || item == net.minecraft.world.item.Items.SPLASH_POTION
                || item == net.minecraft.world.item.Items.LINGERING_POTION || item == net.minecraft.world.item.Items.TIPPED_ARROW;
    }

    /** 1.12's potion tags (Potion, CustomPotionColor, CustomPotionEffects) as the modern potion contents; unknown potions stay in the tag. */
    private static void potionToModern(net.minecraft.nbt.CompoundTag data, net.minecraft.world.item.ItemStack out) {
        java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion>> potion = java.util.Optional.empty();
        if (data.contains("Potion", 8)) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(data.getString("Potion"));
            var holder = id == null ? java.util.Optional.<net.minecraft.core.Holder.Reference<net.minecraft.world.item.alchemy.Potion>>empty()
                    : net.minecraft.core.registries.BuiltInRegistries.POTION.getHolder(id);
            if (holder.isPresent()) {
                potion = java.util.Optional.of(holder.get());
                data.remove("Potion");
            }
        }
        java.util.Optional<Integer> color = java.util.Optional.empty();
        if (data.contains("CustomPotionColor", 99)) {
            color = java.util.Optional.of(data.getInt("CustomPotionColor"));
            data.remove("CustomPotionColor");
        }
        java.util.List<net.minecraft.world.effect.MobEffectInstance> effects = new java.util.ArrayList<>();
        if (data.contains("CustomPotionEffects", 9)) {
            var list = data.getList("CustomPotionEffects", 10);
            boolean all = true;
            for (int i = 0; i < list.size(); i++) {
                var legacy = legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect.readCustomPotionEffectFromNBT(new NBTTagCompound(list.getCompound(i)));
                var effect = legacy == null ? null : legacy.mcc$toModern();
                if (effect != null) effects.add(effect);
                else all = false;
            }
            if (all) data.remove("CustomPotionEffects");
        }
        if (potion.isPresent() || color.isPresent() || !effects.isEmpty()) {
            out.set(DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(potion, color, effects));
        }
    }

    /** The modern potion contents as 1.12's potion tags. */
    private static void potionFromModern(net.minecraft.world.item.alchemy.PotionContents contents, ItemStack out) {
        NBTTagCompound tag = out.hasTagCompound() ? out.getTagCompound() : new NBTTagCompound();
        contents.potion().flatMap(net.minecraft.core.Holder::unwrapKey).ifPresent(k -> tag.setString("Potion", k.location().toString()));
        contents.customColor().ifPresent(c -> tag.setInteger("CustomPotionColor", c));
        if (!contents.customEffects().isEmpty()) {
            var list = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList();
            for (var e : contents.customEffects()) {
                var legacy = legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect.mcc$fromModern(e);
                if (legacy != null) list.appendTag(legacy.writeCustomPotionEffectToNBT(new NBTTagCompound()));
            }
            if (list.tagCount() > 0) tag.setTag("CustomPotionEffects", list);
        }
        if (!tag.isEmpty()) out.setTagCompound(tag);
    }

    /** Live views by the modern stack they stand for (modern stacks compare by identity). */
    private static final java.util.Map<net.minecraft.world.item.ItemStack, ItemStack> VIEWS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    /** Views whose tag legacy code may have changed since the last flush. */
    private static final java.util.Set<ItemStack> TOUCHED = java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    /**
     * A live view of a modern stack: the same legacy object for the same modern stack, so a mod that changes the count, damage or tag
     * of a held or stored stack changes the real one (counts and damage at once, tags at the next {@link #flushViews}).
     */
    public static ItemStack view(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack view = VIEWS.get(stack);
        if (view == null && WEAK_VIEWS != null) {
            var ref = WEAK_VIEWS.get(stack);
            view = ref == null ? null : ref.get();
        }
        if (view != null && view.getItem() == legacyOf(stack.getItem())) return view;
        view = fromModern(stack);
        if (view.isEmpty()) return view;
        view.mcc$bind(stack);
        if (WEAK_VIEWS != null) {
            VIEWS.remove(stack);
            WEAK_VIEWS.put(stack, new java.lang.ref.WeakReference<>(view));
        } else {
            VIEWS.put(stack, view);
        }
        return view;
    }

    /**
     * {@code perf.weakStackViews1122} (default off): views are held weakly, so a view nobody holds any more is collected with its
     * modern stack (in {@link #VIEWS} a view keeps its stack, the key, so every stack ever viewed stayed in memory). A view that
     * made capabilities ({@link #mcc$pin}) is held strongly as before: its providers may keep state that is not in the tag.
     */
    private static final java.util.Map<net.minecraft.world.item.ItemStack, java.lang.ref.WeakReference<ItemStack>> WEAK_VIEWS =
            org.kylepitcock.legacyconnector.core.LegacyConfig.perfWeakStackViews1122()
                    ? java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>()) : null;

    /** Keeps a view strongly from now on (it made capabilities). */
    public static void mcc$pin(ItemStack view) {
        if (WEAK_VIEWS == null) return;
        var backing = view.mcc$backing();
        if (backing == null || backing.isEmpty()) return;
        var ref = WEAK_VIEWS.get(backing);
        if (ref != null && ref.get() == view) VIEWS.put(backing, view);
    }

    /** The legacy item a modern item stands for (null when it has none). */
    private static Item legacyOf(net.minecraft.world.item.Item item) {
        if (item instanceof Legacy1122Item i) return i.legacy();
        if (item instanceof Legacy1122BlockItem b) return b.legacy();
        Object[] variant = Vanilla1122.legacyVariant(item);
        return variant != null ? (Item) variant[0] : Vanilla1122.legacyOf(item);
    }

    /** The modern stack for a legacy one: a view's own modern stack (its tag written back first), a new stack otherwise. */
    public static net.minecraft.world.item.ItemStack toModernLive(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
        var backing = stack.mcc$backing();
        if (backing != null && !backing.isEmpty()) {
            stack.mcc$commit();
            return backing;
        }
        return toModern(stack);
    }

    public static void touched(ItemStack view) {
        TOUCHED.add(view);
    }

    /** Writes the tags of the views legacy code read since the last flush back into their modern stacks. */
    public static void flushViews() {
        if (TOUCHED.isEmpty()) return;
        ItemStack[] views;
        synchronized (TOUCHED) {
            views = TOUCHED.toArray(new ItemStack[0]);
            TOUCHED.clear();
        }
        for (ItemStack v : views) {
            try {
                v.mcc$commit();
            } catch (RuntimeException e) {
                // a tag a modern component cannot hold stays with the legacy view
            }
        }
    }

    public static ItemStack fromModern(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        Item legacy;
        int meta;
        boolean standIn = false;
        if (stack.getItem() instanceof Legacy1122Item i) {
            legacy = i.legacy();
            meta = i.meta();
        } else if (stack.getItem() instanceof Legacy1122BlockItem b) {
            legacy = b.legacy();
            meta = b.meta();
        } else {
            Object[] variant = Vanilla1122.legacyVariant(stack.getItem());
            legacy = variant != null ? (Item) variant[0] : Vanilla1122.legacyOf(stack.getItem());
            if (legacy == null) {
                // a generated bucket of a 1.12.2 fluid: Forge's universal bucket holding it
                ItemStack bucket = FluidBridge1122.legacyBucketStack(stack);
                if (bucket != null) return bucket;
                // an item 1.12.2 never had (a post-1.12 vanilla item, a modern mod's): its legacy stand-in, so legacy code can hold, move and hand it back
                standIn = true;
                legacy = Vanilla1122.standIn(stack.getItem());
            }
            meta = variant != null ? (Integer) variant[1] : 0;
        }
        if (legacy.isDamageable() || stack.isDamageableItem()) meta = stack.getDamageValue();
        var mapId = stack.get(DataComponents.MAP_ID);
        if (mapId != null) meta = mapId.id();
        ItemStack out = new ItemStack(legacy, stack.getCount(), meta);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) out.setTagCompound(new NBTTagCompound(data.copyTag()));
        var potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null && isPotionItem(stack.getItem())) potionFromModern(potion, out);
        if (standIn) keepComponents(stack, out);
        for (Hook hook : HOOKS) hook.fromModern(stack, out);
        return out;
    }

    private static final String COMPONENTS = "mcc$components";

    /** A stand-in's stack keeps the modern stack's component changes (enchantments, contents, names...) in its tag, so they come back unchanged. */
    private static void keepComponents(net.minecraft.world.item.ItemStack stack, ItemStack out) {
        var patch = stack.getComponentsPatch();
        if (patch.isEmpty()) return;
        try {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, server.registryAccess());
            var encoded = net.minecraft.core.component.DataComponentPatch.CODEC.encodeStart(ops, patch).result();
            if (encoded.isEmpty()) return;
            NBTTagCompound tag = out.hasTagCompound() ? out.getTagCompound() : new NBTTagCompound();
            tag.mcc$modern().put(COMPONENTS, encoded.get());
            out.setTagCompound(tag);
        } catch (RuntimeException | LinkageError e) {
            // the stack still travels with its item and count
        }
    }

    /** Applies the component changes {@link #keepComponents} stored, and takes them out of the custom data. */
    private static void restoreComponents(net.minecraft.nbt.CompoundTag data, net.minecraft.world.item.ItemStack out) {
        var saved = data.get(COMPONENTS);
        if (saved == null) return;
        data.remove(COMPONENTS);
        try {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, server.registryAccess());
            net.minecraft.core.component.DataComponentPatch.CODEC.parse(ops, saved).result().ifPresent(out::applyComponents);
        } catch (RuntimeException | LinkageError e) {
            // the item and count stay
        }
    }
}
