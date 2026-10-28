package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.IBlockColor;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.IItemColor;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.ItemColors;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import net.minecraft.client.Minecraft;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;

/**
 * Legacy block and item colour handlers on the game's colour registries. Handlers registered before the game asks for colour handlers
 * wait in the legacy registries and are added in {@code RegisterColorHandlersEvent}; later ones are added at once. Colours get an
 * opaque alpha (the game multiplies item tints by their alpha; legacy colours had none).
 */
public final class Colors1122 {
    private static final BlockColors BLOCKS = new BlockColors();
    private static final ItemColors ITEMS = new ItemColors(BLOCKS);
    private static volatile boolean blocksOpen, itemsOpen;

    private Colors1122() {}

    public static BlockColors blocks() {
        return BLOCKS;
    }

    public static ItemColors items() {
        return ITEMS;
    }

    /** The world legacy colour handlers read (the client world; a chunk mesher's region shows the same blocks). */
    private static IBlockAccess world() {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        Object world = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level);
        return world instanceof IBlockAccess access ? access : null;
    }

    static net.minecraft.client.color.block.BlockColor modernBlock(IBlockColor handler) {
        return (state, level, pos, tint) -> {
            try {
                IBlockState legacy = StatesBridge1122.toLegacy(state);
                BlockPos lp = pos == null ? null : new BlockPos(pos.getX(), pos.getY(), pos.getZ());
                return handler.colorMultiplier(legacy, level == null ? null : world(), lp, tint) | 0xFF000000;
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("a legacy block colour handler", e);
                return -1;
            }
        };
    }

    static net.minecraft.client.color.item.ItemColor modernItem(IItemColor handler) {
        return (stack, tint) -> {
            try {
                return handler.colorMultiplier(Stacks1122.fromModern(stack), tint) | 0xFF000000;
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("a legacy item colour handler", e);
                return -1;
            }
        };
    }

    public static void registerBlock(Block block, IBlockColor handler) {
        if (!blocksOpen || !(block.mcc$modern instanceof net.minecraft.world.level.block.Block modern)) return;
        original(modern);
        Minecraft.getInstance().getBlockColors().register(modernBlock(handler), modern);
    }

    /** The game's own colour handler of each block, taken before a legacy handler replaces it (null: the block has none). */
    private static final java.util.Map<net.minecraft.world.level.block.Block, java.util.Optional<net.minecraft.client.color.block.BlockColor>> ORIGINALS =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static java.lang.reflect.Field providers;

    /** Takes the game's handler of the block before anything replaces it. */
    public static void snapshot(net.minecraft.world.level.block.Block modern) {
        original(modern);
    }

    private static net.minecraft.client.color.block.BlockColor original(net.minecraft.world.level.block.Block modern) {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().getBlockColors() == null) return null; // not built yet: ask again later
        return ORIGINALS.computeIfAbsent(modern, b -> {
            try {
                if (providers == null) {
                    for (var f : net.minecraft.client.color.block.BlockColors.class.getDeclaredFields()) {
                        if (f.getType() == net.minecraft.core.IdMapper.class) {
                            f.setAccessible(true);
                            providers = f;
                        }
                    }
                }
                @SuppressWarnings("unchecked")
                var ids = (net.minecraft.core.IdMapper<net.minecraft.client.color.block.BlockColor>) providers.get(Minecraft.getInstance().getBlockColors());
                return java.util.Optional.ofNullable(ids.byId(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(b)));
            } catch (ReflectiveOperationException | RuntimeException e) {
                return java.util.Optional.empty();
            }
        }).orElse(null);
    }

    /**
     * The game's own colour of a state, never a legacy handler (the vanilla handler mods take from BlockColors.blockColorMap to wrap:
     * Quark wraps grass's and registers the wrapper for grass, so asking the game's colours again looped until the stack overflowed).
     */
    public static int originalBlockColor(IBlockState state, BlockPos pos, int tint) {
        try {
            var modernState = StatesBridge1122.toModern(state);
            var provider = original(modernState.getBlock());
            if (provider == null) return 0xFFFFFF;
            var level = Minecraft.getInstance().level;
            var modernPos = pos == null ? null : new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
            return provider.getColor(modernState, modernPos == null ? null : level, modernPos, tint) & 0xFFFFFF;
        } catch (RuntimeException | LinkageError e) {
            return 0xFFFFFF;
        }
    }

    public static void registerItem(Item item, IItemColor handler) {
        if (!itemsOpen) return;
        var modern = modernItems(item);
        if (modern.length > 0) Minecraft.getInstance().getItemColors().register(modernItem(handler), modern);
    }

    private static net.minecraft.world.level.ItemLike[] modernItems(Item item) {
        java.util.LinkedHashSet<net.minecraft.world.item.Item> set = new java.util.LinkedHashSet<>();
        for (int meta = 0; meta < 16; meta++) {
            var m = ItemBridge1122.modern(item, meta);
            if (m != null) set.add(m);
        }
        if (set.isEmpty() && item.mcc$modern instanceof net.minecraft.world.item.Item m) set.add(m);
        return set.toArray(new net.minecraft.world.level.ItemLike[0]);
    }

    static void onBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        try {
            legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new legacyconnector.v1_12_2.net.minecraftforge.client.event.ColorHandlerEvent.Block(BLOCKS));
        } catch (RuntimeException | LinkageError e) {
            RenderErrors1122.report("posting ColorHandlerEvent.Block", e);
        }
        for (var e : BLOCKS.mcc$handlers().entrySet()) {
            if (e.getKey().mcc$modern instanceof net.minecraft.world.level.block.Block modern) {
                original(modern);
                event.register(modernBlock(e.getValue()), modern);
            }
        }
        blocksOpen = true;
    }

    static void onItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        try {
            legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new legacyconnector.v1_12_2.net.minecraftforge.client.event.ColorHandlerEvent.Item(ITEMS, BLOCKS));
        } catch (RuntimeException | LinkageError e) {
            RenderErrors1122.report("posting ColorHandlerEvent.Item", e);
        }
        for (var e : ITEMS.mcc$handlers().entrySet()) {
            var modern = modernItems(e.getKey());
            if (modern.length > 0) event.register(modernItem(e.getValue()), modern);
        }
        itemsOpen = true;
    }

    /** The game's colour of a state with no legacy handler (vanilla grass, leaves, water). */
    public static int modernBlockColor(IBlockState state, BlockPos pos, int tint) {
        try {
            var level = Minecraft.getInstance().level;
            var modernPos = pos == null ? null : new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
            return Minecraft.getInstance().getBlockColors().getColor(StatesBridge1122.toModern(state), modernPos == null ? null : level, modernPos, tint) & 0xFFFFFF;
        } catch (RuntimeException | LinkageError e) {
            return 0xFFFFFF;
        }
    }

    public static int modernItemColor(ItemStack stack, int tint) {
        try {
            return Minecraft.getInstance().getItemColors().getColor(Stacks1122.toModern(stack), tint);
        } catch (RuntimeException | LinkageError e) {
            return -1;
        }
    }
}
