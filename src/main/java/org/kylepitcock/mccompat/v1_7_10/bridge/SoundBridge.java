package org.kylepitcock.mccompat.v1_7_10.bridge;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SoundType;
import org.joml.Vector3f;
import org.kylepitcock.mccompat.core.Ids;

/**
 * 1.7.10 sound and particle names -> modern registry objects. Sound names were renamed wholesale in 1.9
 * ({@code random.pop} -> {@code entity.item.pickup}); mod sounds ({@code modid:path}) resolve through the mod's
 * own sounds.json and are played as direct holders.
 */
public final class SoundBridge {
    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();

    static {
        SOUNDS.put("random.pop", SoundEvents.ITEM_PICKUP);
        SOUNDS.put("random.click", SoundEvents.STONE_BUTTON_CLICK_ON);
        SOUNDS.put("random.wood_click", SoundEvents.WOODEN_BUTTON_CLICK_ON);
        SOUNDS.put("random.orb", SoundEvents.EXPERIENCE_ORB_PICKUP);
        SOUNDS.put("random.levelup", SoundEvents.PLAYER_LEVELUP);
        SOUNDS.put("random.explode", SoundEvents.GENERIC_EXPLODE.value());
        SOUNDS.put("random.fizz", SoundEvents.FIRE_EXTINGUISH);
        SOUNDS.put("random.bow", SoundEvents.ARROW_SHOOT);
        SOUNDS.put("random.bowhit", SoundEvents.ARROW_HIT);
        SOUNDS.put("random.break", SoundEvents.ITEM_BREAK);
        SOUNDS.put("random.burp", SoundEvents.PLAYER_BURP);
        SOUNDS.put("random.eat", SoundEvents.GENERIC_EAT);
        SOUNDS.put("random.drink", SoundEvents.GENERIC_DRINK);
        SOUNDS.put("random.splash", SoundEvents.GENERIC_SPLASH);
        SOUNDS.put("random.swim", SoundEvents.GENERIC_SWIM);
        SOUNDS.put("random.door_open", SoundEvents.WOODEN_DOOR_OPEN);
        SOUNDS.put("random.door_close", SoundEvents.WOODEN_DOOR_CLOSE);
        SOUNDS.put("random.chestopen", SoundEvents.CHEST_OPEN);
        SOUNDS.put("random.chestclosed", SoundEvents.CHEST_CLOSE);
        SOUNDS.put("random.anvil_land", SoundEvents.ANVIL_LAND);
        SOUNDS.put("random.anvil_use", SoundEvents.ANVIL_USE);
        SOUNDS.put("random.anvil_break", SoundEvents.ANVIL_BREAK);
        SOUNDS.put("random.successful_hit", SoundEvents.ARROW_HIT_PLAYER);
        SOUNDS.put("random.glass", SoundEvents.GLASS_BREAK);
        SOUNDS.put("random.fuse", SoundEvents.TNT_PRIMED);
        SOUNDS.put("random.classic_hurt", SoundEvents.PLAYER_HURT);
        SOUNDS.put("fire.fire", SoundEvents.FIRE_AMBIENT);
        SOUNDS.put("fire.ignite", SoundEvents.FLINTANDSTEEL_USE);
        SOUNDS.put("liquid.water", SoundEvents.WATER_AMBIENT);
        SOUNDS.put("liquid.lava", SoundEvents.LAVA_AMBIENT);
        SOUNDS.put("liquid.lavapop", SoundEvents.LAVA_POP);
        SOUNDS.put("tile.piston.out", SoundEvents.PISTON_EXTEND);
        SOUNDS.put("tile.piston.in", SoundEvents.PISTON_CONTRACT);
        SOUNDS.put("note.harp", SoundEvents.NOTE_BLOCK_HARP.value());
        SOUNDS.put("note.bd", SoundEvents.NOTE_BLOCK_BASEDRUM.value());
        SOUNDS.put("note.snare", SoundEvents.NOTE_BLOCK_SNARE.value());
        SOUNDS.put("note.hat", SoundEvents.NOTE_BLOCK_HAT.value());
        SOUNDS.put("note.bassattack", SoundEvents.NOTE_BLOCK_BASS.value());
        SOUNDS.put("note.pling", SoundEvents.NOTE_BLOCK_PLING.value());
        SOUNDS.put("portal.portal", SoundEvents.PORTAL_AMBIENT);
        SOUNDS.put("portal.trigger", SoundEvents.PORTAL_TRIGGER);
        SOUNDS.put("portal.travel", SoundEvents.PORTAL_TRAVEL);
        SOUNDS.put("mob.endermen.portal", SoundEvents.ENDERMAN_TELEPORT);
        SOUNDS.put("ambient.weather.thunder", SoundEvents.LIGHTNING_BOLT_THUNDER);
        SOUNDS.put("ambient.weather.rain", SoundEvents.WEATHER_RAIN);
        SOUNDS.put("game.player.hurt", SoundEvents.PLAYER_HURT);
        SOUNDS.put("damage.hit", SoundEvents.PLAYER_HURT);
        SOUNDS.put("game.tnt.primed", SoundEvents.TNT_PRIMED);
        SOUNDS.put("minecart.base", SoundEvents.MINECART_RIDING);
        SOUNDS.put("mob.chicken.plop", SoundEvents.CHICKEN_EGG);
        for (var e : Map.of("stone", SoundType.STONE, "wood", SoundType.WOOD, "gravel", SoundType.GRAVEL, "grass", SoundType.GRASS,
                "cloth", SoundType.WOOL, "sand", SoundType.SAND, "snow", SoundType.SNOW, "ladder", SoundType.LADDER, "glass", SoundType.GLASS,
                "anvil", SoundType.ANVIL).entrySet()) {
            SOUNDS.put("dig." + e.getKey(), e.getValue().getBreakSound());
            SOUNDS.put("step." + e.getKey(), e.getValue().getStepSound());
        }
    }

    private SoundBridge() {}

    public static SoundEvent sound(String legacyName) {
        SoundEvent known = SOUNDS.get(legacyName);
        if (known != null) return known;
        // modid:path stays a mod sound; vanilla names that changed fall back to their 1.9+ dotted path guess.
        return SoundEvent.createVariableRangeEvent(Ids.location(legacyName, "minecraft"));
    }

    public static void play(Level level, Player except, double x, double y, double z, String name, float volume, float pitch) {
        if (name == null || name.isEmpty()) return;
        level.playSound(except, x, y, z, Holder.direct(sound(name)), SoundSource.BLOCKS, volume, pitch);
    }

    public static void particle(LevelAccessor level, String name, double x, double y, double z, double vx, double vy, double vz) {
        ParticleOptions p = particle(name, vx, vy, vz);
        if (p == null) return;
        if (name.equals("reddust")) level.addParticle(p, x, y, z, 0, 0, 0);
        else level.addParticle(p, x, y, z, vx, vy, vz);
    }

    static ParticleOptions particle(String name, double vx, double vy, double vz) {
        if (name.startsWith("iconcrack_") || name.startsWith("blockcrack_") || name.startsWith("blockdust_")) {
            String[] parts = name.split("_");
            int id = parts.length > 1 ? parseInt(parts[1]) : 0;
            int meta = parts.length > 2 ? parseInt(parts[2]) : 0;
            if (name.startsWith("iconcrack_")) {
                var item = mccompat.v1_7_10.net.minecraft.item.Item.getItemById(id);
                if (item == null) return null;
                return new ItemParticleOption(ParticleTypes.ITEM, new mccompat.v1_7_10.net.minecraft.item.ItemStack(item, 1, meta).mcc$modern());
            }
            var block = mccompat.v1_7_10.net.minecraft.block.Block.getBlockById(id);
            if (block == null) return null;
            return new BlockParticleOption(name.startsWith("blockdust_") ? ParticleTypes.DUST_PILLAR : ParticleTypes.BLOCK, BlockBridge.toModern(block, meta));
        }
        return switch (name) {
            case "smoke" -> ParticleTypes.SMOKE;
            case "largesmoke" -> ParticleTypes.LARGE_SMOKE;
            case "flame" -> ParticleTypes.FLAME;
            case "reddust" -> new DustParticleOptions(redstoneColor(vx, vy, vz), 1.0F);
            case "portal" -> ParticleTypes.PORTAL;
            case "heart" -> ParticleTypes.HEART;
            case "explode" -> ParticleTypes.POOF;
            case "largeexplode" -> ParticleTypes.EXPLOSION;
            case "hugeexplosion" -> ParticleTypes.EXPLOSION_EMITTER;
            case "crit" -> ParticleTypes.CRIT;
            case "magicCrit" -> ParticleTypes.ENCHANTED_HIT;
            case "enchantmenttable" -> ParticleTypes.ENCHANT;
            case "note" -> ParticleTypes.NOTE;
            case "lava" -> ParticleTypes.LAVA;
            case "splash" -> ParticleTypes.SPLASH;
            case "bubble" -> ParticleTypes.BUBBLE;
            case "snowballpoof" -> ParticleTypes.ITEM_SNOWBALL;
            case "snowshovel" -> ParticleTypes.SNOWFLAKE;
            case "slime" -> ParticleTypes.ITEM_SLIME;
            case "happyVillager" -> ParticleTypes.HAPPY_VILLAGER;
            case "angryVillager" -> ParticleTypes.ANGRY_VILLAGER;
            case "cloud" -> ParticleTypes.CLOUD;
            case "dripWater" -> ParticleTypes.DRIPPING_WATER;
            case "dripLava" -> ParticleTypes.DRIPPING_LAVA;
            case "witchMagic" -> ParticleTypes.WITCH;
            case "spell", "mobSpell", "mobSpellAmbient" -> ParticleTypes.EFFECT;
            case "instantSpell" -> ParticleTypes.INSTANT_EFFECT;
            case "townaura" -> ParticleTypes.MYCELIUM;
            case "fireworksSpark" -> ParticleTypes.FIREWORK;
            case "suspended", "depthsuspend" -> ParticleTypes.UNDERWATER;
            default -> null;
        };
    }

    /** Legacy reddust used the velocity as an RGB color (0 = default red). */
    private static Vector3f redstoneColor(double r, double g, double b) {
        if (r == 0 && g == 0 && b == 0) return new Vector3f(1.0F, 0.0F, 0.0F);
        return new Vector3f((float) Math.max(0, r), (float) Math.max(0, g), (float) Math.max(0, b));
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
