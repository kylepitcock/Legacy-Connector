package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import org.kylepitcock.legacyconnector.core.synth.Synth;

/**
 * World methods whose signatures name types only the synthesizer provides (particle types, ray trace results), answered with their
 * real behaviour through {@link Synth#addInstanceMethod}: particles become modern particle options (client worlds draw them;
 * {@code WorldServer.spawnParticle} sends them to the players), block ray traces use the modern clip.
 */
public final class WorldApis1122 {
    private WorldApis1122() {}

    /** 1.12 particle constants by name, with their numeric ids (the index). */
    private static final String[] PARTICLES = {"EXPLOSION_NORMAL", "EXPLOSION_LARGE", "EXPLOSION_HUGE", "FIREWORKS_SPARK", "WATER_BUBBLE",
            "WATER_SPLASH", "WATER_WAKE", "SUSPENDED", "SUSPENDED_DEPTH", "CRIT", "CRIT_MAGIC", "SMOKE_NORMAL", "SMOKE_LARGE", "SPELL", "SPELL_INSTANT",
            "SPELL_MOB", "SPELL_MOB_AMBIENT", "SPELL_WITCH", "DRIP_WATER", "DRIP_LAVA", "VILLAGER_ANGRY", "VILLAGER_HAPPY", "TOWN_AURA", "NOTE", "PORTAL",
            "ENCHANTMENT_TABLE", "FLAME", "LAVA", "FOOTSTEP", "CLOUD", "REDSTONE", "SNOWBALL", "SNOW_SHOVEL", "SLIME", "HEART", "BARRIER", "ITEM_CRACK",
            "BLOCK_CRACK", "BLOCK_DUST", "WATER_DROP", "ITEM_TAKE", "MOB_APPEARANCE", "DRAGON_BREATH", "END_ROD", "DAMAGE_INDICATOR", "SWEEP_ATTACK",
            "FALLING_DUST", "TOTEM", "SPIT"};

    private static final Map<String, ParticleOptions> SIMPLE = new HashMap<>();

    static {
        SIMPLE.put("EXPLOSION_NORMAL", ParticleTypes.POOF);
        SIMPLE.put("EXPLOSION_LARGE", ParticleTypes.EXPLOSION);
        SIMPLE.put("EXPLOSION_HUGE", ParticleTypes.EXPLOSION_EMITTER);
        SIMPLE.put("FIREWORKS_SPARK", ParticleTypes.FIREWORK);
        SIMPLE.put("WATER_BUBBLE", ParticleTypes.BUBBLE);
        SIMPLE.put("WATER_SPLASH", ParticleTypes.SPLASH);
        SIMPLE.put("WATER_WAKE", ParticleTypes.FISHING);
        SIMPLE.put("SUSPENDED", ParticleTypes.UNDERWATER);
        SIMPLE.put("SUSPENDED_DEPTH", ParticleTypes.UNDERWATER);
        SIMPLE.put("CRIT", ParticleTypes.CRIT);
        SIMPLE.put("CRIT_MAGIC", ParticleTypes.ENCHANTED_HIT);
        SIMPLE.put("SMOKE_NORMAL", ParticleTypes.SMOKE);
        SIMPLE.put("SMOKE_LARGE", ParticleTypes.LARGE_SMOKE);
        SIMPLE.put("SPELL", ParticleTypes.EFFECT);
        SIMPLE.put("SPELL_INSTANT", ParticleTypes.INSTANT_EFFECT);
        SIMPLE.put("SPELL_WITCH", ParticleTypes.WITCH);
        SIMPLE.put("DRIP_WATER", ParticleTypes.DRIPPING_WATER);
        SIMPLE.put("DRIP_LAVA", ParticleTypes.DRIPPING_LAVA);
        SIMPLE.put("VILLAGER_ANGRY", ParticleTypes.ANGRY_VILLAGER);
        SIMPLE.put("VILLAGER_HAPPY", ParticleTypes.HAPPY_VILLAGER);
        SIMPLE.put("TOWN_AURA", ParticleTypes.MYCELIUM);
        SIMPLE.put("NOTE", ParticleTypes.NOTE);
        SIMPLE.put("PORTAL", ParticleTypes.PORTAL);
        SIMPLE.put("ENCHANTMENT_TABLE", ParticleTypes.ENCHANT);
        SIMPLE.put("FLAME", ParticleTypes.FLAME);
        SIMPLE.put("LAVA", ParticleTypes.LAVA);
        SIMPLE.put("CLOUD", ParticleTypes.CLOUD);
        SIMPLE.put("SNOWBALL", ParticleTypes.ITEM_SNOWBALL);
        SIMPLE.put("SNOW_SHOVEL", ParticleTypes.ITEM_SNOWBALL);
        SIMPLE.put("SLIME", ParticleTypes.ITEM_SLIME);
        SIMPLE.put("HEART", ParticleTypes.HEART);
        SIMPLE.put("WATER_DROP", ParticleTypes.RAIN);
        SIMPLE.put("MOB_APPEARANCE", ParticleTypes.ELDER_GUARDIAN);
        SIMPLE.put("DRAGON_BREATH", ParticleTypes.DRAGON_BREATH);
        SIMPLE.put("END_ROD", ParticleTypes.END_ROD);
        SIMPLE.put("DAMAGE_INDICATOR", ParticleTypes.DAMAGE_INDICATOR);
        SIMPLE.put("SWEEP_ATTACK", ParticleTypes.SWEEP_ATTACK);
        SIMPLE.put("TOTEM", ParticleTypes.TOTEM_OF_UNDYING);
        SIMPLE.put("SPIT", ParticleTypes.SPIT);
    }

    private static String particleName(Object type) {
        if (type instanceof Enum<?> e) return e.name();
        if (type instanceof Integer id && id >= 0 && id < PARTICLES.length) return PARTICLES[id];
        return null;
    }

    private static net.minecraft.world.level.block.state.BlockState stateOf(int[] args) {
        if (args == null || args.length == 0) return net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        try {
            return StatesBridge1122.toModern(legacyconnector.v1_12_2.net.minecraft.block.Block.getStateById(args[0]));
        } catch (RuntimeException | LinkageError e) {
            return net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        }
    }

    /**
     * The modern particle of a 1.12 one. Coloured particles (redstone dust, mob spell) took their colour from the speed arguments:
     * {@code colorOut} is set when the speeds were a colour (the particle then has no speed).
     */
    static ParticleOptions particle(Object type, double vx, double vy, double vz, int[] args, boolean[] colorOut) {
        String name = particleName(type);
        if (name == null) return null;
        ParticleOptions simple = SIMPLE.get(name);
        if (simple != null) return simple;
        switch (name) {
            case "REDSTONE" -> {
                colorOut[0] = true;
                float r = (float) vx;
                if (r == 0.0F) r = 1.0F;
                return new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(r, (float) vy, (float) vz), 1.0F);
            }
            case "SPELL_MOB", "SPELL_MOB_AMBIENT" -> {
                colorOut[0] = true;
                return net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, (float) vx, (float) vy, (float) vz);
            }
            case "BLOCK_CRACK", "BLOCK_DUST" -> {
                return new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, stateOf(args));
            }
            case "FALLING_DUST" -> {
                return new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.FALLING_DUST, stateOf(args));
            }
            case "BARRIER" -> {
                return new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK_MARKER, net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState());
            }
            case "ITEM_CRACK" -> {
                net.minecraft.world.item.ItemStack stack = net.minecraft.world.item.ItemStack.EMPTY;
                try {
                    var item = args != null && args.length > 0 ? legacyconnector.v1_12_2.net.minecraft.item.Item.getItemById(args[0]) : null;
                    if (item != null) stack = Stacks1122.toModern(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, 1, args.length > 1 ? args[1] : 0));
                } catch (RuntimeException | LinkageError e) {
                    // a plain stone particle
                }
                if (stack.isEmpty()) stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE);
                return new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, stack);
            }
            default -> {
                return null;
            }
        }
    }

    private static int particleFailures;

    private static void spawnClient(World world, Object type, boolean force, double x, double y, double z, double vx, double vy, double vz, int[] args) {
        var level = world.mcc$liveLevel();
        if (level == null || !level.isClientSide()) return; // a server world only draws particles through its listeners: none
        boolean[] color = new boolean[1];
        ParticleOptions p;
        try {
            p = particle(type, vx, vy, vz, args, color);
        } catch (RuntimeException | LinkageError e) {
            if (particleFailures++ < 3) org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2").warn("Particle {} failed: {}", type, e.toString());
            return;
        }
        if (p == null) return;
        if (color[0]) level.addParticle(p, force, x, y, z, 0.0D, 0.0D, 0.0D);
        else level.addParticle(p, force, x, y, z, vx, vy, vz);
    }

    private static void spawnServer(World world, Object type, boolean longDistance, double x, double y, double z, int count, double xo, double yo,
            double zo, double speed, int[] args) {
        if (!(world.mcc$liveLevel() instanceof ServerLevel level)) {
            spawnClient(world, type, longDistance, x, y, z, xo, yo, zo, args);
            return;
        }
        boolean[] color = new boolean[1];
        ParticleOptions p = particle(type, xo, yo, zo, args, color);
        if (p == null) return;
        if (color[0] && count == 0) {
            xo = yo = zo = 0.0D;
        }
        for (var player : level.players()) level.sendParticles(player, p, longDistance, x, y, z, count, xo, yo, zo, speed);
    }

    private static int[] ints(Object o) {
        return o instanceof int[] a ? a : new int[0];
    }

    private static double d(Object o) {
        return Synth.toDouble(o);
    }

    // ------------------------------------------------------------------ ray traces

    private static Class<?> rayTraceClass;
    private static final Map<Object, BlockPos> TRACE_POS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static Class<?> rayTraceType() {
        if (rayTraceClass != null) return rayTraceClass;
        try {
            rayTraceClass = Class.forName("legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult", false, WorldApis1122.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            rayTraceClass = RuntimeApis1122.synthClass("net.minecraft.util.math.RayTraceResult");
        }
        return rayTraceClass;
    }

    /** A block hit as a 1.12 RayTraceResult (a hand-written one through its constructor, a synthesized one with its fields set). */
    static Object blockHit(Vec3d hit, EnumFacing side, BlockPos pos) {
        Class<?> type = rayTraceType();
        if (type == null) return null;
        try {
            var ctor = type.getConstructor(Vec3d.class, EnumFacing.class, BlockPos.class);
            if (!type.getName().startsWith("legacyconnector_synth.")) return ctor.newInstance(hit, side, pos);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // a synthesized class: fill a blank one
        }
        Object r = RuntimeApis1122.blank(type);
        if (r == null) return null;
        setField(r, "hitVec", hit);
        setField(r, "sideHit", side);
        setField(r, "blockPos", pos);
        try {
            var typeField = type.getField("typeOfHit");
            setField(r, "typeOfHit", Vanilla1122.enumConstant(typeField.getType(), "BLOCK"));
        } catch (NoSuchFieldException e) {
            // no mod reads it
        }
        TRACE_POS.put(r, pos);
        return r;
    }

    private static void setField(Object target, String name, Object value) {
        for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                var f = c.getDeclaredField(name);
                f.setAccessible(true);
                if (value == null || f.getType().isInstance(value)) f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                // the superclass
            } catch (ReflectiveOperationException | RuntimeException e) {
                return;
            }
        }
    }

    /**
     * 1.12's block ray trace: the first block (by its collision shape, or its outline when blocks without a collision box count)
     * the segment hits, liquids only when asked; null on a miss.
     */
    static Object rayTrace(World world, Vec3d start, Vec3d end, boolean liquids, boolean ignoreNoBox, boolean returnLast) {
        var access = world.mcc$access();
        if (access == null || start == null || end == null) return null;
        var hit = access.clip(new net.minecraft.world.level.ClipContext(start.mcc$modern(), end.mcc$modern(),
                ignoreNoBox ? net.minecraft.world.level.ClipContext.Block.COLLIDER : net.minecraft.world.level.ClipContext.Block.OUTLINE,
                liquids ? net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY : net.minecraft.world.level.ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return null;
        return blockHit(Vec3d.mcc$of(hit.getLocation()), EnumFacing.mcc$of(hit.getDirection()), BlockPos.mcc$of(hit.getBlockPos()));
    }

    public static void register() {
        for (String owner : new String[] {"net.minecraft.world.World", "net.minecraft.world.WorldServer", "net.minecraft.client.multiplayer.WorldClient"}) {
            Synth.addInstanceMethod(owner + ".spawnParticle", (self, a) -> {
                if (!(self instanceof World w)) return Synth.NONE;
                if (a.length == 8) spawnClient(w, a[0], false, d(a[1]), d(a[2]), d(a[3]), d(a[4]), d(a[5]), d(a[6]), ints(a[7]));
                else if (a.length == 9 && a[1] instanceof Boolean b) spawnClient(w, a[0], b, d(a[2]), d(a[3]), d(a[4]), d(a[5]), d(a[6]), d(a[7]), ints(a[8]));
                else if (a.length == 10) spawnServer(w, a[0], false, d(a[1]), d(a[2]), d(a[3]), Synth.toInt(a[4]), d(a[5]), d(a[6]), d(a[7]), d(a[8]), ints(a[9]));
                else if (a.length == 11) spawnServer(w, a[0], Synth.toBoolean(a[1]), d(a[2]), d(a[3]), d(a[4]), Synth.toInt(a[5]), d(a[6]), d(a[7]), d(a[8]), d(a[9]),
                        ints(a[10]));
                else if (a.length == 12 && a[0] instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player) {
                    // spawnParticle(EntityPlayerMP, type, longDistance, x, y, z, count, xo, yo, zo, speed, args): to one player
                    if (!(w.mcc$liveLevel() instanceof ServerLevel level) || player.mcc$serverPlayer() == null) return null;
                    boolean[] color = new boolean[1];
                    ParticleOptions p = particle(a[1], d(a[7]), d(a[8]), d(a[9]), ints(a[11]), color);
                    if (p != null) level.sendParticles(player.mcc$serverPlayer(), p, Synth.toBoolean(a[2]), d(a[3]), d(a[4]), d(a[5]), Synth.toInt(a[6]),
                            d(a[7]), d(a[8]), d(a[9]), d(a[10]));
                }
                return null;
            });
            Synth.addInstanceMethod(owner + ".spawnAlwaysVisibleParticle", (self, a) -> {
                if (self instanceof World w && a.length == 8) spawnClient(w, a[0], true, d(a[1]), d(a[2]), d(a[3]), d(a[4]), d(a[5]), d(a[6]), ints(a[7]));
                return null;
            });
            Synth.addInstanceMethod(owner + ".rayTraceBlocks", (self, a) -> {
                if (!(self instanceof World w) || a.length < 2 || !(a[0] instanceof Vec3d s) || !(a[1] instanceof Vec3d e)) return Synth.NONE;
                boolean liquids = a.length > 2 && Synth.toBoolean(a[2]);
                boolean ignoreNoBox = a.length > 3 && Synth.toBoolean(a[3]);
                boolean last = a.length > 4 && Synth.toBoolean(a[4]);
                return rayTrace(w, s, e, liquids, ignoreNoBox, last);
            });
        }
        Synth.addInstanceMethod("net.minecraft.util.math.RayTraceResult.getBlockPos", (self, a) -> {
            BlockPos p = TRACE_POS.get(self);
            return p != null ? p : Synth.NONE;
        });
    }
}
