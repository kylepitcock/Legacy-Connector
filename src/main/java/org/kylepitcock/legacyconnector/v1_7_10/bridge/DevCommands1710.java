package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.WorldProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockEntity;

/**
 * Development commands, {@code /legacyconnector ...} (permission level 2): switch dimensions, open any mod GUI by id,
 * place blocks with metadata, give items of a subtype, spawn legacy entities, jump to biomes and inspect what you look at.
 */
public final class DevCommands1710 {
    private DevCommands1710() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DevCommands1710::onRegister);
    }

    private static void onRegister(RegisterCommandsEvent event) {
        var root = Commands.literal("legacyconnector").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("mods").executes(ctx -> mods(ctx.getSource())));
        root.then(Commands.literal("dim")
                .then(Commands.literal("list").executes(ctx -> dimList(ctx.getSource())))
                .then(Commands.argument("id", IntegerArgumentType.integer())
                        .executes(ctx -> dim(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), null))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> dim(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), BlockPosArgument.getBlockPos(ctx, "pos"))))));
        root.then(Commands.literal("gui")
                .then(Commands.argument("mod", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(LegacyMods.ordered().stream().map(r -> r.legacyId).toList(), b))
                        .then(Commands.argument("id", IntegerArgumentType.integer())
                                .executes(ctx -> gui(ctx.getSource(), StringArgumentType.getString(ctx, "mod"), IntegerArgumentType.getInteger(ctx, "id"), null))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> gui(ctx.getSource(), StringArgumentType.getString(ctx, "mod"), IntegerArgumentType.getInteger(ctx, "id"),
                                                BlockPosArgument.getBlockPos(ctx, "pos")))))));
        root.then(Commands.literal("place")
                .then(Commands.argument("block", ResourceLocationArgument.id()).suggests((c, b) -> SharedSuggestionProvider.suggestResource(legacyBlockIds(), b))
                        .executes(ctx -> place(ctx.getSource(), ResourceLocationArgument.getId(ctx, "block"), 0))
                        .then(Commands.argument("meta", IntegerArgumentType.integer(0, 15))
                                .executes(ctx -> place(ctx.getSource(), ResourceLocationArgument.getId(ctx, "block"), IntegerArgumentType.getInteger(ctx, "meta"))))));
        root.then(Commands.literal("give")
                .then(Commands.argument("item", ResourceLocationArgument.id()).suggests((c, b) -> SharedSuggestionProvider.suggestResource(legacyItemIds(), b))
                        .executes(ctx -> give(ctx.getSource(), ResourceLocationArgument.getId(ctx, "item"), 0, 1))
                        .then(Commands.argument("meta", IntegerArgumentType.integer(0))
                                .executes(ctx -> give(ctx.getSource(), ResourceLocationArgument.getId(ctx, "item"), IntegerArgumentType.getInteger(ctx, "meta"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> give(ctx.getSource(), ResourceLocationArgument.getId(ctx, "item"), IntegerArgumentType.getInteger(ctx, "meta"),
                                                IntegerArgumentType.getInteger(ctx, "count")))))));
        root.then(Commands.literal("spawn")
                .then(Commands.argument("entity", ResourceLocationArgument.id()).suggests((c, b) -> SharedSuggestionProvider.suggestResource(legacyEntityIds(), b))
                        .executes(ctx -> spawn(ctx.getSource(), ResourceLocationArgument.getId(ctx, "entity"), 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 32))
                                .executes(ctx -> spawn(ctx.getSource(), ResourceLocationArgument.getId(ctx, "entity"), IntegerArgumentType.getInteger(ctx, "count"))))));
        root.then(Commands.literal("biome")
                .then(Commands.argument("biome", ResourceLocationArgument.id()).suggests((c, b) -> SharedSuggestionProvider.suggestResource(
                        c.getSource().getServer().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).keySet().stream()
                                .filter(k -> !k.getNamespace().equals("minecraft")), b))
                        .executes(ctx -> biome(ctx.getSource(), ResourceLocationArgument.getId(ctx, "biome")))));
        root.then(Commands.literal("inspect").executes(ctx -> inspect(ctx.getSource())));
        event.getDispatcher().register(root);
    }

    // ------------------------------------------------------------------ id lists

    private static List<ResourceLocation> legacyBlockIds() {
        List<ResourceLocation> out = new ArrayList<>();
        for (var e : BuiltInRegistries.BLOCK.entrySet()) if (e.getValue() instanceof LegacyBlockAdapter) out.add(e.getKey().location());
        return out;
    }

    private static List<ResourceLocation> legacyItemIds() {
        List<ResourceLocation> out = new ArrayList<>();
        for (var e : BuiltInRegistries.ITEM.entrySet()) if (!e.getKey().location().getNamespace().equals("minecraft")) out.add(e.getKey().location());
        return out;
    }

    private static List<ResourceLocation> legacyEntityIds() {
        List<ResourceLocation> out = new ArrayList<>();
        for (var e : BuiltInRegistries.ENTITY_TYPE.entrySet()) if (!e.getKey().location().getNamespace().equals("minecraft")) out.add(e.getKey().location());
        return out;
    }

    // ------------------------------------------------------------------ commands

    private static int mods(CommandSourceStack source) {
        var all = LegacyMods.ordered();
        source.sendSuccess(() -> Component.literal(all.size() + " legacy mod(s):"), false);
        for (LegacyModRecord r : all) {
            source.sendSuccess(() -> Component.literal("  " + r.name() + " [" + r.legacyId + "] " + r.modVersion() + " (" + r.version + ") -> " + r.modernId), false);
        }
        return all.size();
    }

    private static int dimList(CommandSourceStack source) {
        int n = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            n++;
            source.sendSuccess(() -> Component.literal("  " + WorldProvider.dimensionId(level.dimension()) + "  " + level.dimension().location()), false);
        }
        return n;
    }

    private static int dim(CommandSourceStack source, int id, BlockPos pos) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel target = DimensionBridge.level(source.getServer(), id);
        if (target == null) {
            source.sendFailure(Component.literal("No dimension " + id + " (see /legacyconnector dim list)"));
            return 0;
        }
        int x = pos != null ? pos.getX() : player.getBlockX();
        int z = pos != null ? pos.getZ() : player.getBlockZ();
        int y;
        if (pos != null) {
            y = pos.getY();
        } else {
            target.getChunk(x >> 4, z >> 4);
            y = target.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) + 1;
            if (y <= target.getMinBuildHeight() + 1) y = 100;
        }
        player.teleportTo(target, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
        int finalY = y;
        source.sendSuccess(() -> Component.literal("Teleported to dimension " + id + " at " + x + " " + finalY + " " + z), false);
        return 1;
    }

    private static int gui(CommandSourceStack source, String modId, int id, BlockPos pos) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LegacyModRecord record = LegacyMods.byLegacyId(modId);
        if (record == null || record.instance == null) {
            source.sendFailure(Component.literal("No legacy mod '" + modId + "' (see /legacyconnector mods)"));
            return 0;
        }
        BlockPos at = pos != null ? pos : player.blockPosition();
        GuiBridge.openGui((legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer) EntityBridge.wrap(player), record.instance, id,
                World.mcc$wrap(player.serverLevel()), at.getX(), at.getY(), at.getZ());
        source.sendSuccess(() -> Component.literal("Asked " + modId + " to open GUI " + id + " at " + at.toShortString()), false);
        return 1;
    }

    private static int place(CommandSourceStack source, ResourceLocation id, int meta) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var block = BuiltInRegistries.BLOCK.get(id);
        if (!(block instanceof LegacyBlockAdapter)) {
            source.sendFailure(Component.literal(id + " is not a legacy block"));
            return 0;
        }
        BlockPos target;
        HitResult hit = player.pick(8.0, 0.0F, false);
        target = hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK ? b.getBlockPos().relative(b.getDirection()) : player.blockPosition().relative(player.getDirection(), 2);
        player.serverLevel().setBlockAndUpdate(target, block.defaultBlockState().setValue(LegacyBlockAdapter.META, meta));
        source.sendSuccess(() -> Component.literal("Placed " + id + "[meta=" + meta + "] at " + target.toShortString()), false);
        return 1;
    }

    private static int give(CommandSourceStack source, ResourceLocation id, int meta, int count) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            source.sendFailure(Component.literal("Unknown item " + id + " (tab completion lists the legacy items)"));
            return 0;
        }
        var item = BuiltInRegistries.ITEM.get(id);
        if (meta != 0) {
            ResourceLocation variant = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_" + meta);
            if (BuiltInRegistries.ITEM.containsKey(variant)) item = BuiltInRegistries.ITEM.get(variant);
        }
        if (!BuiltInRegistries.ITEM.containsKey(BuiltInRegistries.ITEM.getKey(item)) || item == net.minecraft.world.item.Items.AIR) {
            source.sendFailure(Component.literal("Unknown item " + id));
            return 0;
        }
        ItemStack stack = new ItemStack(item, count);
        player.getInventory().add(stack);
        source.sendSuccess(() -> Component.literal("Gave " + count + " x " + BuiltInRegistries.ITEM.getKey(stack.getItem())), false);
        return 1;
    }

    private static int spawn(CommandSourceStack source, ResourceLocation id, int count) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            source.sendFailure(Component.literal("Unknown entity " + id));
            return 0;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        var look = player.getLookAngle();
        int made = 0;
        for (int i = 0; i < count; i++) {
            var entity = type.create(player.serverLevel());
            if (entity == null) continue;
            entity.moveTo(player.getX() + look.x * 4 + (i % 4) * 1.5, player.getY(), player.getZ() + look.z * 4 + (i / 4) * 1.5, player.getYRot() + 180.0F, 0.0F);
            if (entity instanceof Mob mob) mob.finalizeSpawn(player.serverLevel(), player.serverLevel().getCurrentDifficultyAt(player.blockPosition()),
                    net.minecraft.world.entity.MobSpawnType.COMMAND, null);
            if (player.serverLevel().addFreshEntity(entity)) made++;
        }
        int total = made;
        source.sendSuccess(() -> Component.literal("Spawned " + total + " x " + id), false);
        return made;
    }

    private static int biome(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        var registry = source.getServer().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var holder = registry.getHolder(ResourceLocation.parse(id.toString()));
        if (holder.isEmpty()) {
            source.sendFailure(Component.literal("Unknown biome " + id));
            return 0;
        }
        var found = level.findClosestBiome3d(h -> h.is(id), player.blockPosition(), 6400, 32, 64);
        if (found == null) {
            source.sendFailure(Component.literal("No " + id + " within 6400 blocks"));
            return 0;
        }
        BlockPos pos = found.getFirst();
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) + 1;
        player.teleportTo(level, pos.getX() + 0.5, y, pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        source.sendSuccess(() -> Component.literal("Teleported to " + id + " at " + pos.getX() + " " + y + " " + pos.getZ()), false);
        return 1;
    }

    private static int inspect(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(10.0, 0.0F, false);
        // entities first
        var from = player.getEyePosition();
        var to = from.add(player.getLookAngle().scale(10.0));
        var entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player.level(), player, from, to,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(10.0)).inflate(1.0), e -> !e.isSpectator());
        if (entityHit instanceof EntityHitResult eh) {
            var e = eh.getEntity();
            var view = EntityBridge.wrap(e);
            source.sendSuccess(() -> Component.literal("Entity " + BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()) + " legacy class " + view.getClass().getName()
                    + " at " + e.blockPosition().toShortString()), false);
            return 1;
        }
        if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) {
            var state = player.level().getBlockState(b.getBlockPos());
            var block = state.getBlock();
            StringBuilder sb = new StringBuilder("Block ").append(BuiltInRegistries.BLOCK.getKey(block));
            if (block instanceof LegacyBlockAdapter adapter) {
                var legacy = (legacyconnector.v1_7_10.net.minecraft.block.Block) adapter.legacy();
                sb.append("  legacy class ").append(legacy.getClass().getName()).append("  meta ").append(state.getValue(LegacyBlockAdapter.META))
                        .append("  render type ").append(legacy.getRenderType());
                if (player.level().getBlockEntity(b.getBlockPos()) instanceof LegacyBlockEntity be) {
                    var te = be.legacy();
                    sb.append("  tile entity ").append(te == null ? "none" : te.getClass().getName());
                }
            }
            String text = sb.toString();
            source.sendSuccess(() -> Component.literal(text), false);
            return 1;
        }
        source.sendFailure(Component.literal("Nothing in range"));
        return 0;
    }
}
