package org.kylepitcock.legacyconnector.loader.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FunctionConverter1201Test {
    private static String quoted(String value) { return (char)34 + value + (char)34; }

    @Test
    void convertsLegacyParticleOptionsAndKeepsParticleArguments() {
        assertEquals("particle minecraft:dust{color:[0.7,1,18000000],scale:2} ~ ~ ~ 1 1 1 0 10",
                FunctionConverter1201.convertCommand(
                        "particle minecraft:dust 0.7 1 18000000 2 ~ ~ ~ 1 1 1 0 10"));
        assertEquals("particle minecraft:block{block_state:" + quoted("minecraft:stone") + "} ~ ~ ~ 1 0.5 1 0 30 normal",
                FunctionConverter1201.convertCommand("particle block stone ~ ~ ~ 1 0.5 1 0 30 normal"));
        String expectedItemParticle = "execute at @s run particle minecraft:item{item:{id:"
                + quoted("minecraft:carrot_on_a_stick") + ",components:{"
                + quoted("minecraft:custom_model_data") + ":1005}}} ^ ^1 ^ 0 1.5 0 .1 20";
        assertEquals(expectedItemParticle, FunctionConverter1201.convertCommand(
                "execute at @s run particle item carrot_on_a_stick{CustomModelData:1005} ^ ^1 ^ 0 1.5 0 .1 20"));
    }

    @Test
    void acceptsParticleCommandsWithoutOptionalForceModeAndDoesNotRewriteGrassBlocks() {
        assertEquals("particle minecraft:block{block_state:" + quoted("minecraft:big_dripleaf") + "} ~ ~ ~ 0.3 0.5 0.3 1 50",
                FunctionConverter1201.convertCommand("particle block minecraft:big_dripleaf ~ ~ ~ 0.3 0.5 0.3 1 50"));
        assertEquals("particle minecraft:item{item:{id:" + quoted("minecraft:carrot_on_a_stick") + "}} ^ ^1 ^ 0 1.5 0 .1 20",
                FunctionConverter1201.convertCommand("particle item carrot_on_a_stick ^ ^1 ^ 0 1.5 0 .1 20"));
        assertEquals("setblock ~ ~ ~ minecraft:grass_block",
                FunctionConverter1201.convertCommand("setblock ~ ~ ~ minecraft:grass_block"));
        assertEquals("setblock ~ ~ ~ minecraft:short_grass",
                FunctionConverter1201.convertCommand("setblock ~ ~ ~ minecraft:grass"));
    }

    @Test
    void migratesLegacyEntityAndStackNbtWithoutChangingTheArmorItemArray() {
        String source = "summon armor_stand ~ ~ ~ {ArmorItems:[{id:diamond_boots,Count:0},{id:diamond_leggings,Count:0},{id:iron_chestplate,Count:0},{id:\"carrot_on_a_stick\",Count:1,tag:{CustomModelData:1005}}],active:false}";
        String migrated = FunctionConverter1201.convertCommand(source);
        String customStack = "{id:" + quoted("minecraft:carrot_on_a_stick") + ",count:1,components:{"
                + quoted("minecraft:custom_model_data") + ":1005}}";
        assertTrue(migrated.contains("ArmorItems:[{},{},{},"
                + customStack + "]"), migrated);
        assertTrue(FunctionConverter1201.convertCommand(
                "summon minecraft:item ~ ~ ~ {Item:{id:\"minecraft:rose_bush\",Count:1b}}")
                .contains("Item:{id:\"minecraft:rose_bush\",count:1}"));
        assertEquals("attributes:[{id:" + quoted("minecraft:max_health") + ",base:20}]",
                FunctionConverter1201.convertCommand("Attributes:[{Name:generic.max_health,Base:20}]"));
        assertEquals("active_effects:[{id:" + quoted("minecraft:invisibility")
                        + ",amplifier:0b,duration:20,show_particles:0b}]",
                FunctionConverter1201.convertCommand("ActiveEffects:[{Id:14b,Amplifier:0b,Duration:20,ShowParticles:0b}]"));
        String selected = "@a[nbt={SelectedItem:{id:" + quoted("minecraft:carrot_on_a_stick")
                + ",components:{" + quoted("minecraft:custom_model_data") + ":2}}}]";
        assertEquals(selected, FunctionConverter1201.convertCommand("@a[nbt={SelectedItem:{id:"
                + quoted("minecraft:carrot_on_a_stick") + ",tag:{CustomModelData:2}}}]"));
    }

    @Test
    void commentsOnlyTheKnownOrphanedEarthSpikesSelector() {
        Map<String, byte[]> resources = new LinkedHashMap<>();
        String path = "data/afomni/function/aliens/vax/earth_spikes.mcfunction";
        resources.put(path, ("say before\n"
                + "execute as @e[type=armor_stand, tag=earth_spike, scores={afomni.EarthSpikes=..90}]\n"
                + "say after\n").getBytes(StandardCharsets.UTF_8));
        FunctionConverter1201.Stats stats = new FunctionConverter1201.Stats();
        String converted = new String(FunctionConverter1201.convertAll(resources, stats).get(path), StandardCharsets.UTF_8);
        assertTrue(converted.contains("# Legacy Connector: omitted an incomplete, actionless EarthSpikes execute selector."), converted);
        assertEquals(1, stats.droppedIncompleteCommands);
        assertEquals(1, stats.changedFiles);
    }

    @Test
    void convertsEnchantmentItemArgumentAndPreservesOtherCommands() {
        String expected = "loot replace entity @s container.0 mine ~ ~ ~ minecraft:diamond_pickaxe"
                + "[minecraft:enchantments={levels:{" + quoted("minecraft:silk_touch") + ":1}}]";
        assertEquals(expected, FunctionConverter1201.convertCommand(
                "loot replace entity @s container.0 mine ~ ~ ~ minecraft:diamond_pickaxe{Enchantments:[{lvl:1,id:silk_touch}]}"));
        assertEquals("execute run function example:test", FunctionConverter1201.convertCommand("execute run function example:test"));
        assertEquals("execute run", FunctionConverter1201.convertCommand("execute run"));
    }

    @Test
    void translatedParticleCommandsAreIdempotent() {
        String[] legacy = {
                "particle block stone ~ ~ ~ 1 1 1 0 30 normal",
                "particle block minecraft:big_dripleaf ~ ~ ~ 0.3 0.5 0.3 1 50",
                "particle item carrot_on_a_stick{CustomModelData:1005} ^ ^1 ^ 0 1.5 0 .1 20",
                "particle minecraft:dust 0.7 1 18000000 2 ~ ~ ~ 1 1 1 0 10"
        };
        for (String command : legacy) {
            String once = FunctionConverter1201.convertCommand(command);
            assertEquals(once, FunctionConverter1201.convertCommand(once), command);
        }
    }

    @Test
    void convertsAllFunctionResourcesAndReportsChanges() {
        Map<String, byte[]> resources = new LinkedHashMap<>();
        resources.put("data/example/functions/test.mcfunction",
                "particle block stone ~ ~ ~ 1 1 1 0 1\n".getBytes(StandardCharsets.UTF_8));
        resources.put("data/example/function/other.mcfunction", "say unchanged\n".getBytes(StandardCharsets.UTF_8));

        FunctionConverter1201.Stats stats = new FunctionConverter1201.Stats();
        Map<String, byte[]> converted = FunctionConverter1201.convertAll(resources, stats);

        assertEquals(1, stats.changedFiles);
        assertEquals(1, stats.rewrittenCommands);
        assertTrue(new String(converted.get("data/example/functions/test.mcfunction"), StandardCharsets.UTF_8)
                .contains("minecraft:block{block_state:" + quoted("minecraft:stone") + "}"));
        assertEquals("say unchanged\n",
                new String(converted.get("data/example/function/other.mcfunction"), StandardCharsets.UTF_8));
    }
}
