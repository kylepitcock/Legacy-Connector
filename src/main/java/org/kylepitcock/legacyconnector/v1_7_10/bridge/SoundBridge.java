package org.kylepitcock.legacyconnector.v1_7_10.bridge;

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
import org.kylepitcock.legacyconnector.core.Ids;

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

    /**
     * Further 1.7.10 sound events whose 1.9+ name the rules in {@link #vanillaEntitySound} do not reach, by modern id (looked
     * up on first use). Mods play vanilla's sounds by these names: {@code gui.button.press} is the click of every legacy
     * button a mod draws itself (DragonAPI's, ChromatiCraft's), which the engine reported as an unknown event.
     */
    private static final Map<String, String> RENAMES = new HashMap<>();

    static {
        String[][] renames = {
            {"gui.button.press", "ui.button.click"},
            {"ambient.cave.cave", "ambient.cave"},
            {"damage.fallbig", "entity.player.big_fall"},
            {"damage.fallsmall", "entity.player.small_fall"},
            {"liquid.splash", "entity.generic.splash"},
            {"liquid.swim", "entity.generic.swim"},
            {"minecart.inside", "entity.minecart.inside"},
            {"note.bass", "block.note_block.bass"},
            {"random.hurt", "entity.player.hurt"},
            {"game.potion.smash", "entity.splash_potion.break"},
            {"fireworks.blast", "entity.firework_rocket.blast"},
            {"fireworks.blast_far", "entity.firework_rocket.blast_far"},
            {"fireworks.largeBlast", "entity.firework_rocket.large_blast"},
            {"fireworks.largeBlast_far", "entity.firework_rocket.large_blast_far"},
            {"fireworks.launch", "entity.firework_rocket.launch"},
            {"fireworks.twinkle", "entity.firework_rocket.twinkle"},
            {"fireworks.twinkle_far", "entity.firework_rocket.twinkle_far"},
            {"mob.cat.hitt", "entity.cat.hurt"},
            {"mob.cat.meow", "entity.cat.ambient"},
            {"mob.enderdragon.wings", "entity.ender_dragon.flap"},
            {"mob.enderdragon.end", "entity.ender_dragon.death"},
            {"mob.ghast.affectionate_scream", "entity.ghast.scream"},
            {"mob.ghast.charge", "entity.ghast.warn"},
            {"mob.ghast.fireball", "entity.ghast.shoot"},
            {"mob.ghast.scream", "entity.ghast.hurt"},
            {"mob.horse.leather", "entity.horse.saddle"},
            {"mob.horse.soft", "entity.horse.step"},
            {"mob.horse.wood", "entity.horse.step_wood"},
            {"mob.horse.idle", "entity.horse.ambient"},
            {"mob.irongolem.throw", "entity.iron_golem.attack"},
            {"mob.magmacube.big", "entity.magma_cube.squish"},
            {"mob.magmacube.small", "entity.magma_cube.squish_small"},
            {"mob.slime.big", "entity.slime.squish"},
            {"mob.slime.small", "entity.slime.squish_small"},
            {"mob.wolf.bark", "entity.wolf.ambient"},
            {"mob.wolf.panting", "entity.wolf.pant"},
            {"mob.zombie.metal", "entity.zombie.attack_iron_door"},
            {"mob.zombie.wood", "entity.zombie.attack_wooden_door"},
            {"mob.zombie.woodbreak", "entity.zombie.break_wooden_door"},
            {"mob.zombie.remedy", "entity.zombie_villager.cure"},
            {"mob.zombie.unfect", "entity.zombie_villager.converted"},
            {"mob.zombiepig.zpig", "entity.zombified_piglin.ambient"},
            {"mob.zombiepig.zpigangry", "entity.zombified_piglin.angry"},
            {"mob.zombiepig.zpighurt", "entity.zombified_piglin.hurt"},
            {"mob.zombiepig.zpigdeath", "entity.zombified_piglin.death"},
        };
        for (String[] r : renames) RENAMES.put(r[0], r[1]);
        // game.<player|neutral|hostile>.<die|hurt|hurt.fall.big|hurt.fall.small|swim|swim.splash>
        for (String[] who : new String[][] {{"player", "player"}, {"neutral", "generic"}, {"hostile", "hostile"}}) {
            RENAMES.put("game." + who[0] + ".die", "entity." + who[1] + ".death");
            RENAMES.put("game." + who[0] + ".hurt", "entity." + who[1] + ".hurt");
            RENAMES.put("game." + who[0] + ".hurt.fall.big", "entity." + who[1] + ".big_fall");
            RENAMES.put("game." + who[0] + ".hurt.fall.small", "entity." + who[1] + ".small_fall");
            RENAMES.put("game." + who[0] + ".swim", "entity." + who[1] + ".swim");
            RENAMES.put("game." + who[0] + ".swim.splash", "entity." + who[1] + ".splash");
        }
        // The horse's relatives were sub-sounds of mob.horse: mob.horse.donkey.angry is entity.donkey.angry.
        for (String[] kind : new String[][] {{"donkey", "donkey"}, {"skeleton", "skeleton_horse"}, {"zombie", "zombie_horse"}}) {
            RENAMES.put("mob.horse." + kind[0] + ".idle", "entity." + kind[1] + ".ambient");
            RENAMES.put("mob.horse." + kind[0] + ".hit", "entity." + kind[1] + ".hurt");
            RENAMES.put("mob.horse." + kind[0] + ".death", "entity." + kind[1] + ".death");
            RENAMES.put("mob.horse." + kind[0] + ".angry", "entity." + kind[1] + ".angry");
        }
        for (String disc : new String[] {"13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11", "wait"}) {
            RENAMES.put("records." + disc, "music_disc." + disc);
        }
    }

    private SoundBridge() {}

    public static SoundEvent sound(String legacyName) {
        if (legacyName == null || legacyName.isEmpty()) return null;
        SoundEvent known = SOUNDS.get(legacyName);
        if (known != null) return known;
        String renamed = RENAMES.get(legacyName.startsWith("minecraft:") ? legacyName.substring("minecraft:".length()) : legacyName);
        if (renamed != null) {
            var id = net.minecraft.resources.ResourceLocation.withDefaultNamespace(renamed);
            known = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(id);
            if (known != null) {
                SOUNDS.put(legacyName, known);
                return known;
            }
        }
        known = vanillaEntitySound(legacyName);
        if (known != null) {
            SOUNDS.put(legacyName, known);
            return known;
        }
        // modid:path stays a mod sound; vanilla names that changed fall back to their 1.9+ dotted path guess.
        return SoundEvent.createVariableRangeEvent(Ids.location(legacyName, "minecraft"));
    }

    /** 1.7.10 vanilla sound events renamed in 1.9: {@code mob.cow.say} is {@code entity.cow.ambient}, {@code mob.zombie.hurt} is {@code entity.zombie.hurt}. */
    private static SoundEvent vanillaEntitySound(String name) {
        if (name.indexOf(':') >= 0) return null;
        String[] parts = name.split("\\.");
        if (parts.length < 3 || !(parts[0].equals("mob") || parts[0].equals("game"))) return null;
        String mob = switch (parts[1]) {
            case "endermen" -> "enderman";
            case "irongolem" -> "iron_golem";
            case "zombiepig" -> "zombified_piglin";
            case "snowman" -> "snow_golem";
            case "enderdragon" -> "ender_dragon";
            case "magmacube" -> "magma_cube";
            case "villager" -> "villager";
            case "neutral" -> "generic";
            case "hostile" -> "generic";
            case "player" -> "player";
            default -> parts[1];
        };
        StringBuilder rest = new StringBuilder();
        for (int i = 2; i < parts.length; i++) rest.append(i > 2 ? "." : "").append(parts[i]);
        String action = switch (rest.toString()) {
            case "say", "idle", "moan", "breathe", "bark" -> "ambient";
            case "haggle" -> "trade";
            case "hit", "hurtflesh" -> "hurt";
            case "kill" -> "death";
            case "walk" -> "step";
            case "no" -> "no";
            case "yes" -> "yes";
            default -> rest.toString().replace("hurt.fall.big", "big_fall").replace("hurt.fall.small", "small_fall");
        };
        for (String candidate : new String[] {"entity." + mob + "." + action, "entity." + mob + ".ambient", "entity." + mob + ".hurt"}) {
            var id = net.minecraft.resources.ResourceLocation.withDefaultNamespace(candidate);
            if (net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
                return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(id);
            }
        }
        return null;
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
                var item = legacyconnector.v1_7_10.net.minecraft.item.Item.getItemById(id);
                if (item == null) return null;
                return new ItemParticleOption(ParticleTypes.ITEM, new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, 1, meta).mcc$modern());
            }
            var block = legacyconnector.v1_7_10.net.minecraft.block.Block.getBlockById(id);
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
