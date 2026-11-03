package org.kylepitcock.legacyconnector.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.Deflater;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Join-size check ({@code -Dlegacyconnector.joinSizeTest=true}, or any run with {@code -Dlegacyconnector.entitySelfTest=true}):
 * builds the payloads a joining client receives (recipes, tags, synced registries, NeoForge registry sync, commands,
 * advancements), measures raw and zlib-compressed size (what the 21-bit frame limit of 2,097,151 bytes applies to) and
 * logs them largest first. Also used as a startup warning: {@link #warnIfOversized} is not run in normal play.
 */
public final class JoinPayloadSizeTest {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/JoinSize");
    static final int FRAME_LIMIT = (1 << 21) - 1;
    private static final int WARN = 1 << 20;

    private JoinPayloadSizeTest() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            if (!Boolean.getBoolean("legacyconnector.joinSizeTest") && !Boolean.getBoolean("legacyconnector.entitySelfTest")) {
                warnIfOversized(e.getServer());
                return;
            }
            try {
                run(e.getServer());
            } catch (Throwable t) {
                LOG.warn("join size test failed", t);
            }
        });
    }

    record Row(String name, int raw, int packed) {}

    static int compressedSize(ByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);
        if (data.length < 256) return data.length;
        Deflater d = new Deflater();
        d.setInput(data);
        d.finish();
        byte[] out = new byte[8192];
        int total = 0;
        while (!d.finished()) total += d.deflate(out);
        d.end();
        return total;
    }

    private static <B extends FriendlyByteBuf, T> Row measure(String name, StreamCodec<B, T> codec, B buf, T value) {
        try {
            codec.encode(buf, value);
            return new Row(name, buf.readableBytes(), compressedSize(buf));
        } finally {
            buf.release();
        }
    }

    private static RegistryFriendlyByteBuf rbuf(RegistryAccess access) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void run(MinecraftServer server) {
        RegistryAccess access = server.registryAccess();
        List<Row> rows = new ArrayList<>();
        StringBuilder detail = new StringBuilder();

        // recipes
        Collection<RecipeHolder<?>> recipes = server.getRecipeManager().getRecipes();
        try {
            rows.add(measure("UpdateRecipes (" + recipes.size() + " recipes)", (StreamCodec) ClientboundUpdateRecipesPacket.STREAM_CODEC,
                    rbuf(access), new ClientboundUpdateRecipesPacket(recipes)));
            Map<String, List<RecipeHolder<?>>> byNs = new TreeMap<>();
            for (RecipeHolder<?> h : recipes) byNs.computeIfAbsent(h.id().getNamespace(), k -> new ArrayList<>()).add(h);
            List<Row> nsRows = new ArrayList<>();
            for (var en : byNs.entrySet()) {
                nsRows.add(measure(en.getKey() + " x" + en.getValue().size(), (StreamCodec) ClientboundUpdateRecipesPacket.STREAM_CODEC,
                        rbuf(access), new ClientboundUpdateRecipesPacket(en.getValue())));
            }
            nsRows.sort(Comparator.comparingInt(Row::raw).reversed());
            detail.append("\n  recipes by namespace (raw/compressed, top 25 of ").append(nsRows.size()).append("):");
            for (int i = 0; i < Math.min(25, nsRows.size()); i++) {
                detail.append("\n    ").append(nsRows.get(i).name()).append(": ").append(nsRows.get(i).raw()).append(" / ").append(nsRows.get(i).packed());
            }
            Map<String, Integer> bySer = new TreeMap<>();
            Map<String, Integer> serBytes = new TreeMap<>();
            for (RecipeHolder<?> h : recipes) {
                String key = net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER.getKey(h.value().getSerializer()) + "";
                bySer.merge(key, 1, Integer::sum);
                var b = rbuf(access);
                try {
                    ((StreamCodec) h.value().getSerializer().streamCodec()).encode(b, h.value());
                    serBytes.merge(key, b.readableBytes(), Integer::sum);
                } catch (RuntimeException ignored) {
                } finally {
                    b.release();
                }
            }
            detail.append("\n  recipes by serializer (count, raw bytes of the recipe body):");
            serBytes.entrySet().stream().sorted((a, c) -> c.getValue() - a.getValue()).limit(15)
                    .forEach(en -> detail.append("\n    ").append(en.getKey()).append(": ").append(bySer.get(en.getKey())).append(", ").append(en.getValue()));
        } catch (RuntimeException ex) {
            LOG.warn("recipe measurement failed", ex);
        }

        // tags
        try {
            var tags = TagNetworkSerialization.serializeTagsToNetwork(server.registries());
            rows.add(measure("UpdateTags (" + tags.size() + " registries)", (StreamCodec) ClientboundUpdateTagsPacket.STREAM_CODEC,
                    new FriendlyByteBuf(Unpooled.buffer()), new ClientboundUpdateTagsPacket(tags)));
            List<Row> tagRows = new ArrayList<>();
            for (var en : tags.entrySet()) {
                tagRows.add(measure("tags " + en.getKey().location(), (StreamCodec) ClientboundUpdateTagsPacket.STREAM_CODEC,
                        new FriendlyByteBuf(Unpooled.buffer()), new ClientboundUpdateTagsPacket(Map.of(en.getKey(), en.getValue()))));
            }
            tagRows.sort(Comparator.comparingInt(Row::raw).reversed());
            detail.append("\n  tags by registry (raw/compressed, top 10):");
            for (int i = 0; i < Math.min(10, tagRows.size()); i++) {
                detail.append("\n    ").append(tagRows.get(i).name()).append(": ").append(tagRows.get(i).raw()).append(" / ").append(tagRows.get(i).packed());
            }
        } catch (RuntimeException ex) {
            LOG.warn("tag measurement failed", ex);
        }

        // synced datapack registries, one packet per registry as the server sends them
        try {
            List<Row> regRows = new ArrayList<>();
            RegistrySynchronization.packRegistries(net.minecraft.resources.RegistryOps.create(NbtOps.INSTANCE, access),
                    access, Set.of(), (key, entries) -> regRows.add(measure("registry " + key.location() + " (" + entries.size() + " entries)",
                            (StreamCodec) ClientboundRegistryDataPacket.STREAM_CODEC, new FriendlyByteBuf(Unpooled.buffer()),
                            new ClientboundRegistryDataPacket(key, entries))));
            rows.addAll(regRows);
        } catch (RuntimeException ex) {
            LOG.warn("registry sync measurement failed", ex);
        }

        // NeoForge registry id sync (one payload per synced registry)
        try {
            for (var p : net.neoforged.neoforge.registries.RegistryManager.generateRegistryPackets(false)) {
                rows.add(measure("neoforge FrozenRegistry " + p.registryName(), (StreamCodec) net.neoforged.neoforge.network.payload.FrozenRegistryPayload.STREAM_CODEC,
                        new FriendlyByteBuf(Unpooled.buffer()), p));
            }
        } catch (Throwable ex) {
            LOG.warn("neoforge registry payload measurement failed: {}", ex.toString());
        }

        // commands
        try {
            var root = (com.mojang.brigadier.tree.RootCommandNode) (com.mojang.brigadier.tree.RootCommandNode<?>) server.getCommands().getDispatcher().getRoot();
            rows.add(measure("Commands", (StreamCodec) net.minecraft.network.protocol.game.ClientboundCommandsPacket.STREAM_CODEC,
                    new FriendlyByteBuf(Unpooled.buffer()), new net.minecraft.network.protocol.game.ClientboundCommandsPacket(root)));
        } catch (Throwable ex) {
            LOG.warn("command measurement failed: {}", ex.toString());
        }

        // advancements
        try {
            var adv = new ArrayList<>(server.getAdvancements().getAllAdvancements());
            rows.add(measure("UpdateAdvancements (" + adv.size() + ")", (StreamCodec) ClientboundUpdateAdvancementsPacket.STREAM_CODEC, rbuf(access),
                    new ClientboundUpdateAdvancementsPacket(true, adv, Set.of(), Map.of())));
        } catch (Throwable ex) {
            LOG.warn("advancement measurement failed: {}", ex.toString());
        }

        rows.sort(Comparator.comparingInt(Row::packed).reversed());
        StringBuilder sb = new StringBuilder("JOIN SIZE TEST: join-time payloads, largest compressed first (frame limit " + FRAME_LIMIT + ")");
        long total = 0;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            total += r.packed();
            if (i < 30 || r.packed() > WARN) {
                sb.append("\n  ").append(r.packed() > FRAME_LIMIT ? "OVER LIMIT " : r.packed() > WARN ? "over 1 MiB " : "").append(r.name())
                        .append(": raw ").append(r.raw()).append(", compressed ").append(r.packed());
            }
        }
        sb.append("\n  total compressed: ").append(total).append(detail);
        LOG.info(sb.toString());
    }

    /** One-line warning when a single sync payload cannot fit the vanilla frame (clients would fail with "length wider than 21-bit"). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void warnIfOversized(MinecraftServer server) {
        try {
            RegistryAccess access = server.registryAccess();
            var buf = rbuf(access);
            Row r = measure("recipe", (StreamCodec) ClientboundUpdateRecipesPacket.STREAM_CODEC, buf,
                    new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes()));
            report("recipe sync", r.packed());
            var tags = TagNetworkSerialization.serializeTagsToNetwork(server.registries());
            Row t = measure("tags", (StreamCodec) ClientboundUpdateTagsPacket.STREAM_CODEC, new FriendlyByteBuf(Unpooled.buffer()), new ClientboundUpdateTagsPacket(tags));
            report("tag sync", t.packed());
        } catch (Throwable ignored) {
            // diagnostics only
        }
    }

    private static void report(String what, int packed) {
        if (packed > FRAME_LIMIT) {
            LOG.error("Legacy Connector: the {} packet is {} KiB compressed, over the {} KiB network frame limit; clients will fail to join with"
                    + " 'length wider than 21-bit' (see the wiki Troubleshooting page)", what, packed / 1024, FRAME_LIMIT / 1024);
        } else if (packed > FRAME_LIMIT * 3 / 4) {
            LOG.warn("Legacy Connector: the {} packet is {} KiB compressed, close to the {} KiB network frame limit", what, packed / 1024, FRAME_LIMIT / 1024);
        }
    }
}
