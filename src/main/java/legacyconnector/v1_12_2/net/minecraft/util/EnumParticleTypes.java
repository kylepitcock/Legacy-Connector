package legacyconnector.v1_12_2.net.minecraft.util;

import java.util.Map;
import java.util.Set;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * 1.12.2's particle types: names, network ids and argument counts as in 1.12.2. {@link #mcc$modern} gives the modern particle each
 * one became (block and item particles take their legacy arguments: a block state id, or an item id and metadata; coloured
 * particles their colour from the speed arguments, as 1.12's renderer read them).
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
    private static final Map<Integer, EnumParticleTypes> PARTICLES = new java.util.HashMap<>();
    private static final Map<String, EnumParticleTypes> BY_NAME = new java.util.LinkedHashMap<>();

    EnumParticleTypes(String name, int id, boolean ignoreRange, int argumentCount) {
        this.particleName = name;
        this.particleID = id;
        this.shouldIgnoreRange = ignoreRange;
        this.argumentCount = argumentCount;
    }

    EnumParticleTypes(String name, int id, boolean ignoreRange) {
        this(name, id, ignoreRange, 0);
    }

    public static Set<String> getParticleNames() {
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

    public static EnumParticleTypes getParticleFromId(int particleId) {
        return PARTICLES.get(particleId);
    }

    public static EnumParticleTypes getByName(String name) {
        return BY_NAME.get(name);
    }

    static {
        for (EnumParticleTypes t : values()) {
            PARTICLES.put(t.getParticleID(), t);
            BY_NAME.put(t.getParticleName(), t);
        }
    }

    /**
     * The modern particle for this type, or null when it has none ({@code footstep} and {@code take} are gone). {@code args} are
     * the legacy particle arguments; the speeds are the ones passed to {@code spawnParticle} (colour of {@code reddust} and the
     * mob spell particles).
     */
    public ParticleOptions mcc$modern(double xSpeed, double ySpeed, double zSpeed, int... args) {
        return switch (this) {
            case EXPLOSION_NORMAL -> ParticleTypes.POOF;
            case EXPLOSION_LARGE -> ParticleTypes.EXPLOSION;
            case EXPLOSION_HUGE -> ParticleTypes.EXPLOSION_EMITTER;
            case FIREWORKS_SPARK -> ParticleTypes.FIREWORK;
            case WATER_BUBBLE -> ParticleTypes.BUBBLE;
            case WATER_SPLASH -> ParticleTypes.SPLASH;
            case WATER_WAKE -> ParticleTypes.FISHING;
            case SUSPENDED, SUSPENDED_DEPTH -> ParticleTypes.UNDERWATER;
            case CRIT -> ParticleTypes.CRIT;
            case CRIT_MAGIC -> ParticleTypes.ENCHANTED_HIT;
            case SMOKE_NORMAL -> ParticleTypes.SMOKE;
            case SMOKE_LARGE -> ParticleTypes.LARGE_SMOKE;
            case SPELL -> ParticleTypes.EFFECT;
            case SPELL_INSTANT -> ParticleTypes.INSTANT_EFFECT;
            case SPELL_MOB, SPELL_MOB_AMBIENT -> net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
                    (this == SPELL_MOB_AMBIENT ? 0x26000000 : 0xFF000000) | (int) (colour(xSpeed) * 255.0F) << 16
                            | (int) (colour(ySpeed) * 255.0F) << 8 | (int) (colour(zSpeed) * 255.0F));
            case SPELL_WITCH -> ParticleTypes.WITCH;
            case DRIP_WATER -> ParticleTypes.DRIPPING_WATER;
            case DRIP_LAVA -> ParticleTypes.DRIPPING_LAVA;
            case VILLAGER_ANGRY -> ParticleTypes.ANGRY_VILLAGER;
            case VILLAGER_HAPPY -> ParticleTypes.HAPPY_VILLAGER;
            case TOWN_AURA -> ParticleTypes.MYCELIUM;
            case NOTE -> ParticleTypes.NOTE;
            case PORTAL -> ParticleTypes.PORTAL;
            case ENCHANTMENT_TABLE -> ParticleTypes.ENCHANT;
            case FLAME -> ParticleTypes.FLAME;
            case LAVA -> ParticleTypes.LAVA;
            case FOOTSTEP, ITEM_TAKE -> null;
            case CLOUD -> ParticleTypes.CLOUD;
            // reddust: a zero red speed is full red, as 1.12's ParticleRedstone read it
            case REDSTONE -> new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(
                    xSpeed == 0.0D ? 1.0F : colour(xSpeed), colour(ySpeed), colour(zSpeed)), 1.0F);
            case SNOWBALL, SNOW_SHOVEL -> ParticleTypes.ITEM_SNOWBALL;
            case SLIME -> ParticleTypes.ITEM_SLIME;
            case HEART -> ParticleTypes.HEART;
            case BARRIER -> new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK_MARKER,
                    net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState());
            case ITEM_CRACK -> new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, itemArgument(args));
            case BLOCK_CRACK, BLOCK_DUST -> new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, blockArgument(args));
            case FALLING_DUST -> new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.FALLING_DUST, blockArgument(args));
            case WATER_DROP -> ParticleTypes.RAIN;
            case MOB_APPEARANCE -> ParticleTypes.ELDER_GUARDIAN;
            case DRAGON_BREATH -> ParticleTypes.DRAGON_BREATH;
            case END_ROD -> ParticleTypes.END_ROD;
            case DAMAGE_INDICATOR -> ParticleTypes.DAMAGE_INDICATOR;
            case SWEEP_ATTACK -> ParticleTypes.SWEEP_ATTACK;
            case TOTEM -> ParticleTypes.TOTEM_OF_UNDYING;
            case SPIT -> ParticleTypes.SPIT;
        };
    }

    /** Whether the speeds of this type are a colour (the modern particle then gets zero speed). */
    public boolean mcc$speedIsColour() {
        return this == REDSTONE || this == SPELL_MOB || this == SPELL_MOB_AMBIENT;
    }

    private static float colour(double v) {
        return (float) Math.max(0.0D, Math.min(1.0D, v));
    }

    private static net.minecraft.world.level.block.state.BlockState blockArgument(int[] args) {
        net.minecraft.world.level.block.state.BlockState state = null;
        if (args != null && args.length > 0) {
            try {
                var legacy = legacyconnector.v1_12_2.net.minecraft.block.Block.getStateById(args[0]);
                if (legacy != null) state = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(legacy);
            } catch (RuntimeException | LinkageError e) {
                // unknown id: stone below
            }
        }
        return state == null ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : state;
    }

    private static net.minecraft.world.item.ItemStack itemArgument(int[] args) {
        net.minecraft.world.item.ItemStack stack = net.minecraft.world.item.ItemStack.EMPTY;
        if (args != null && args.length > 0) {
            try {
                var item = legacyconnector.v1_12_2.net.minecraft.item.Item.getItemById(args[0]);
                if (item != null) {
                    stack = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(
                            new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, 1, args.length > 1 ? args[1] : 0));
                }
            } catch (RuntimeException | LinkageError e) {
                // unknown id: stone below
            }
        }
        return stack.isEmpty() ? new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE) : stack;
    }
}
