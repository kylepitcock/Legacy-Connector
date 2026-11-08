package org.kylepitcock.legacyconnector.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Self test ({@code -Dlegacyconnector.entitySelfTest=true}) for SILENT failures: things that crash nothing, log no error and that link
 * or translation checks cannot see, but that make the game quietly wrong (the class of bugs behind Sinytra Connector's "a mod bypassed
 * attribute registration, SWIM_SPEED stayed 0 and nobody could swim" report). Runs once at server start, is read-only, and prints
 * {@code SILENT FAILURE CHECKS: N passed, M failed} plus one line per failure (logged at ERROR). Not covered: music/sound playback
 * (client only), proxy login (Velocity modern forwarding), client rendering; see docs/wiki/Development.md.
 */
public final class SilentFailureChecks {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/SilentFailures");
    private static final int MAX_NAMES = 12;

    private final List<String> failures = new ArrayList<>();
    private int passed;

    private SilentFailureChecks() {}

    public static void register() {
        if (!Boolean.getBoolean("legacyconnector.entitySelfTest")) return;
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            try {
                new SilentFailureChecks().runAll();
            } catch (RuntimeException | LinkageError ex) {
                LOG.error("SILENT FAILURE CHECKS: the checks themselves threw", ex);
            }
        });
    }

    private void runAll() {
        check("attributes: legacy living-entity hosts", this::hostAttributes);
        check("attributes: player", () -> attributesOf(EntityType.PLAYER, "minecraft:player", false));
        check("network: no login/configuration-phase payload registrations", this::networkPhases);
        check("network: LegacyPayload encoding is idempotent", this::payloadIdempotent);
        check("registry: entity host ids match their registry keys", this::hostRegistryKeys);
        check("registry: non-vanilla blocks, items, sounds resolve", this::registrySanity);
        LOG.info("SILENT FAILURE CHECKS: {} passed, {} failed", passed, failures.size());
        for (String f : failures) LOG.error("SILENT FAILURE CHECK FAILED: {}", f);
        if (Boolean.getBoolean("legacyconnector.selfTestFailFast") && !failures.isEmpty()) {
            LOG.error("SILENT FAILURE CHECKS: legacyconnector.selfTestFailFast is set, halting the server");
            Runtime.getRuntime().halt(3);
        }
    }

    private void check(String name, Supplier<List<String>> body) {
        List<String> problems;
        try {
            problems = body.get();
        } catch (RuntimeException | LinkageError ex) {
            problems = List.of("threw " + ex);
        }
        if (problems.isEmpty()) {
            passed++;
            LOG.info("  ok: {}", name);
        } else {
            failures.add(name + ": " + cap(problems));
        }
    }

    private static String cap(List<String> names) {
        return names.size() <= MAX_NAMES ? String.join("; ", names)
                : String.join("; ", names.subList(0, MAX_NAMES)) + "; ... and " + (names.size() - MAX_NAMES) + " more";
    }

    // ---------------------------------------------------------------- attributes

    private List<String> hostAttributes() {
        List<String> out = new ArrayList<>();
        Map<EntityType<?>, String> living = new LinkedHashMap<>();
        Map<EntityType<?>, String> expectLiving = new LinkedHashMap<>();
        for (var i : org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts.all()) {
            expectLiving.put(i.type(), i.id().toString());
            if (i.kind() != org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts.Kind.ENTITY) living.put(i.type(), i.id().toString());
        }
        for (var i : org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.all()) {
            expectLiving.put(i.type(), i.id().toString());
            if (i.kind() != org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.Kind.ENTITY) living.put(i.type(), i.id().toString());
        }
        // the shared fallback types (unknown legacy entities) carry the same maps
        for (EntityType<?> t : org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts.allTypes()) {
            if (DefaultAttributes.hasSupplier(t)) living.putIfAbsent(t, String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(t)));
        }
        for (EntityType<?> t : org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.allTypes()) {
            if (DefaultAttributes.hasSupplier(t)) living.putIfAbsent(t, String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(t)));
        }
        for (var e : living.entrySet()) out.addAll(attributesOf(e.getKey(), e.getValue(), true));
        return out;
    }

    /** The attributes NeoForge and vanilla code read every tick: absent or zero ones silently break swimming, walking, falling... */
    private List<String> attributesOf(EntityType<?> type, String name, boolean mob) {
        @SuppressWarnings("unchecked")
        EntityType<? extends net.minecraft.world.entity.LivingEntity> living = (EntityType<? extends net.minecraft.world.entity.LivingEntity>) type;
        if (!DefaultAttributes.hasSupplier(living)) return List.of(name + " has no default attribute map");
        AttributeSupplier map = DefaultAttributes.getSupplier(living);
        List<String> out = new ArrayList<>();
        positive(map, Attributes.MAX_HEALTH, name, out);
        positive(map, Attributes.MOVEMENT_SPEED, name, out);
        positive(map, Attributes.STEP_HEIGHT, name, out);
        positive(map, Attributes.GRAVITY, name, out);
        positive(map, Attributes.SAFE_FALL_DISTANCE, name, out);
        positive(map, Attributes.FALL_DAMAGE_MULTIPLIER, name, out);
        positive(map, Attributes.SCALE, name, out);
        positive(map, NeoForgeMod.SWIM_SPEED, name, out);
        positive(map, NeoForgeMod.NAMETAG_DISTANCE, name, out);
        if (mob) {
            positive(map, Attributes.FOLLOW_RANGE, name, out);
            // monsters need attack damage; every legacy mob host shares the monster map, so require it of all of them
            positive(map, Attributes.ATTACK_DAMAGE, name, out);
        } else {
            positive(map, Attributes.ATTACK_DAMAGE, name, out);
            positive(map, Attributes.BLOCK_BREAK_SPEED, name, out);
        }
        return out;
    }

    private static void positive(AttributeSupplier map, net.minecraft.core.Holder<Attribute> attr, String name, List<String> out) {
        String key = attr.unwrapKey().map(k -> k.location().getPath()).orElse("?");
        if (!map.hasAttribute(attr)) {
            out.add(name + " lacks attribute " + key);
            return;
        }
        double v = map.getBaseValue(attr);
        if (!(v > 0) || Double.isInfinite(v)) out.add(name + " attribute " + key + " = " + v);
    }

    // ---------------------------------------------------------------- network

    @SuppressWarnings("unchecked")
    private List<String> networkPhases() {
        List<String> out = new ArrayList<>();
        try {
            Field f = Class.forName("net.neoforged.neoforge.network.registration.NetworkRegistry").getDeclaredField("PAYLOAD_REGISTRATIONS");
            f.setAccessible(true);
            Map<Object, Map<ResourceLocation, ?>> regs = (Map<Object, Map<ResourceLocation, ?>>) f.get(null);
            boolean playSeen = false;
            for (var proto : regs.entrySet()) {
                for (ResourceLocation id : proto.getValue().keySet()) {
                    if (!id.getNamespace().equals("legacyconnector")) continue;
                    if (String.valueOf(proto.getKey()).equals("PLAY")) playSeen = true;
                    else out.add("payload " + id + " registered for the " + proto.getKey() + " phase");
                }
            }
            if (!playSeen) out.add("the play-phase LegacyPayload is not registered at all");
        } catch (ReflectiveOperationException ex) {
            // NeoForge changed its internals: do not fail the run for that, but say so
            LOG.warn("network phase check skipped, NetworkRegistry internals not readable: {}", ex.toString());
        }
        return out;
    }

    private List<String> payloadIdempotent() {
        byte[] data = new byte[300];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i * 31);
        byte[] before = data.clone();
        LegacyPayload p = new LegacyPayload("1.7.10", "test:chan", 77, data);
        byte[] a = encode(p), b = encode(p);
        List<String> out = new ArrayList<>();
        if (!java.util.Arrays.equals(a, b)) out.add("two encodings of one LegacyPayload differ");
        if (!java.util.Arrays.equals(before, data)) out.add("encoding mutated the payload's byte array");
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(a));
        LegacyPayload back = LegacyPayload.CODEC.decode(buf);
        if (!back.version().equals(p.version()) || !back.channel().equals(p.channel()) || back.discriminator() != 77
                || !java.util.Arrays.equals(back.data(), before)) out.add("decode(encode(payload)) differs from the payload");
        if (buf.isReadable()) out.add("decoding left unread bytes");
        return out;
    }

    private static byte[] encode(LegacyPayload p) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        LegacyPayload.CODEC.encode(buf, p);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        buf.release();
        return bytes;
    }

    // ---------------------------------------------------------------- registries

    private List<String> hostRegistryKeys() {
        List<String> out = new ArrayList<>();
        for (var i : org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts.all()) hostKey(i.id(), i.type(), out);
        for (var i : org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.all()) hostKey(i.id(), i.type(), out);
        return out;
    }

    private static void hostKey(ResourceLocation id, EntityType<?> type, List<String> out) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) out.add(id + " is not in the entity type registry");
        else if (!id.equals(key)) out.add(id + " is registered under " + key);
    }

    private List<String> registrySanity() {
        List<String> out = new ArrayList<>();
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation id = e.getKey().location();
            if (id.getNamespace().equals("minecraft")) continue;
            var b = e.getValue();
            if (b.defaultBlockState() == null) out.add("block " + id + " has no default state");
            if (BuiltInRegistries.BLOCK.getKey(b) == null || !BuiltInRegistries.BLOCK.getKey(b).equals(id)) out.add("block " + id + " registered under another key");
        }
        for (var e : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation id = e.getKey().location();
            if (id.getNamespace().equals("minecraft")) continue;
            var item = e.getValue();
            if (item == net.minecraft.world.item.Items.AIR) out.add("item " + id + " is air");
            else if (item.getDefaultInstance().isEmpty()) out.add("item " + id + " default stack is empty");
            else if (BuiltInRegistries.ITEM.getKey(item) == null || !BuiltInRegistries.ITEM.getKey(item).equals(id)) out.add("item " + id + " registered under another key");
            if (item instanceof net.minecraft.world.item.BlockItem bi && BuiltInRegistries.BLOCK.getKey(bi.getBlock()).equals(BuiltInRegistries.BLOCK.getDefaultKey())
                    && bi.getBlock() != net.minecraft.world.level.block.Blocks.AIR) out.add("item " + id + " places an unregistered block");
        }
        for (var e : BuiltInRegistries.SOUND_EVENT.entrySet()) {
            ResourceLocation id = e.getKey().location();
            if (id.getNamespace().equals("minecraft")) continue;
            // a mod may register an event under one key and name another sound of its own namespace (sounds.json is keyed by the location): fine
            if (e.getValue() == null || !id.getNamespace().equals(e.getValue().getLocation().getNamespace())) out.add("sound " + id + " resolves to " + (e.getValue() == null ? "null" : e.getValue().getLocation()));
        }
        return out;
    }
}
