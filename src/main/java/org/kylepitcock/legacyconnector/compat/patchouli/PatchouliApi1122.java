package org.kylepitcock.legacyconnector.compat.patchouli;

import java.io.InputStream;
import java.util.function.Predicate;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.vazkii.patchouli.api.IMultiblock;
import legacyconnector.v1_12_2.vazkii.patchouli.api.IStateMatcher;
import legacyconnector.v1_12_2.vazkii.patchouli.api.PatchouliAPI;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The 1.12 Patchouli API over the modern Patchouli. Legacy states, stacks, positions and worlds are converted with the layer's
 * bridges; legacy ids given as strings are the 1.12 ones ({@link Ids1122}).
 */
public final class PatchouliApi1122 implements PatchouliAPI.IPatchouliAPI {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Patchouli");

    private static vazkii.patchouli.api.PatchouliAPI.IPatchouliAPI api() {
        return vazkii.patchouli.api.PatchouliAPI.get();
    }

    @Override
    public boolean isStub() {
        return api().isStub();
    }

    @Override
    public void setConfigFlag(String flag, boolean value) {
        api().setConfigFlag(flag, value);
    }

    @Override
    public boolean getConfigFlag(String flag) {
        return api().getConfigFlag(flag);
    }

    @Override
    public void openBookGUI(EntityPlayerMP player, ResourceLocation book) {
        api().openBookGUI(player.mcc$serverPlayer(), book.mcc$modern());
    }

    @Override
    public void openBookGUI(ResourceLocation book) {
        api().openBookGUI(book.mcc$modern());
    }

    @Override
    public ResourceLocation getOpenBookGui() {
        var open = api().getOpenBookGui();
        return open == null ? null : new ResourceLocation(open.toString());
    }

    @Override
    public void reloadBookContents() {
        try {
            // ClientBookRegistry.INSTANCE.reload(false): not part of the API, so reached by name
            Class<?> registry = Class.forName("vazkii.patchouli.client.book.ClientBookRegistry");
            Object instance = registry.getField("INSTANCE").get(null);
            registry.getMethod("reload", boolean.class).invoke(instance, false);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            LOG.debug("Cannot reload book contents: {}", e.toString());
        }
    }

    @Override
    public ItemStack getBookStack(String book) {
        var stack = api().getBookStack(Ids1122.rl(book));
        if (stack.isEmpty()) return ItemStack.EMPTY;
        // the guide book is a modern item: give legacy code an item for it first (a view keeps the modern stack with its book component)
        Vanilla1122.itemForModern(stack.getItem());
        return Stacks1122.view(stack);
    }

    @Override
    public void registerTemplateAsBuiltin(ResourceLocation res, Supplier<InputStream> streamProvider) {
        api().registerTemplateAsBuiltin(res.mcc$modern(), streamProvider);
    }

    @Override
    public ItemStack deserializeItemStack(String str) {
        ItemStack stack = Ids1122.stack(str);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public String serializeItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        var item = stack.getItem();
        String name = item.getRegistryName() == null ? "minecraft:air" : item.getRegistryName().toString();
        StringBuilder sb = new StringBuilder(name);
        if (stack.getMetadata() != 0) sb.append(':').append(stack.getMetadata());
        if (stack.getCount() != 1) sb.append('#').append(stack.getCount());
        if (stack.getTagCompound() != null) sb.append(stack.getTagCompound());
        return sb.toString();
    }

    @Override
    public Ingredient deserializeIngredient(String str) {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (String part : str.split(",")) {
            ItemStack s = Ids1122.stack(part.trim());
            if (s != null && !s.isEmpty()) stacks.add(s);
        }
        return Ingredient.fromStacks(stacks.toArray(new ItemStack[0]));
    }

    @Override
    public String serializeIngredient(Ingredient ingredient) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack s : ingredient.getMatchingStacks()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(serializeItemStack(s));
        }
        return sb.toString();
    }

    @Override
    public IMultiblock getMultiblock(ResourceLocation res) {
        var found = api().getMultiblock(res.mcc$modern());
        return found == null ? null : Multiblock1122.view(() -> found);
    }

    /** Multiblocks the mods registered before their blocks existed as modern blocks: registered with the modern Patchouli by {@link #flush}. */
    private static final java.util.Map<net.minecraft.resources.ResourceLocation, IMultiblock> PENDING = new java.util.LinkedHashMap<>();
    private static boolean flushed;

    @Override
    public synchronized IMultiblock registerMultiblock(ResourceLocation res, IMultiblock mb) {
        if (mb instanceof Multiblock1122 view) {
            if (flushed) api().registerMultiblock(res.mcc$modern(), view.modern());
            else PENDING.put(res.mcc$modern(), mb);
        }
        return mb;
    }

    /** Registers the multiblocks the mods made in preInit (called once every mod's blocks are registered). */
    public static synchronized void flush() {
        flushed = true;
        for (var e : PENDING.entrySet()) {
            try {
                api().registerMultiblock(e.getKey(), ((Multiblock1122) e.getValue()).modern());
            } catch (RuntimeException | LinkageError ex) {
                LOG.warn("Cannot register the multiblock {}: {}", e.getKey(), ex.toString());
            }
        }
        PENDING.clear();
    }

    @Override
    public IMultiblock makeMultiblock(String[][] pattern, Object... targets) {
        return Multiblock1122.view(() -> {
            Object[] converted = new Object[targets.length];
            for (int i = 0; i < targets.length; i++) {
                Object t = targets[i];
                if (t instanceof IStateMatcher m) converted[i] = Matchers1122.modern(m);
                else if (t instanceof IBlockState s) converted[i] = api().stateMatcher(StatesBridge1122.toModern(s));
                else if (t instanceof Block b) converted[i] = api().looseBlockMatcher(Ids1122.modern(b));
                else converted[i] = t;
            }
            return api().makeMultiblock(pattern, converted);
        });
    }

    @Override
    public IStateMatcher predicateMatcher(IBlockState display, Predicate<IBlockState> predicate) {
        return Matchers1122.view(() -> api().predicateMatcher(StatesBridge1122.toModern(display), modernPredicate(predicate)));
    }

    @Override
    public IStateMatcher predicateMatcher(Block display, Predicate<IBlockState> predicate) {
        return Matchers1122.view(() -> api().predicateMatcher(Ids1122.modern(display), modernPredicate(predicate)));
    }

    private static Predicate<BlockState> modernPredicate(Predicate<IBlockState> legacy) {
        return s -> {
            IBlockState l = StatesBridge1122.toLegacy(s);
            return l != null && legacy.test(l);
        };
    }

    @Override
    public IStateMatcher stateMatcher(IBlockState state) {
        return Matchers1122.view(() -> api().stateMatcher(StatesBridge1122.toModern(state)));
    }

    @Override
    public IStateMatcher looseBlockMatcher(Block block) {
        return Matchers1122.view(() -> api().looseBlockMatcher(Ids1122.modern(block)));
    }

    @Override
    public IStateMatcher strictBlockMatcher(Block block) {
        return Matchers1122.view(() -> api().strictBlockMatcher(Ids1122.modern(block)));
    }

    @Override
    public IStateMatcher displayOnlyMatcher(IBlockState state) {
        return Matchers1122.view(() -> api().displayOnlyMatcher(StatesBridge1122.toModern(state)));
    }

    @Override
    public IStateMatcher displayOnlyMatcher(Block block) {
        return Matchers1122.view(() -> api().displayOnlyMatcher(Ids1122.modern(block)));
    }

    @Override
    public IStateMatcher airMatcher() {
        return Matchers1122.view(() -> api().airMatcher());
    }

    @Override
    public IStateMatcher anyMatcher() {
        return Matchers1122.view(() -> api().anyMatcher());
    }
}
