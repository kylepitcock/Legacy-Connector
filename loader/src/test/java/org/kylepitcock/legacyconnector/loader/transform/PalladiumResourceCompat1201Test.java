package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Regression tests for AlienEvo armor, Omnitrix property, and customization UI rewrites. */
class PalladiumResourceCompat1201Test {
    private static void put(Map<String, byte[]> resources, String path, String script) {
        resources.put(path, script.getBytes(StandardCharsets.UTF_8));
    }

    private static String get(Map<String, byte[]> resources, String path) {
        return new String(resources.get(path), StandardCharsets.UTF_8);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int offset = 0; (offset = text.indexOf(needle, offset)) >= 0; offset += needle.length()) {
            count++;
        }
        return count;
    }

    @Test
    void migratesAlienEvoArmorTransformAndPowerUiPathsAndIsIdempotent() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_STORE_ARMOR, """
                armorSlots.forEach(slot => {
                    if (entity[slot + 'ArmorItem']) {
                        hasArmor = true;
                        entity.persistentData.put(slot + 'ArmorStore', entity[slot + 'ArmorItem']);
                    }
                });
                """);
        String slotTransform = """
                let hasAnyArmor = armorSlots.some(slot => {
                    let armorItem = player[slot + 'ArmorItem'];
                    return armorItem && armorItem.id !== 'minecraft:air';
                });
                armorSlots.forEach(slot => {
                    let armorItem = player[slot + 'ArmorItem'];
                    if (armorItem && armorItem.id !== 'minecraft:air') {
                        player.persistentData.put(slot + 'ArmorStore', armorItem);
                    }
                });
                """;
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLOT_TRANSFORM, slotTransform);
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_QUICK_CHANGE, slotTransform);
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_POWER_UI, """
                if (currentTab === omnitrixTab) {}
                if (currentTab === omnitrixTab) {}
                if (currentTab === omnitrixTab) {}
                if (e.tab === omnitrixTab) {}
                if (e.tab === omnitrixTab) {}
                if (e.tab === omnitrixTab) {}
                if (e.tab === omnitrixTab) {}
                if (e.tab === omnitrixTab) {}
                InventoryScreen.renderEntityInInventory(guiGraphics, 42, 65, 35, rotation, null, player);
                new ResourceLocation("alienevo:textures/gui/power_screen/power_interface.png");
                """);
        put(resources, "addon/alienevo/kubejs_scripts/decouple.js", """
                function getDecoupleSoundId(prefix){ return 'alienevo:' + prefix + '_decouple'; }
                entity.level.playSound(null, entity.x, entity.y, entity.z,
                        getDecoupleSoundId(watchPrefix), entity.getSoundSource(), 1.0, 1.0);
                """);
        put(resources, "addon/alienevo/kubejs_scripts/detransform.js", """
                entity.level.playSound(null, entity.x, entity.y, entity.z,
                        `alienevo:${watchType}_detransform`, entity.getSoundSource(), 1.0, 1.0);
                """);
        put(resources, "addon/alienevo/kubejs_scripts/omnitrix_cycle.js", """
                entity.level.playSound(null, entity.x, entity.y, entity.z,
                        `alienevo:${soundToPlay}`, entity.getSoundSource(), 0.7, pitch);
                """);
        put(resources, "addon/alienevo/kubejs_scripts/codes.js", """
                const prototypePower = new ResourceLocation('alienevo:prototype_omnitrix');
                """);
        put(resources, "data/alienevo/kubejs_scripts/slam.js",
                "const gravityAttribute = \"forge:entity_gravity\";");
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_TRANSFORM, """
                function getRandomInt(min, max) {
                    return 1;
                }
                palladium.setProperty(entity, omnitrixPropertyKey, newAlienUnlocked);
                palladium.setProperty(entity, omnitrixPropertyKey, newAlienUnlocked);
                palladium.setProperty(entity, omnitrixPropertyKey, selectedAlienNum);
                palladium.setProperty(entity, omnitrixPropertyKey, selectedAlienNum);
                palladium.setProperty(entity, omnitrixPropertyKey, alienNumber);
                palladium.setProperty(entity, omnitrixPropertyKey, alien_num);
                let watchTypePowerId = new ResourceLocation(watchNamespace, `${watchName}_omnitrix`);
                let alienPowerId = new ResourceLocation(alienNamespace, alienPath);
                let bubblePowerId = new ResourceLocation(bubbleNamespace, 'transform_bubble');
                entity.level.playSound(null, entity.x, entity.y, entity.z,
                        `alienevo:${transformSound}_transform`, entity.getSoundSource(), 0.7, 1.0);
                """);

        assertEquals(10, PalladiumResourceCompat1201.apply(resources));

        String storeArmor = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_STORE_ARMOR);
        assertTrue(storeArmor.contains(PalladiumResourceCompat1201.SAFE_STORE_ARMOR_CHECK));
        assertFalse(storeArmor.contains(PalladiumResourceCompat1201.LEGACY_STORE_ARMOR_CHECK));

        for (String path : new String[] {
                PalladiumResourceCompat1201.ALIEN_EVO_SLOT_TRANSFORM,
                PalladiumResourceCompat1201.ALIEN_EVO_QUICK_CHANGE
        }) {
            String script = get(resources, path);
            assertTrue(script.contains(PalladiumResourceCompat1201.SAFE_ARMOR_ID_RETURN));
            assertTrue(script.contains(PalladiumResourceCompat1201.SAFE_ARMOR_ID_CHECK));
            assertFalse(script.contains(PalladiumResourceCompat1201.LEGACY_ARMOR_ID_RETURN));
            assertFalse(script.contains(PalladiumResourceCompat1201.LEGACY_ARMOR_ID_CHECK));
        }

        String transform = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_TRANSFORM);
        assertTrue(transform.startsWith(PalladiumResourceCompat1201.OMNITRIX_INTEGER_HELPER));
        assertEquals(6, transform.split("palladium\\.setProperty\\(entity, omnitrixPropertyKey, toOmnitrixInteger\\(", -1).length - 1);
        assertFalse(transform.contains("palladium.setProperty(entity, omnitrixPropertyKey, selectedAlienNum);"));
        assertFalse(transform.contains("palladium.setProperty(entity, omnitrixPropertyKey, alienNumber);"));
        assertFalse(transform.contains("palladium.setProperty(entity, omnitrixPropertyKey, alien_num);"));
        assertFalse(transform.contains("palladium.setProperty(entity, omnitrixPropertyKey, newAlienUnlocked);"));
        assertTrue(transform.contains("function ResourceLocation(namespaceOrLocation, path)"));
        assertTrue(transform.contains("    const AlienEvoJavaResourceLocation = Java.loadClass"));
        assertTrue(transform.contains("    const AlienEvoSoundRegistry = Java.loadClass"));
        assertTrue(transform.contains("    const AlienEvoSoundResourceLocation = Java.loadClass"));
        assertTrue(transform.contains("AlienEvoJavaResourceLocation.fromNamespaceAndPath"));
        assertTrue(transform.contains("getAlienEvoSoundEvent('alienevo:' + String(transformSound) + '_transform')"));
        assertFalse(transform.contains("`alienevo:${transformSound}_transform`,"));

        String decouple = get(resources, "addon/alienevo/kubejs_scripts/decouple.js");
        assertTrue(decouple.contains("function getAlienEvoSoundEvent(soundId)"));
        assertTrue(decouple.contains("getAlienEvoSoundEvent(getDecoupleSoundId(watchPrefix))"));
        assertFalse(decouple.contains("getDecoupleSoundId(watchPrefix),"));

        String detransform = get(resources, "addon/alienevo/kubejs_scripts/detransform.js");
        assertTrue(detransform.contains("function getAlienEvoSoundEvent(soundId)"));
        assertTrue(detransform.contains("getAlienEvoSoundEvent('alienevo:' + String(watchType) + '_detransform')"));
        assertFalse(detransform.contains("`alienevo:${watchType}_detransform`"));

        String cycle = get(resources, "addon/alienevo/kubejs_scripts/omnitrix_cycle.js");
        assertTrue(cycle.contains("function getAlienEvoSoundEvent(soundId)"));
        assertTrue(cycle.contains("    const AlienEvoSoundRegistry = Java.loadClass"));
        assertTrue(cycle.contains("    const AlienEvoSoundResourceLocation = Java.loadClass"));
        assertTrue(cycle.contains("getAlienEvoSoundEvent('alienevo:' + String(soundToPlay))"));
        assertFalse(cycle.contains("`alienevo:${soundToPlay}`,"));

        String codes = get(resources, "addon/alienevo/kubejs_scripts/codes.js");
        assertTrue(codes.contains("function ResourceLocation(namespaceOrLocation, path)"));
        assertTrue(codes.contains("    const AlienEvoJavaResourceLocation = Java.loadClass"));

        assertEquals("const gravityAttribute = \"minecraft:generic.gravity\";",
                get(resources, "data/alienevo/kubejs_scripts/slam.js"));

        String powerUi = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_POWER_UI);
        assertTrue(powerUi.contains("function ResourceLocation(namespaceOrLocation, path)"));
        assertTrue(powerUi.contains("    const AlienEvoJavaResourceLocation = Java.loadClass"));
        assertEquals(3, occurrences(powerUi, "currentTab.toString() === omnitrixTab"));
        assertEquals(5, occurrences(powerUi, "e.tab.toString() === omnitrixTab"));
        assertTrue(powerUi.contains(
                "InventoryScreen.renderEntityInInventory(guiGraphics, 42, 65, 35, "
                        + "new (Java.loadClass('org.joml.Vector3f'))(0.0, 0.0, 0.0), rotation, null, player);"));
        assertFalse(powerUi.contains("35, rotation, null, player"));
        assertFalse(powerUi.contains("currentTab === omnitrixTab"));
        assertFalse(powerUi.contains("e.tab === omnitrixTab"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }


    @Test
    void resolvesDiamondCreationSoundStringBeforeLevelPlaySoundOverloadResolution() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_DIAMOND_CREATION, """
                function tickDiamondCreation(entity, soundId) {
                    entity.level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            "alienevo:" + soundId, entity.getSoundSource(), 1.0, 1.0);
                }
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_DIAMOND_CREATION);
        assertTrue(migrated.contains("function getAlienEvoSoundEvent(soundId)"));
        assertTrue(migrated.contains("getAlienEvoSoundEvent(\"alienevo:\" + soundId)"));
        assertFalse(migrated.contains("\"alienevo:\" + soundId, entity.getSoundSource()"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_DIAMOND_CREATION));
    }

    @Test
    void adaptsAlienEvoScoreboardApiAcrossCommandsAndPowers() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, "data/alienevo/kubejs_scripts/level_command.js", """
                function maxAllAliens(playerName) {
                  let server = Utils.server;
                  let scoreboard = server.scoreboard;
                  scoreboard.getOrCreatePlayerScore(playerName, levelObj).setScore(10);
                  let current = scoreboard.getOrCreatePlayerScore(playerName, xpObj).getScore();
                }
                ServerEvents.commandRegistry(event => {
                    let { commands: Commands, arguments: Arguments } = event;
                    event.register(Commands.literal("maxaliens")
                        .requires(src => src.hasPermission(2))
                        .then(Commands.argument('player', Arguments.PLAYER.create(event))
                            .executes(ctx => {
                                let target = Arguments.PLAYER.getResult(ctx, 'player');
                                let username = target.getGameProfile().getName();
                                let aliensMaxed = maxAllAliens(username);
                                return 1;
                            })));
                });
                function giveAlienControl(playerName) {
                  let server = Utils.server;
                  let scoreboard = server.scoreboard;
                  let current = scoreboard.getOrCreatePlayerScore(playerName, xpObj).getScore();
                }
                ServerEvents.commandRegistry(event => {
                    let { commands: Commands, arguments: Arguments } = event;
                    event.register(Commands.literal("mastercontrol")
                        .requires(src => src.hasPermission(2))
                        .then(Commands.argument('player', Arguments.PLAYER.create(event))
                            .executes(ctx => {
                                let target = Arguments.PLAYER.getResult(ctx, 'player');
                                let username = target.getGameProfile().getName();
                                return giveAlienControl(username);
                            })));
                });
                """);
        put(resources, "addon/alienevo/kubejs_scripts/detransform.js", """
                entity.server.runCommandSilent(`execute as ${playerUsername} at @s run superpower remove ${currentNamespace}:${currentPath}`);
                superpowerUtil.addSuperpower(entity, new ResourceLocation(`${watchNamespace}:${watchType}_omnitrix`));
                entity.tags.remove("AlienEvo.Transformation");
                let scoreboardManager = Utils.server.scoreboard;
                let timerObjective = scoreboardManager.getObjective('AlienEvo.Timer');
                let playerTimerScore = scoreboardManager.getOrCreatePlayerScore(playerUsername, timerObjective);
                let currentTimerValue = playerTimerScore.getScore();
                entity.server.runCommandSilent(`execute as ${playerUsername} at @s run scoreboard players set @s AlienEvo.Timer ${Math.floor(currentTimerValue / 2)}`);
                palladium.setProperty(entity, 'quick_change_wheel', "disabled");
                entity.level.playSound(null, entity.x, entity.y, entity.z,
                        `alienevo:${watchType}_detransform`, entity.getSoundSource(), 1.0, 1.0);
                """);
        put(resources, "addon/alienevo/kubejs_scripts/charge_leap.js", """
                let score = scoreboard.getOrCreatePlayerScore(username, scoreboardObj);
                score.setScore(score.getScore() + 1);
                """);
        put(resources, "addon/alienevo/kubejs_scripts/rolling_mechanics.js", """
                let score = scoreboard.getOrCreatePlayerScore(username, scoreboardObj);
                let current = score.getScore();
                """);
        put(resources, "addon/alienevo/kubejs_scripts/stat_ability.js", """
                .tick((entity, entry, holder, enabled) => {
                    const scoreboard = Utils.server.scoreboard;
                    let score = scoreboard.getOrCreatePlayerScore(username, scoreboard_obj);
                    let current = score.getScore();
                });
                .lastTick((entity, entry, holder, enabled) => {
                    const scoreboard = Utils.server.scoreboard;
                });
                """);
        put(resources, "data/alienevo/kubejs_scripts/xp.js", """
                let score = scoreboard.getOrCreatePlayerScore(username, xpObj);
                let current = score.getScore();
                score.setScore(current + 1);
                """);

        assertEquals(6, PalladiumResourceCompat1201.apply(resources));

        String level = get(resources, "data/alienevo/kubejs_scripts/level_command.js");
        assertTrue(level.contains("forNameOnly(String(playerName))"));
        assertTrue(level.contains(".set(10)"));
        assertTrue(level.contains(".get()"));
        assertFalse(level.contains("getOrCreatePlayerScore(playerName,"));
        assertFalse(level.contains(".getScore()"));
        assertFalse(level.contains(".setScore("));
        assertTrue(level.contains("Commands.literal(\"maxaliens\")"));
        assertTrue(level.contains("Commands.literal(\"mastercontrol\")"));
        assertEquals(2, occurrences(level, ".requires(src => src.hasPermission(2))"));
        assertEquals(2, occurrences(level, "Arguments.PLAYER.create(event)"));
        assertFalse(level.contains("Arguments.ENTITY.create(event)"));
        assertFalse(level.contains("Utils.server"));
        assertTrue(level.contains("function maxAllAliens(playerName, server)"));
        assertTrue(level.contains("function giveAlienControl(playerName, server)"));
        assertTrue(level.contains("maxAllAliens(username, target.server)"));
        assertTrue(level.contains("giveAlienControl(username, target.server)"));

        String detransform = get(resources, "addon/alienevo/kubejs_scripts/detransform.js");
        assertTrue(detransform.contains("forNameOnly(String(playerUsername))"));
        assertTrue(detransform.contains("playerTimerScore.get()"));
        assertTrue(detransform.contains("if (timerObjective != null) {"));
        assertTrue(detransform.contains("Math.floor(currentTimerValue / 2)"));
        assertTrue(detransform.indexOf("if (timerObjective != null) {")
                < detransform.indexOf("playerTimerScore = scoreboardManager.getOrCreatePlayerScore"));
        assertTrue(detransform.indexOf("playerTimerScore = scoreboardManager.getOrCreatePlayerScore")
                < detransform.indexOf("Math.floor(currentTimerValue / 2)"));
        assertTrue(detransform.indexOf("superpower remove ${currentNamespace}:${currentPath}")
                < detransform.indexOf("new ResourceLocation(`${watchNamespace}:${watchType}_omnitrix`)") );
        assertTrue(detransform.indexOf("new ResourceLocation(`${watchNamespace}:${watchType}_omnitrix`)")
                < detransform.indexOf("entity.tags.remove(\"AlienEvo.Transformation\")"));
        assertTrue(detransform.contains("palladium.setProperty(entity, 'quick_change_wheel', \"disabled\")"));

        String chargeLeap = get(resources, "addon/alienevo/kubejs_scripts/charge_leap.js");
        assertTrue(chargeLeap.contains("forNameOnly(String(username))"));
        assertTrue(chargeLeap.contains("score.get()"));
        assertTrue(chargeLeap.contains("score.set("));

        String rolling = get(resources, "addon/alienevo/kubejs_scripts/rolling_mechanics.js");
        assertTrue(rolling.contains("forNameOnly(String(username))"));
        assertTrue(rolling.contains("score.get()"));

        String stat = get(resources, "addon/alienevo/kubejs_scripts/stat_ability.js");
        assertTrue(stat.contains("forNameOnly(String(username))"));
        assertTrue(stat.contains("score.get()"));
        assertEquals(2, occurrences(stat, "entity.server.scoreboard"));
        assertFalse(stat.contains("Utils.server.scoreboard"));
        String xp = get(resources, "data/alienevo/kubejs_scripts/xp.js");
        assertTrue(xp.contains("forNameOnly(String(username))"));
        assertTrue(xp.contains("score.get()"));
        assertTrue(xp.contains("score.set("));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }

    @Test
    void migratesRemainingAlienEvoServerAccessThroughCallbackContextAndIsIdempotent() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, "addon/alienevo/kubejs_scripts/charge_leap.js", """
                .tick((entity, entry, holder, enabled) => {
                    let username = entity.getGameProfile().getName();
                    let scoreboard = Utils.server.scoreboard;
                    let score = scoreboard.getOrCreatePlayerScore(username, scoreboardObj);
                });
                .lastTick((entity, entry, holder, enabled) => {
                    let username = entity.getGameProfile().getName();
                    let scoreboard = Utils.server.scoreboard;
                    let score = scoreboard.getOrCreatePlayerScore(username, scoreboardObj);
                });
                """);
        put(resources, "addon/alienevo/kubejs_scripts/detransform.js", """
                .lastTick((entity, entry, holder, enabled) => {
                    let scoreboardManager = Utils.server.scoreboard;
                    entity.server.runCommandSilent("say reverting");
                });
                """);
        put(resources, "addon/alienevo/kubejs_scripts/rolling_mechanics.js", """
                .tick((entity, entry, holder, enabled) => {
                    let scoreboard = Utils.server.scoreboard;
                });
                """);
        put(resources, "data/alienevo/kubejs_scripts/xp.js", """
                EntityEvents.death(event => {
                    let source = event.source;
                    if (source && source.player) {
                        let player = source.player;
                        let scoreboard = Utils.server.scoreboard;
                    }
                });
                """);
        put(resources, "data/alienevo/kubejs_scripts/level_command.js", """
                function maxAllAliens(playerName) {
                  let server = Utils.server;
                  let scoreboard = server.scoreboard;
                  let score = scoreboard.getOrCreatePlayerScore(playerName, levelObj);
                }
                ServerEvents.commandRegistry(event => {
                  event.register(Commands.literal("maxaliens").executes(ctx => {
                    let target = Arguments.PLAYER.getResult(ctx, 'player');
                    let username = target.getGameProfile().getName();
                    let aliensMaxed = maxAllAliens(username);
                    return 1;
                  }));
                });
                function giveAlienControl(playerName) {
                  let server = Utils.server;
                  let scoreboard = server.scoreboard;
                }
                ServerEvents.commandRegistry(event => {
                  event.register(Commands.literal("mastercontrol").executes(ctx => {
                    let target = Arguments.PLAYER.getResult(ctx, 'player');
                    let username = target.getGameProfile().getName();
                    return giveAlienControl(username);
                  }));
                });
                """);

        assertEquals(5, PalladiumResourceCompat1201.apply(resources));

        String chargeLeap = get(resources, "addon/alienevo/kubejs_scripts/charge_leap.js");
        assertEquals(2, occurrences(chargeLeap, "entity.server.scoreboard"));
        assertFalse(chargeLeap.contains("Utils.server"));

        String detransform = get(resources, "addon/alienevo/kubejs_scripts/detransform.js");
        assertEquals(1, occurrences(detransform, "entity.server.scoreboard"));
        assertFalse(detransform.contains("Utils.server"));

        String rolling = get(resources, "addon/alienevo/kubejs_scripts/rolling_mechanics.js");
        assertEquals(1, occurrences(rolling, "entity.server.scoreboard"));
        assertFalse(rolling.contains("Utils.server"));

        String xp = get(resources, "data/alienevo/kubejs_scripts/xp.js");
        assertEquals(1, occurrences(xp, "player.server.scoreboard"));
        assertFalse(xp.contains("Utils.server"));

        String level = get(resources, "data/alienevo/kubejs_scripts/level_command.js");
        assertFalse(level.contains("Utils.server"));
        assertTrue(level.contains("function maxAllAliens(playerName, server)"));
        assertTrue(level.contains("function giveAlienControl(playerName, server)"));
        assertTrue(level.contains("maxAllAliens(username, target.server)"));
        assertTrue(level.contains("return giveAlienControl(username, target.server)"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }

    @Test
    void migratesAlienEvoStatAbilityServerAccessAndIsIdempotent() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_STAT_ABILITY, """
                .lastTick((entity, entry, holder, enabled) => {
                    const scoreboard = Utils.server.scoreboard;
                });
                .tick((entity, entry, holder, enabled) => {
                    const scoreboard = Utils.server.scoreboard;
                });
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_STAT_ABILITY);
        assertEquals(2, occurrences(migrated, "entity.server.scoreboard"));
        assertFalse(migrated.contains("Utils.server.scoreboard"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_STAT_ABILITY));

        Map<String, byte[]> unknownShape = new HashMap<>();
        put(unknownShape, PalladiumResourceCompat1201.ALIEN_EVO_STAT_ABILITY,
                "const scoreboard = Utils.server.scoreboard;");
        assertThrows(IllegalStateException.class, () -> PalladiumResourceCompat1201.apply(unknownShape));
    }

    @Test
    void migratesOmnitrixPaletteAndNicknamePacketSendersToKubeJs7Player() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_STUFF, """
                NetworkEvents.dataReceived('saveBeamColorData', event => {
                    let player = event.player;
                });
                NetworkEvents.dataReceived('saveEnergyColorData', event => {
                    let player = event.player;
                });
                NetworkEvents.dataReceived('savePaletteData', event => {
                    let player = event.entity;
                });
                NetworkEvents.dataReceived('alienevo_nickname', event => {
                    let player = event.entity;
                });
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_STUFF);
        assertEquals(4, occurrences(migrated, "let player = event.player;"));
        assertFalse(migrated.contains("let player = event.entity;"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_STUFF));
    }

    @Test
    void migratesAlienEvoFailsafeSoundEventAndIsIdempotent() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_FAILSAFE, """
                entity.level.playSound(null, entity.x, entity.y, entity.z,
                        `alienevo:${watchType}_detransform`, entity.getSoundSource(), 1.0, 1.0);
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_FAILSAFE);
        assertTrue(migrated.contains("function getAlienEvoSoundEvent(soundId)"));
        assertTrue(migrated.contains("getAlienEvoSoundEvent('alienevo:' + String(watchType) + '_detransform')"));
        assertFalse(migrated.contains("`alienevo:${watchType}_detransform`"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_FAILSAFE));

        Map<String, byte[]> unknownShape = new HashMap<>();
        put(unknownShape, PalladiumResourceCompat1201.ALIEN_EVO_FAILSAFE,
                "entity.level.playSound(null, x, y, z, 'alienevo:unexpected', source, 1.0, 1.0);");
        assertThrows(IllegalStateException.class, () -> PalladiumResourceCompat1201.apply(unknownShape));
    }

    @Test
    void keepsAlienEvoFlattenAbilityScriptsLoadableWithoutOptionalPehkui() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, "addon/alienevo/kubejs_scripts/flatten.js", """
                const SCALE_TYPES = Java.loadClass("virtuoel.pehkui.api.ScaleTypes");
                StartupEvents.registry('palladium:abilities', event => {
                    event.create('alienevo:flatten')
                        .tick((entity, entry, holder, enabled) => {
                            if (enabled) SCALE_TYPES.HEIGHT.getScaleData(entity).setScale(0.3);
                        })
                        .lastTick((entity, entry, holder, enabled) => {
                            SCALE_TYPES.HEIGHT.getScaleData(entity).setScale(1.0);
                        });
                });
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String flatten = get(resources, "addon/alienevo/kubejs_scripts/flatten.js");
        assertFalse(flatten.contains("const SCALE_TYPES = Java.loadClass("));
        assertTrue(flatten.contains("try {\n        return Java.loadClass(\"virtuoel.pehkui.api.ScaleTypes\");"), flatten);
        assertTrue(flatten.contains("const noScale = { getScaleData: entity => ({ setScale: value => {} }) };"), flatten);
        assertTrue(flatten.contains("HEIGHT: noScale, WIDTH: noScale, STEP_HEIGHT: noScale, JUMP_HEIGHT: noScale"), flatten);
        assertTrue(flatten.contains("SCALE_TYPES.HEIGHT.getScaleData(entity).setScale(0.3)"), flatten);

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(flatten, get(resources, "addon/alienevo/kubejs_scripts/flatten.js"));
    }


    @Test
    void doesNotTouchOtherResourcesOrUnrelatedScripts() {
        Map<String, byte[]> resources = new HashMap<>();
        String unrelated = "if (entity[slot + 'ArmorItem']) { }";
        put(resources, "addon/example/kubejs_scripts/store_armor.js", unrelated);
        put(resources, "data/example/kubejs_scripts/quick_change.js",
                "return armorItem && armorItem.id !== 'minecraft:air';");
        String unrelatedResourceLocation = "new ResourceLocation('example:unrelated');";
        put(resources, "addon/example/kubejs_scripts/ui.js", unrelatedResourceLocation);

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(unrelated, get(resources, "addon/example/kubejs_scripts/store_armor.js"));
        assertEquals("return armorItem && armorItem.id !== 'minecraft:air';",
                get(resources, "data/example/kubejs_scripts/quick_change.js"));
        assertEquals(unrelatedResourceLocation,
                get(resources, "addon/example/kubejs_scripts/ui.js"));
    }

    @Test
    void toleratesAlreadyUpdatedAndMissingAlienEvoScripts() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_STORE_ARMOR,
                "if (entity[slot + 'ArmorItem'] && !entity[slot + 'ArmorItem'].isEmpty()) { }");
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLOT_TRANSFORM,
                "return armorItem && !armorItem.isEmpty(); if (armorItem && !armorItem.isEmpty()) { }");

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertFalse(resources.containsKey(PalladiumResourceCompat1201.ALIEN_EVO_QUICK_CHANGE));
    }
}
