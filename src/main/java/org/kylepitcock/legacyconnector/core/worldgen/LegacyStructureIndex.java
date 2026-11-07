package org.kylepitcock.legacyconnector.core.worldgen;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Where the structures of legacy world generators were recorded ({@link LegacyGeneratorStructures}), kept per dimension in saved data, and
 * {@code /locate structure} answering from it. The game's own locate asks each chunk (the structure set is a spacing-1 grid, so it walks up
 * to 100 rings of chunks, loading every one to the structure-start status); with the index the nearest recorded structure is found at once,
 * at any distance, without loading chunks. A structure with nothing recorded yet, and every other structure or tag, goes to the game's own
 * command (registered by merging a handler into the existing {@code locate structure <structure>} node, as an addon command would).
 */
public final class LegacyStructureIndex extends SavedData {
    private static final String NAME = "legacyconnector_structures";
    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(o -> Component.translatable("commands.locate.structure.invalid", o));
    private static boolean registered;

    /** Structure id to the block positions (x and z, y 0) of the middle of each recorded build. */
    private final Map<ResourceLocation, Set<Long>> origins = new HashMap<>();

    public static SavedData.Factory<LegacyStructureIndex> factory() {
        return new SavedData.Factory<>(LegacyStructureIndex::new, LegacyStructureIndex::load, null);
    }

    static LegacyStructureIndex of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), NAME);
    }

    private static LegacyStructureIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        LegacyStructureIndex index = new LegacyStructureIndex();
        CompoundTag all = tag.getCompound("structures");
        for (String key : all.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) continue;
            Set<Long> set = new HashSet<>();
            for (long l : all.getLongArray(key)) set.add(l);
            index.origins.put(id, set);
        }
        return index;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        for (var e : origins.entrySet()) all.put(e.getKey().toString(), new LongArrayTag(e.getValue().stream().mapToLong(Long::longValue).toArray()));
        tag.put("structures", all);
        return tag;
    }

    /** A build of {@code structure} was recorded around this block position. */
    static void record(ServerLevel level, ResourceLocation structure, int x, int z) {
        try {
            LegacyStructureIndex index = of(level);
            synchronized (index) {
                if (index.origins.computeIfAbsent(structure, k -> new HashSet<>()).add(BlockPos.asLong(x, 0, z))) index.setDirty();
            }
        } catch (RuntimeException e) {
            // the index is an aid; the recorded start itself is what the game keeps
        }
    }

    private synchronized long nearest(ResourceLocation structure, int x, int z) {
        Set<Long> set = origins.get(structure);
        if (set == null || set.isEmpty()) return Long.MIN_VALUE;
        long best = Long.MIN_VALUE;
        double bestDist = Double.MAX_VALUE;
        for (long l : set) {
            double dx = BlockPos.getX(l) - x, dz = BlockPos.getZ(l) - z;
            double d = dx * dx + dz * dz;
            if (d < bestDist) {
                bestDist = d;
                best = l;
            }
        }
        return best;
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // after the game registered its own locate command
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, (RegisterCommandsEvent e) -> wrap(e.getDispatcher()));
    }

    private static void wrap(CommandDispatcher<CommandSourceStack> dispatcher) {
        var locate = dispatcher.getRoot().getChild("locate");
        var structure = locate == null ? null : locate.getChild("structure");
        var argument = structure == null ? null : structure.getChild("structure");
        if (argument == null || argument.getCommand() == null) return;
        Command<CommandSourceStack> original = argument.getCommand();
        dispatcher.register(Commands.literal("locate").then(Commands.literal("structure")
                .then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE)).executes(ctx -> run(ctx, original)))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, Command<CommandSourceStack> original) throws CommandSyntaxException {
        var arg = ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, "structure", Registries.STRUCTURE, INVALID);
        var key = arg.unwrap().left();
        if (key.isPresent()) {
            ResourceLocation id = key.get().location();
            CommandSourceStack source = ctx.getSource();
            BlockPos here = BlockPos.containing(source.getPosition());
            long found = of(source.getLevel()).nearest(id, here.getX(), here.getZ());
            if (found != Long.MIN_VALUE) {
                int x = BlockPos.getX(found), z = BlockPos.getZ(found);
                int distance = Mth.floor(Mth.sqrt((float) (((double) x - here.getX()) * (x - here.getX()) + ((double) z - here.getZ()) * (z - here.getZ()))));
                Component coords = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", x, "~", z)).withStyle(s -> s
                        .withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + x + " ~ " + z))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))));
                source.sendSuccess(() -> Component.translatable("commands.locate.structure.success", arg.asPrintable(), coords, distance), false);
                return distance;
            }
        }
        return original.run(ctx);
    }
}
