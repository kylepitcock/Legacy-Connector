package legacyconnector.v1_12_2.net.minecraft.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * 1.12.2 particle types (names, ids, argument counts as in vanilla), each mapped to the modern particle it became
 * ({@link #mcc$options}): colours of dust and mob spells come from the legacy velocity arguments, block and item particles
 * from the legacy state/item ids.
 */
public enum EnumParticleTypes {
    EXPLOSION_NORMAL("explode", 0, true),
    EXPLOSION_LARGE("largeexplode", 1, true),
    EXPLOSION_HUGE("hugeexplosion", 2, true),
    FIREWORKS_SPARK("fireworksSpark", 3, false),
    WATER_BUBBLE("bubble", 4, false),
    WATER_SPLASH("splash", 5, false),
    WATER_WAKE("wake", 6, false),
    SUSPENDED("suspended", 7, false),
    SUSPENDED_DEPTH("depthsuspend", 8, false),
    CRIT("crit", 9, false),
    CRIT_MAGIC("magicCrit", 10, false),
    SMOKE_NORMAL("smoke", 11, false),
    SMOKE_LARGE("largesmoke", 12, false),
    SPELL("spell", 13, false),
    SPELL_INSTANT("instantSpell", 14, false),
    SPELL_MOB("mobSpell", 15, false),
    SPELL_MOB_AMBIENT("mobSpellAmbient", 16, false),
    SPELL_WITCH("witchMagic", 17, false),
    DRIP_WATER("dripWater", 18, false),
    DRIP_LAVA("dripLava", 19, false),
    VILLAGER_ANGRY("angryVillager", 20, false),
    VILLAGER_HAPPY("happyVillager", 21, false),
    TOWN_AURA("townaura", 22, false),
    NOTE("note", 23, false),
    PORTAL("portal", 24, false),
    ENCHANTMENT_TABLE("enchantmenttable", 25, false),
    FLAME("flame", 26, false),
    LAVA("lava", 27, false),
    FOOTSTEP("footstep", 28, false),
    CLOUD("cloud", 29, false),
    REDSTONE("reddust", 30, false),
    SNOWBALL("snowballpoof", 31, false),
    SNOW_SHOVEL("snowshovel", 32, false),
    SLIME("slime", 33, false),
    HEART("heart", 34, false),
    BARRIER("barrier", 35, false),
    ITEM_CRACK("iconcrack", 36, false, 2),
    BLOCK_CRACK("blockcrack", 37, false, 1),
    BLOCK_DUST("blockdust", 38, false, 1),
    WATER_DROP("droplet", 39, false),
    ITEM_TAKE("take", 40, false),
    MOB_APPEARANCE("mobappearance", 41, true),
    DRAGON_BREATH("dragonbreath", 42, false),
    END_ROD("endRod", 43, false),
    DAMAGE_INDICATOR("damageIndicator", 44, true),
    SWEEP_ATTACK("sweepAttack", 45, true),
    FALLING_DUST("fallingdust", 46, false, 1),
    TOTEM("totem", 47, false),
    SPIT("spit", 48, true);

    private final String particleName;
    private final int particleID;
    private final boolean shouldIgnoreRange;
    private final int argumentCount;
    private static final Map<Integer, EnumParticleTypes> PARTICLES = new HashMap<>();
    private static final Map<String, EnumParticleTypes> BY_NAME = new HashMap<>();

    EnumParticleTypes(String name, int id, boolean ignoreRange, int argumentCount) {
        this.particleName = name;
        this.particleID = id;
        this.shouldIgnoreRange = ignoreRange;
        this.argumentCount = argumentCount;
    }

    EnumParticleTypes(String name, int id, boolean ignoreRange) {
        this(name, id, ignoreRange, 0);
    }

    public static java.util.Set<String> getParticleNames() {
        return BY_NAME.keySet();
    }

    public String getParticleName() {
        return particleName;
    }

    public int getParticleID() {
        return particleID;
    }

    public int getArgumentCount() {
        return argumentCount;
    }

    public boolean getShouldIgnoreRange() {
        return shouldIgnoreRange;
    }

    public static EnumParticleTypes getParticleFromId(int id) {
        return PARTICLES.get(id);
    }

    public static EnumParticleTypes getByName(String name) {
        return BY_NAME.get(name);
    }

    static {
        for (EnumParticleTypes t : values()) {
            PARTICLES.put(t.particleID, t);
            BY_NAME.put(t.particleName, t);
        }
    }

    /**
     * The modern particle for a legacy spawn, or null when it has none (footstep, item pickup). {@code velocity} may be
     * adjusted in place: dust and mob spell colours were passed as the velocity, which the modern particle does not use.
     */
    public ParticleOptions mcc$options(double[] velocity, int... args) {
        try {
            return switch (this) {
                case EXPLOSION_NORMAL -> ParticleTypes.POOF;
                case EXPLOSION_LARGE -> ParticleTypes.EXPLOSION;
                case EXPLOSION_HUGE -> ParticleTypes.EXPLOSION_EMITTER;
                case FIREWORKS_SPARK -> ParticleTypes.FIREWORK;
                case WATER_BUBBLE -> ParticleTypes.BUBBLE;
                case WATER_SPLASH -> ParticleTypes.SPLASH;
                case WATER_WAKE -> ParticleTypes.FISHING;
                case SUSPENDED -> ParticleTypes.UNDERWATER;
                case SUSPENDED_DEPTH, TOWN_AURA -> ParticleTypes.MYCELIUM;
                case CRIT -> ParticleTypes.CRIT;
                case CRIT_MAGIC -> ParticleTypes.ENCHANTED_HIT;
                case SMOKE_NORMAL -> ParticleTypes.SMOKE;
                case SMOKE_LARGE -> ParticleTypes.LARGE_SMOKE;
                case SPELL -> ParticleTypes.EFFECT;
                case SPELL_INSTANT -> ParticleTypes.INSTANT_EFFECT;
                case SPELL_MOB, SPELL_MOB_AMBIENT -> {
                    float r = (float) velocity[0], g = (float) velocity[1], b = (float) velocity[2];
                    velocity[0] = velocity[1] = velocity[2] = 0;
                    yield net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, this == SPELL_MOB_AMBIENT ? 0.15F : 1.0F,
                            r, g, b);
                }
                case SPELL_WITCH -> ParticleTypes.WITCH;
                case DRIP_WATER -> ParticleTypes.DRIPPING_WATER;
                case DRIP_LAVA -> ParticleTypes.DRIPPING_LAVA;
                case VILLAGER_ANGRY -> ParticleTypes.ANGRY_VILLAGER;
                case VILLAGER_HAPPY -> ParticleTypes.HAPPY_VILLAGER;
                case NOTE -> ParticleTypes.NOTE;
                case PORTAL -> ParticleTypes.PORTAL;
                case ENCHANTMENT_TABLE -> ParticleTypes.ENCHANT;
                case FLAME -> ParticleTypes.FLAME;
                case LAVA -> ParticleTypes.LAVA;
                case CLOUD -> ParticleTypes.CLOUD;
                case REDSTONE -> {
                    // 1.12 reddust: the velocity is the colour (red when 0)
                    float r = (float) velocity[0], g = (float) velocity[1], b = (float) velocity[2];
                    if (r == 0.0F) r = 1.0F;
                    velocity[0] = velocity[1] = velocity[2] = 0;
                    yield new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(clamp(r), clamp(g), clamp(b)), 1.0F);
                }
                case SNOWBALL, SNOW_SHOVEL -> ParticleTypes.ITEM_SNOWBALL;
                case SLIME -> ParticleTypes.ITEM_SLIME;
                case HEART -> ParticleTypes.HEART;
                case BARRIER -> new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK_MARKER,
                        net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState());
                case ITEM_CRACK -> {
                    var item = legacyconnector.v1_12_2.net.minecraft.item.Item.getItemById(args.length > 0 ? args[0] : 0);
                    var stack = item == null ? net.minecraft.world.item.ItemStack.EMPTY
                            : org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, 1,
                                    args.length > 1 ? args[1] : 0));
                    yield stack.isEmpty() ? ParticleTypes.POOF : new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, stack);
                }
                case BLOCK_CRACK, BLOCK_DUST, FALLING_DUST -> {
                    var state = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(
                            legacyconnector.v1_12_2.net.minecraft.block.Block.getStateById(args.length > 0 ? args[0] : 0));
                    if (state.isAir()) yield null;
                    yield new net.minecraft.core.particles.BlockParticleOption(this == FALLING_DUST ? ParticleTypes.FALLING_DUST : ParticleTypes.BLOCK, state);
                }
                case WATER_DROP -> ParticleTypes.RAIN;
                case MOB_APPEARANCE -> ParticleTypes.ELDER_GUARDIAN;
                case DRAGON_BREATH -> ParticleTypes.DRAGON_BREATH;
                case END_ROD -> ParticleTypes.END_ROD;
                case DAMAGE_INDICATOR -> ParticleTypes.DAMAGE_INDICATOR;
                case SWEEP_ATTACK -> ParticleTypes.SWEEP_ATTACK;
                case TOTEM -> ParticleTypes.TOTEM_OF_UNDYING;
                case SPIT -> ParticleTypes.SPIT;
                case FOOTSTEP, ITEM_TAKE -> null;
            };
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }
}
