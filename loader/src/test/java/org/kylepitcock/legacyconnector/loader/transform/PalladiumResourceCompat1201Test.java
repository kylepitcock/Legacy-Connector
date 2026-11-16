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
    void migratesAlienEvoItemOfNbtOverloadToCustomDataComponent() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_CORE_REMOVAL, """
                var coreItem = Item.of('alienevo:prototype_core', 1, nbtData);
                entity.give(coreItem);
                """);
        put(resources, "addon/alienevo/kubejs_scripts/decouple.js", """
                function getDecoupleSoundId(prefix) { return 'alienevo:' + prefix + '_decouple'; }
                var watchItem = Item.of(itemId, 1, nbtData);
                entity.give(watchItem);
                entity.level.playSound(null, entity.x, entity.y, entity.z,
                        getDecoupleSoundId(watchPrefix), entity.getSoundSource(), 1.0, 1.0);
                """);

        assertEquals(2, PalladiumResourceCompat1201.apply(resources));

        String core = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_CORE_REMOVAL);
        assertTrue(core.contains("var coreItem = Item.of('alienevo:prototype_core', 1);"));
        assertTrue(core.contains("coreItem.setCustomData(NBT.wrapCompound(nbtData));"));
        assertFalse(core.contains("Item.of('alienevo:prototype_core', 1, nbtData)"));

        String decouple = get(resources, "addon/alienevo/kubejs_scripts/decouple.js");
        assertTrue(decouple.contains("var watchItem = Item.of(itemId, 1);"));
        assertTrue(decouple.contains("watchItem.setCustomData(NBT.wrapCompound(nbtData));"));
        assertTrue(decouple.contains("getAlienEvoSoundEvent(getDecoupleSoundId(watchPrefix))"));
        assertFalse(decouple.contains("Item.of(itemId, 1, nbtData)"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }

    @Test
    void migratesSeparateAlienEvoItemStackSetNbtCallToCustomDataComponent() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_DECOUPLE, """
                var watchItem = Item.of(itemId, 1);
                watchItem.setNbt(nbtData);
                entity.give(watchItem);
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_DECOUPLE);
        assertTrue(migrated.contains("watchItem.setCustomData(NBT.wrapCompound(nbtData));"));
        assertFalse(migrated.contains("watchItem.setNbt(nbtData);"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_DECOUPLE));
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
                var watchItem = Item.of(itemId, 1, nbtData);
                entity.give(watchItem);
                var watchPowerId = new ResourceLocation('alienevo:prototype_omnitrix');
                entity.level.playSound(null, entity.x, entity.y, entity.z,
                        getDecoupleSoundId(watchPrefix), entity.getSoundSource(), 1.0, 1.0);
                """);
        put(resources, "addon/alienevo/kubejs_scripts/detransform.js", """
                var watchPowerId = new ResourceLocation('alienevo:prototype_omnitrix');
                entity.level.playSound(null, entity.x, entity.y, entity.z,
                        `alienevo:${watchType}_detransform`, entity.getSoundSource(), 1.0, 1.0);
                """);
        put(resources, "addon/alienevo/kubejs_scripts/cycle_sounds.js", """
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
        assertTrue(transform.contains("    var AlienEvoJavaResourceLocation = Java.loadClass"));
        assertTrue(transform.contains("    var AlienEvoSoundRegistry = Java.loadClass"));
        assertTrue(transform.contains("    var AlienEvoSoundResourceLocation = Java.loadClass"));
        assertTrue(transform.contains("AlienEvoJavaResourceLocation.fromNamespaceAndPath"));
        assertTrue(transform.contains("getAlienEvoSoundEvent('alienevo:' + String(transformSound) + '_transform')"));
        assertFalse(transform.contains("`alienevo:${transformSound}_transform`,"));

        String decouple = get(resources, "addon/alienevo/kubejs_scripts/decouple.js");
        assertTrue(decouple.contains("function getAlienEvoSoundEvent(soundId)"));
        assertTrue(decouple.contains("getAlienEvoSoundEvent(getDecoupleSoundId(watchPrefix))"));
        assertFalse(decouple.contains("getDecoupleSoundId(watchPrefix),"));
        assertTrue(decouple.contains("var watchItem = Item.of(itemId, 1);"));
        assertTrue(decouple.contains("watchItem.setCustomData(NBT.wrapCompound(nbtData));"));
        assertFalse(decouple.contains("Item.of(itemId, 1, nbtData)"));

        String detransform = get(resources, "addon/alienevo/kubejs_scripts/detransform.js");
        assertTrue(detransform.contains("function getAlienEvoSoundEvent(soundId)"));
        assertTrue(detransform.contains("getAlienEvoSoundEvent('alienevo:' + String(watchType) + '_detransform')"));
        assertFalse(detransform.contains("`alienevo:${watchType}_detransform`"));

        // Reuse the decouple script loaded above; assert its injected helper alias is redeclarable.
        assertTrue(decouple.contains("    var AlienEvoJavaResourceLocation = Java.loadClass"));
        assertFalse(decouple.contains("    const AlienEvoJavaResourceLocation = Java.loadClass"));

        // Reuse the detransform script loaded above; assert its injected helper alias is redeclarable.
        assertTrue(detransform.contains("    var AlienEvoJavaResourceLocation = Java.loadClass"));
        assertFalse(detransform.contains("    const AlienEvoJavaResourceLocation = Java.loadClass"));

        String cycle = get(resources, "addon/alienevo/kubejs_scripts/cycle_sounds.js");
        assertTrue(cycle.contains("function getAlienEvoSoundEvent(soundId)"));
        assertTrue(cycle.contains("    var AlienEvoSoundRegistry = Java.loadClass"));
        assertTrue(cycle.contains("    var AlienEvoSoundResourceLocation = Java.loadClass"));
        assertTrue(cycle.contains("getAlienEvoSoundEvent('alienevo:' + String(soundToPlay))"));
        assertFalse(cycle.contains("`alienevo:${soundToPlay}`,"));

        String codes = get(resources, "addon/alienevo/kubejs_scripts/codes.js");
        assertTrue(codes.contains("function ResourceLocation(namespaceOrLocation, path)"));
        assertTrue(codes.contains("    var AlienEvoJavaResourceLocation = Java.loadClass"));

        assertEquals("const gravityAttribute = \"minecraft:generic.gravity\";",
                get(resources, "data/alienevo/kubejs_scripts/slam.js"));

        String powerUi = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_POWER_UI);
        assertTrue(powerUi.contains("function ResourceLocation(namespaceOrLocation, path)"));
        assertTrue(powerUi.contains("    var AlienEvoJavaResourceLocation = Java.loadClass"));
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
    void resolvesDiamondCreationSoundAndPreservesNativeSoundSourceBeforeLevelPlaySoundOverloadResolution() {
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
        assertFalse(migrated.contains("function getAlienEvoSoundSource(soundSource)"));
        assertTrue(migrated.contains(
                "getAlienEvoSoundEvent(\"alienevo:\" + soundId), entity.getSoundSource()"));
        assertFalse(migrated.contains("\"alienevo:\" + soundId, entity.getSoundSource()"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_DIAMOND_CREATION));
    }

    @Test
    void fixesTheExactOmnitrixCycleCrashPathAndIsIdempotent() {
        Map<String, byte[]> resources = new HashMap<>();
        String path = "addon/alienevo/kubejs_scripts/omnitrix_cycle.js";
        put(resources, path, """
                function playCycleSound(entity, soundToPlay, pitch) {
                    entity.level.playSound(
                        null,
                        entity.x,
                        entity.y,
                        entity.z,
                        `alienevo:${soundToPlay}`,
                        entity.getSoundSource(),
                        0.7,
                        pitch
                    );
                }
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String migrated = get(resources, path);
        assertTrue(migrated.contains("getAlienEvoSoundEvent(`alienevo:${soundToPlay}`)"));
        assertTrue(migrated.contains("entity.getSoundSource()"));
        assertFalse(migrated.contains("`alienevo:${soundToPlay}`, entity.getSoundSource()"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, path));
    }

    @Test
    void adaptsStringSoundCategoriesAcrossAlienEvoServerScriptsButLeavesUiPlayerSoundsAlone() {
        Map<String, byte[]> resources = new HashMap<>();
        String fireAbsorbPath = "addon/alienevo/kubejs_scripts/fire_absorb.js";
        put(resources, fireAbsorbPath, """
                entity.level.playSound(null, x + 0.5, y + 0.5, z + 0.5,
                        extinguishSound, "blocks", extinguishSoundVolume, extinguishSoundPitch);
                """);
        String hitPath = "data/alienevo/kubejs_scripts/hit.js";
        put(resources, hitPath, """
                event.level.playSound(null, entity.x, entity.y, entity.z,
                        "minecraft:entity.frog.hurt", "ambient", 1, 1.5);
                """);
        String uiPath = "assets/alienevo/kubejs_scripts/power_ui.js";
        put(resources, uiPath, "player.playSound('minecraft:ui.button.click', 0.33, 1.0);");

        assertEquals(2, PalladiumResourceCompat1201.apply(resources));

        String fireAbsorb = get(resources, fireAbsorbPath);
        assertTrue(fireAbsorb.contains("getAlienEvoSoundEvent(extinguishSound)"));
        assertTrue(fireAbsorb.contains("getAlienEvoSoundSource(\"blocks\")"));
        String hit = get(resources, hitPath);
        assertTrue(hit.contains("getAlienEvoSoundEvent(\"minecraft:entity.frog.hurt\")"));
        assertTrue(hit.contains("getAlienEvoSoundSource(\"ambient\")"));
        assertEquals("player.playSound('minecraft:ui.button.click', 0.33, 1.0);", get(resources, uiPath));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }

    @Test
    void migratesCanonboltBlockParticleArgumentsForLfAndCrLfAndIsIdempotent() {
        String legacyLf = String.join("\n",
                "                    let formats = [",
                "                        \"minecraft:block \" + block.id,        // Standard format",
                "                        \"block \" + block.id,                  // Alternative format",
                "                        \"minecraft:falling_dust \" + block.id, // Falling dust format",
                "                        block.id + \"_break\",                  // Break particle format",
                "                        \"minecraft:item \" + block.id          // Item particle format",
                "                    ];",
                "");

        for (String lineEnding : new String[] {"\n", "\r\n"}) {
            Map<String, byte[]> resources = new HashMap<>();
            put(resources, PalladiumResourceCompat1201.ALIEN_EVO_BLOCK_PARTICLES,
                    legacyLf.replace("\n", lineEnding));

            assertEquals(1, PalladiumResourceCompat1201.apply(resources), "line ending: " + (lineEnding.equals("\r\n") ? "CRLF" : "LF"));
            String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_BLOCK_PARTICLES);
            String normalized = migrated.replace("\r\n", "\n");
            assertTrue(normalized.contains("JSON.stringify(String(block.id))"));
            assertTrue(normalized.contains("`minecraft:block{block_state:${blockState}}`"));
            assertTrue(normalized.contains("`minecraft:falling_dust{block_state:${blockState}}`"));
            assertTrue(normalized.contains("`minecraft:item{item:${blockState}}`"));
            assertFalse(normalized.contains("block.id +"));
            if ("\r\n".equals(lineEnding)) {
                assertFalse(migrated.replace("\r\n", "").contains("\n"),
                        "the migrated CRLF script should not contain bare LF characters");
            }

            assertEquals(0, PalladiumResourceCompat1201.apply(resources));
            assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_BLOCK_PARTICLES));
        }
    }
    @Test
    void migratesLegacySetSecondsOnFireCallsAcrossAlienEvoAndAfOmniScripts() {
        Map<String, byte[]> resources = new HashMap<>();
        String crlf = String.valueOf((char) 13) + (char) 10;
        put(resources, PalladiumResourceCompat1201.AFOMNI_AOE_DAMAGE,
                "targets[j].setSecondsOnFire(firetick);" + crlf);
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_DIRECTIONAL_DAMAGE,
                "targets[j].setSecondsOnFire(firetick);" + crlf);
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_ENTITY_DAMAGE,
                String.join(crlf,
                        "target.setSecondsOnFire(fire_seconds)",
                        "target.setSecondsOnFire(fire_seconds)",
                        ""));
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_INFINITY,
                "entities[i].setSecondsOnFire(fireSeconds);" + crlf);
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_ROLLING_MECHANICS,
                "targets[j].setSecondsOnFire(firetick);" + crlf);

        assertEquals(5, PalladiumResourceCompat1201.apply(resources));

        String aoe = get(resources, PalladiumResourceCompat1201.AFOMNI_AOE_DAMAGE);
        String directional = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_DIRECTIONAL_DAMAGE);
        String targeted = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_ENTITY_DAMAGE);
        String infinity = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_INFINITY);
        String rolling = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_ROLLING_MECHANICS);

        assertTrue(aoe.contains("targets[j].igniteForSeconds(firetick);"));
        assertTrue(directional.contains("targets[j].igniteForSeconds(firetick);"));
        assertEquals(2, occurrences(targeted, "target.igniteForSeconds(fire_seconds)"));
        assertTrue(infinity.contains("entities[i].igniteForSeconds(fireSeconds);"));
        assertTrue(rolling.contains("targets[j].igniteForSeconds(firetick);"));

        for (String migrated : new String[] {aoe, directional, targeted, infinity, rolling}) {
            assertFalse(migrated.contains("setSecondsOnFire"));
            assertFalse(migrated.replace(crlf, "").contains(String.valueOf((char) 10)),
                    "the CRLF source layout should be preserved");
        }

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(aoe, get(resources, PalladiumResourceCompat1201.AFOMNI_AOE_DAMAGE));
        assertEquals(directional, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_DIRECTIONAL_DAMAGE));
        assertEquals(targeted, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_ENTITY_DAMAGE));
        assertEquals(infinity, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_INFINITY));
        assertEquals(rolling, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_ROLLING_MECHANICS));
    }
    @Test
    void normalizesQuickChangePlaylistAndSlotNbtToExplicitIntegerMethods() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLOT_TRANSFORM, String.join("\n",
                "let currentPlaylist = entity.persistentData[CURRENT_PLAYLIST_KEY] || DEFAULT_PLAYLIST;",
                "let alienNumber = entity.persistentData[`alienevo.alien_${currentPlaylist}_${useSlot}`];",
                "entity.persistentData[CURRENT_ALIEN_SLOT_KEY] = useSlot;",
                "let current_playlist = entity.persistentData[CURRENT_PLAYLIST_KEY] || DEFAULT_PLAYLIST;",
                "let current_slot = entity.persistentData[CURRENT_ALIEN_SLOT_KEY] || DEFAULT_SLOT;",
                "let selectedAlienNum = entity.persistentData[`alienevo.alien_${current_playlist}_${current_slot}`] || 0;",
                "alien_num = entity.persistentData[`alienevo.alien_${random_playlist}_${random_slot}`] || 0;",
                "entity.persistentData[CURRENT_PLAYLIST_KEY] = random_playlist;",
                "entity.persistentData[CURRENT_ALIEN_SLOT_KEY] = random_slot;",
                "let alienInSlot = entity.persistentData[`alienevo.alien_${playlist}_${slot}`];",
                ""));

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLOT_TRANSFORM);
        assertEquals(2, occurrences(migrated, "entity.persistentData.getInt(CURRENT_PLAYLIST_KEY)"));
        assertTrue(migrated.contains("entity.persistentData.getInt(CURRENT_ALIEN_SLOT_KEY)"));
        assertTrue(migrated.contains("entity.persistentData.getInt(`alienevo.alien_${currentPlaylist}_${useSlot}`)"));
        assertTrue(migrated.contains("entity.persistentData.getInt(`alienevo.alien_${current_playlist}_${current_slot}`)"));
        assertTrue(migrated.contains("entity.persistentData.getInt(`alienevo.alien_${random_playlist}_${random_slot}`)"));
        assertTrue(migrated.contains("entity.persistentData.getInt(`alienevo.alien_${playlist}_${slot}`)"));
        assertTrue(migrated.contains("entity.persistentData.putInt(CURRENT_ALIEN_SLOT_KEY, useSlot)"));
        assertTrue(migrated.contains("entity.persistentData.putInt(CURRENT_PLAYLIST_KEY, random_playlist)"));
        assertTrue(migrated.contains("entity.persistentData.putInt(CURRENT_ALIEN_SLOT_KEY, random_slot)"));
        assertFalse(migrated.contains("entity.persistentData[CURRENT_PLAYLIST_KEY]"));
        assertFalse(migrated.contains("entity.persistentData[CURRENT_ALIEN_SLOT_KEY]"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLOT_TRANSFORM));
    }

    @Test
    void migratesPlaylistCycleNbtAccessToExplicitIntegerMethods() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_PLAYLIST_CYCLE, String.join("\n",
                "let MAX_PLAYLISTS = 100;",
                "let currentPlaylist = entity.persistentData[CURRENT_PLAYLIST_KEY] || DEFAULT_PLAYLIST;",
                "let currentSlot = entity.persistentData[CURRENT_ALIEN_SLOT_KEY] || DEFAULT_SLOT;",
                "let alienNumber = entity.persistentData[`alienevo.alien_${currentPlaylist}_${slot}`];",
                "entity.persistentData[CURRENT_PLAYLIST_KEY] = currentPlaylist;",
                "entity.persistentData[CURRENT_ALIEN_SLOT_KEY] = slot;",
                "let alienType = entity.persistentData.getInt(`alienevo.alien_${playlist}_${slot}`) || 0;",
                ""));

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_PLAYLIST_CYCLE);
        assertTrue(migrated.contains("let MAX_PLAYLISTS = 10;"));
        assertTrue(migrated.contains(
                "let currentPlaylist = entity.persistentData.getInt(CURRENT_PLAYLIST_KEY) || DEFAULT_PLAYLIST;"));
        assertTrue(migrated.contains(
                "let currentSlot = entity.persistentData.getInt(CURRENT_ALIEN_SLOT_KEY) || DEFAULT_SLOT;"));
        assertTrue(migrated.contains(
                "entity.persistentData.getInt(`alienevo.alien_${currentPlaylist}_${slot}`)"));
        assertTrue(migrated.contains("entity.persistentData.putInt(CURRENT_PLAYLIST_KEY, currentPlaylist);"));
        assertTrue(migrated.contains("entity.persistentData.putInt(CURRENT_ALIEN_SLOT_KEY, slot);"));
        assertFalse(migrated.contains("entity.persistentData[CURRENT_PLAYLIST_KEY]"));
        assertFalse(migrated.contains("entity.persistentData[CURRENT_ALIEN_SLOT_KEY]"));
        assertFalse(migrated.contains("entity.persistentData[`alienevo.alien_"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_PLAYLIST_CYCLE));
    }

    @Test
    void migratesAlienEvoNetworkDataReceivedHandlersToKubeJs7Player() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_STUFF, String.join("\n",
                "NetworkEvents.dataReceived('AlienEvo.isJumping', event => {",
                "    let entity = event.entity;",
                "});",
                "NetworkEvents.dataReceived('AlienEvo.notJumping', event => {",
                "    let entity = event.entity;",
                "});",
                "NetworkEvents.dataReceived('saveBeamColorData', event => {",
                "    let player = event.player;",
                "});",
                "NetworkEvents.dataReceived('saveEnergyColorData', event => {",
                "    let player = event.player;",
                "});",
                "NetworkEvents.dataReceived('savePaletteData', event => {",
                "    let player = event.entity;",
                "});",
                "NetworkEvents.dataReceived('alienevo_nickname', event => {",
                "    let player = event.entity;",
                "});",
                ""));

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_STUFF);
        assertEquals(6, occurrences(migrated, "event.player"));
        assertFalse(migrated.contains("event.entity"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_STUFF));
    }

    @Test
    void readsOmnitrixPalettePacketAsCompoundTagEntries() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_STUFF, String.join("\r\n",
                "NetworkEvents.dataReceived('saveBeamColorData', event => {",
                "    let player = event.player;",
                "    let data = event.data;",
                "    let colorValue = data.value;",
                "});",
                "NetworkEvents.dataReceived('saveEnergyColorData', event => {",
                "    let player = event.player;",
                "    let data = event.data;",
                "    let energyColor = data.value;",
                "});",
                "NetworkEvents.dataReceived('savePaletteData', event => {",
                "    let player = event.entity;",
                "    let propertiesToSave = event.data;",
                "    for (let propertyName in propertiesToSave) {",
                "        palladium.setProperty(player, propertyName, propertiesToSave[propertyName]);",
                "    }",
                "});",
                "NetworkEvents.dataReceived('alienevo_nickname', event => {",
                "    let player = event.entity;",
                "    let inputText = event.data.nickname;",
                "});",
                ""));

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_STUFF);
        assertTrue(migrated.contains("let propertyIterator = propertiesToSave.getAllKeys().iterator();"));
        assertTrue(migrated.contains("while (propertyIterator.hasNext()) {"));
        assertTrue(migrated.contains("let propertyName = propertyIterator.next();"));
        assertTrue(migrated.contains(
                "palladium.setProperty(player, propertyName, legacyConnectorAlienEvoPacketString(propertiesToSave.get(propertyName)));"));
        assertFalse(migrated.contains("for (let propertyName in propertiesToSave)"));
        assertFalse(migrated.contains("propertiesToSave[propertyName]"));
        assertTrue(migrated.contains("\r\n"));
        assertFalse(migrated.replace("\r\n", "").contains("\n"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_STUFF));
    }

    @Test
    void migratesHeatblastFireAbsorbArrayBlockLookupsToCoordinateOverload() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_FIRE_ABSORB, String.join("\n",
                "let blockPos = [x, y, z];",
                "let currentBlock = entity.level.getBlock(blockPos);",
                "let blockPos = [x, y, z];",
                "let block = entity.level.getBlock(blockPos);",
                "if (!entity.persistentData.fireBlocks) {",
                "entity.persistentData.fireBlocks = [];",
                "entity.persistentData.fireBlocks = fireBlocks;",
                "let fireBlocks = entity.persistentData.fireBlocks;",
                "entity.persistentData.lastFireSearch = 0;",
                "if (currentTime - entity.persistentData.lastFireSearch >= searchInterval) {",
                "entity.persistentData.lastFireSearch = currentTime;",
                "if (!entity.persistentData.trackedFireBlocks) {",
                "entity.persistentData.trackedFireBlocks = {};",
                "if (extinguishFire && entity.persistentData.trackedFireBlocks) {",
                "for (let blockKey in entity.persistentData.trackedFireBlocks) {",
                "let blockData = entity.persistentData.trackedFireBlocks[blockKey];",
                "delete entity.persistentData.trackedFireBlocks[blockKey];",
                "if (extinguishFire && entity.persistentData.trackedFireBlocks) {",
                "if (!entity.persistentData.trackedFireBlocks[blockKey]) {",
                "entity.persistentData.trackedFireBlocks[blockKey] = {",
                "let trackedCount = entity.persistentData.trackedFireBlocks ? Object.keys(entity.persistentData.trackedFireBlocks).length : 0;",
                "if (entity.isPlayer() && entity.persistentData.trackedFireBlocks) {",
                "let trackedCount = Object.keys(entity.persistentData.trackedFireBlocks).length;",
                "entity.persistentData.trackedFireBlocks = {};",
                ""));

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_FIRE_ABSORB);
        assertEquals(2, occurrences(migrated, "entity.level.getBlock(x, y, z);"));
        assertFalse(migrated.contains("entity.level.getBlock(blockPos)"));
        assertTrue(migrated.contains("entity.data.put('fireBlocks', []);"));
        assertTrue(migrated.contains("let fireBlocks = entity.data.get('fireBlocks');"));
        assertTrue(migrated.contains("entity.data.put('lastFireSearch', currentTime);"));
        assertTrue(migrated.contains("entity.data.get('lastFireSearch')"));
        assertEquals(2, occurrences(migrated, "entity.data.put('trackedFireBlocks', {});"));
        assertTrue(migrated.contains("entity.data.get('trackedFireBlocks')[blockKey]"));
        assertFalse(migrated.contains("entity.persistentData.fireBlocks"));
        assertFalse(migrated.contains("entity.persistentData.lastFireSearch"));
        assertFalse(migrated.contains("entity.persistentData.trackedFireBlocks"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_FIRE_ABSORB));
    }

    @Test
    void preservesCurrentAlienIdWhenDecouplingAndRebondingOmnitrix() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, "addon/alienevo/kubejs_scripts/decouple.js",
                "storedCurrentAlienSlot: currentAlienSlot,\n"
                        + "storedOmnitrixCycle: currentAlienSlot,\n"
                        + "    getDecoupleSoundId(watchPrefix),\n"
                        + "var watchItem = Item.of(itemId, 1, nbtData);\n"
                        + "var nbt = mainHandItem.nbt;\n"
                        + "if (!nbt) {\n"
                        + "var storedAllDNAString = nbt.storedAllDNA || '';\n"
                        + "var storedCurrentPlaylist = nbt.storedCurrentPlaylist || 1;\n"
                        + "if (nbt.storedMastercontrol) entity.tags.add('AlienEvo.MasterControl');\n"
                        + "if (typeof nbt.storedGlowColor1 === 'string') writeGlowHex(entity, 1, nbt.storedGlowColor1, wgPrefix);\n"
                        + "var displayNbt = nbt.display;\n"
                        + "if (displayNbt && displayNbt.Name) {\n"
                        + "itemName = displayNbt.Name;\n"
                        + "palladium.setProperty(entity, OMNITRIX_PROPERTY_KEY, storedOmnitrixCycle);\n");

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, "addon/alienevo/kubejs_scripts/decouple.js");
        assertTrue(migrated.contains(
                "storedOmnitrixCycle: palladium.getProperty(entity, OMNITRIX_PROPERTY_KEY),"));
        assertTrue(migrated.contains("storedCurrentAlienSlot: currentAlienSlot,"));
        assertTrue(migrated.contains("getAlienEvoSoundEvent(getDecoupleSoundId(watchPrefix)),"));
        assertTrue(migrated.contains("var watchItem = Item.of(itemId, 1);"));
        assertTrue(migrated.contains("watchItem.setCustomData(NBT.wrapCompound(nbtData));"));
        assertFalse(migrated.contains("watchItem.setNbt(nbtData);"));
        assertTrue(migrated.contains("var nbt = mainHandItem.getCustomData();"));
        assertTrue(migrated.contains("if (nbt.isEmpty()) {"));
        assertTrue(migrated.contains("var storedAllDNAString = nbt.getString('storedAllDNA');"));
        assertTrue(migrated.contains("nbt.getInt('storedCurrentPlaylist') || 1"));
        assertTrue(migrated.contains("nbt.getBoolean('storedMastercontrol')"));
        assertTrue(migrated.contains("nbt.getString('storedGlowColor1')"));
        assertTrue(migrated.contains("displayNbt.getString('Name')"));
        assertFalse(migrated.contains("storedOmnitrixCycle: currentAlienSlot"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, "addon/alienevo/kubejs_scripts/decouple.js"));
    }

    @Test
    void migratesOmnitrixWorkbenchItemCustomDataAndPreservesSolidLayoutScores() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, "data/alienevo/kubejs_scripts/workbench.js", String.join("\n",
                "run summon minecraft:item ~ ~ ~ {Item:{id:\"alienevo:prototype_omnitrix\",Count:1b,tag:${JSON.stringify(nbtData)" + "}},PickupDelay:10}",
                "omnitrix.setNbt(nbtData);",
                "var nbt = mainHandItem.nbt;",
                "if (nbt && nbt.display && nbt.display.Name) {",
                "event.target.persistentData.putString('storedItemName', nbt.display.Name);",
                "var storedDNA = nbt && nbt.storedAllDNA ? nbt.storedAllDNA : '';",
                "let storedCode = nbt && nbt.storedCode ? nbt.storedCode : '000000';",
                "if (nbt && nbt.storedCode)          palladium.setProperty(armorStand, 'code', nbt.storedCode);",
                "let storedPosition = nbt && nbt.storedPosition ? nbt.storedPosition : '0';",
                "if (nbt && nbt.storedPosition && !isNaN(parseInt(nbt.storedPosition))) positionValue = parseInt(nbt.storedPosition);",
                "let storedMasterTargetCode = nbt && nbt.storedMasterTargetCode ? nbt.storedMasterTargetCode : '';",
                "let storedSelfDestructTargetCode = nbt && nbt.storedSelfDestructTargetCode ? nbt.storedSelfDestructTargetCode : '';",
                "if (nbt && nbt.storedAlienNamespaces) {",
                "event.target.persistentData.putString('storedAlienNamespaces', nbt.storedAlienNamespaces);",
                "let storedCurrentPlaylist = nbt && nbt.storedCurrentPlaylist ? nbt.storedCurrentPlaylist : 1;",
                "let storedCurrentAlienSlot = nbt && nbt.storedCurrentAlienSlot ? nbt.storedCurrentAlienSlot : 1;",
                "if (nbt && nbt.storedOmnitrixCycle) palladium.setProperty(armorStand, 'omnitrix_cycle', nbt.storedOmnitrixCycle);",
                "if (nbt && nbt.storedOmnitrixCycle) event.target.persistentData.putInt('storedOmnitrixCycle', nbt.storedOmnitrixCycle);",
                "if (typeof nbt?.storedUniformGlowColorBase === 'string') palladium.setProperty(armorStand, 'uniform_glow_color_base', nbt.storedUniformGlowColorBase);",
                "if (typeof nbt?.storedUniformGlowColorBase === 'string') event.target.persistentData.putString('uniform_glow_color_base', nbt.storedUniformGlowColorBase);",
                "if (typeof nbt?.storedUniformGlowColor1   === 'string') palladium.setProperty(armorStand, 'uniform_glow_color_1', nbt.storedUniformGlowColor1);",
                "if (typeof nbt?.storedUniformGlowColor1   === 'string') event.target.persistentData.putString('uniform_glow_color_1', nbt.storedUniformGlowColor1);",
                "if (typeof nbt?.storedUniformGlowColor2   === 'string') palladium.setProperty(armorStand, 'uniform_glow_color_2', nbt.storedUniformGlowColor2);",
                "if (typeof nbt?.storedUniformGlowColor2   === 'string') event.target.persistentData.putString('uniform_glow_color_2', nbt.storedUniformGlowColor2);",
                "if (typeof nbt?.storedUniformGlowColor3   === 'string') palladium.setProperty(armorStand, 'uniform_glow_color_3', nbt.storedUniformGlowColor3);",
                "if (typeof nbt?.storedUniformGlowColor3   === 'string') event.target.persistentData.putString('uniform_glow_color_3', nbt.storedUniformGlowColor3);",
                "if (typeof nbt?.storedUniformGlowColor4   === 'string') palladium.setProperty(armorStand, 'uniform_glow_color_4', nbt.storedUniformGlowColor4);",
                "if (typeof nbt?.storedUniformGlowColor4   === 'string') event.target.persistentData.putString('uniform_glow_color_4', nbt.storedUniformGlowColor4);",
                "if (typeof nbt?.storedUniformGlowColor5   === 'string') palladium.setProperty(armorStand, 'uniform_glow_color_5', nbt.storedUniformGlowColor5);",
                "if (typeof nbt?.storedUniformGlowColor5   === 'string') event.target.persistentData.putString('uniform_glow_color_5', nbt.storedUniformGlowColor5);",
                "if (typeof nbt?.storedGlowColorBase === 'string') palladium.setProperty(armorStand, 'prototype_glow_color_base', nbt.storedGlowColorBase);",
                "if (typeof nbt?.storedGlowColorBase === 'string') event.target.persistentData.putString('prototype_glow_color_base', nbt.storedGlowColorBase);",
                "if (typeof nbt?.storedGlowColor1    === 'string') palladium.setProperty(armorStand, 'prototype_glow_color_1', nbt.storedGlowColor1);",
                "if (typeof nbt?.storedGlowColor1    === 'string') event.target.persistentData.putString('prototype_glow_color_1', nbt.storedGlowColor1);",
                "if (typeof nbt?.storedGlowColor2    === 'string') palladium.setProperty(armorStand, 'prototype_glow_color_2', nbt.storedGlowColor2);",
                "if (typeof nbt?.storedGlowColor2    === 'string') event.target.persistentData.putString('prototype_glow_color_2', nbt.storedGlowColor2);",
                "if (typeof nbt?.storedGlowColor3    === 'string') palladium.setProperty(armorStand, 'prototype_glow_color_3', nbt.storedGlowColor3);",
                "if (typeof nbt?.storedGlowColor3    === 'string') event.target.persistentData.putString('prototype_glow_color_3', nbt.storedGlowColor3);",
                "if (typeof nbt?.storedGlowColor4    === 'string') palladium.setProperty(armorStand, 'prototype_glow_color_4', nbt.storedGlowColor4);",
                "if (typeof nbt?.storedGlowColor4    === 'string') event.target.persistentData.putString('prototype_glow_color_4', nbt.storedGlowColor4);",
                "if (typeof nbt?.storedGlowColor5    === 'string') palladium.setProperty(armorStand, 'prototype_glow_color_5', nbt.storedGlowColor5);",
                "if (typeof nbt?.storedGlowColor5    === 'string') event.target.persistentData.putString('prototype_glow_color_5', nbt.storedGlowColor5);",
                "if (typeof nbt?.storedAlienGlowProps === 'string' && nbt.storedAlienGlowProps.length) {",
                "var parsedAlienGlow = JSON.parse(nbt.storedAlienGlowProps);",
                "if (typeof nbt?.storedAlienGlowProps === 'string') {",
                "event.target.persistentData.putString('storedAlienGlowProps', nbt.storedAlienGlowProps);",
                "if (!nbt && mainHandItem.id.includes('_10k')) {",
                "if (nbt && nbt.storedMastercontrol) {",
                "function getStoredScore(nbtTag, defaultValue) {",
                "return (nbt && nbt.contains && nbt.contains(nbtTag)) ? nbt.getInt(nbtTag) : defaultValue;",
                "}",
                ""));
        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, "data/alienevo/kubejs_scripts/workbench.js");
        assertTrue(migrated.contains("count:1,components:{\"minecraft:custom_data\":"));
        assertTrue(migrated.contains("JSON.stringify(nbtData)}}},PickupDelay:10"));
        assertTrue(migrated.contains("omnitrix.setCustomData(NBT.wrapCompound(nbtData));"));
        assertTrue(migrated.contains("var nbt = mainHandItem.getCustomData();"));
        assertTrue(migrated.contains("function getStoredScore(nbtTag, defaultValue)"));
        assertTrue(migrated.contains("nbt.getInt(nbtTag)"));
        assertTrue(migrated.contains("nbt.getString('storedGlowColor1')"));
        assertTrue(migrated.contains("nbt.getString('storedUniformGlowColor1')"));
        assertTrue(migrated.contains("nbt.getInt('storedMastercontrol') !== 0"));
        assertTrue(migrated.contains("if (nbt.isEmpty() && mainHandItem.id.includes('_10k'))"));
        assertFalse(migrated.contains("mainHandItem.nbt"));
        assertFalse(migrated.contains("omnitrix.setNbt(nbtData);"));
        assertFalse(migrated.contains("Count:1b,tag:"));
        assertFalse(migrated.contains("nbt.storedGlowColor1"));
        assertFalse(migrated.contains("nbt.storedUniformGlowColor1"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, "data/alienevo/kubejs_scripts/workbench.js"));
    }

    @Test
    void makesAlienEvoGlidePitchLengthMutableBeforeAdjustment() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_GLIDE, String.join("\n",
                "const pitchLength = JavaMath.cos(pitchRad)",
                "pitchLength = pitchLength * pitchLength * JavaMath.min(1, lookLength / 0.4)",
                ""));

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_GLIDE);
        assertTrue(migrated.contains("let pitchLength = JavaMath.cos(pitchRad)"));
        assertFalse(migrated.contains("const pitchLength = JavaMath.cos(pitchRad)"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_GLIDE));
    }

    @Test
    void migratesCannonboltSlamLandingTrackingAndPreservesCrLf() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLAM, String.join("\r\n",
                "let playersWithGravity = new Set();",
                "",
                "PlayerEvents.tick(event => {",
                "    let p = event.player",
                "    if (abilityEnabled && !playersWithGravity.has(p.uuid.toString())) {",
                "        p.modifyAttribute(gravityAttribute, \"gravity_slam\", 6, \"addition\")",
                "        p.modifyAttribute(gravityAttribute, \"gravity_slam\", 6, \"addition\")",
                "        playersWithGravity.add(p.uuid.toString());",
                "    }",
                "    if (!abilityEnabled && playersWithGravity.has(p.uuid.toString())) {",
                "        playersWithGravity.delete(p.uuid.toString());",
                "    }",
                "    if (p.getDeltaMovement().get(\"y\") < -5 && p.onGround()) {",
                "        p.removeAttribute(gravityAttribute, \"gravity_slam\")",
                "    }",
                "})"));

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLAM);
        assertTrue(migrated.contains("let playersFalling = new Set();"));
        assertTrue(migrated.contains("const clearGravitySlam = (player, playerId) => {"));
        assertTrue(migrated.contains("if (p.onGround() && playersFalling.has(playerId))"));
        assertTrue(migrated.contains("if (p.onGround() || isAtLeast10BlocksAboveGround(p)) {"));
        assertFalse(migrated.contains("canStart"));
        assertTrue(migrated.contains("else if (activeTicks >= 200)"));
        assertTrue(migrated.contains("let slamLastY = new Map();"));
        assertTrue(migrated.contains("const previousY = slamLastY.get(playerId);"));
        assertTrue(migrated.contains("damageSlamImpact(p, x, y, z, 6.0, 15.0);"));
        assertTrue(migrated.contains("target.attack(player.damageSources().generic(), damageAmount);"));
        assertFalse(migrated.contains("getDeltaMovement().get(\"y\")"));
        assertTrue(migrated.contains(
                "p.modifyAttribute(gravityAttribute, \"gravity_slam\", 6, \"add_value\")"));
        assertFalse(migrated.contains("\"addition\""));
        assertFalse(migrated.contains("if (!abilityEnabled && playersWithGravity.has"));
        assertFalse(migrated.contains("get(\"y\") < -5"));
        assertTrue(migrated.contains("\r\n"));
        assertFalse(migrated.replace("\r\n", "").contains("\n"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLAM));
    }

    @Test
    void preservesOmnitrixLayoutScoresWhenRemovingAndReapplyingPrototypeCore() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_CORE_REMOVAL, String.join("\n",
                "function createAndGivePrototypeCore(entity) {",
                "  var username = entity.getGameProfile().getName();",
                "",
                "  var currentPlaylist = entity.persistentData.getInt(CURRENT_PLAYLIST_KEY) || 1;",
                "  var nbtData = {",
                "    storedMastercontrol: hasMastercontrol,",
                "    storedWatch: watchPrefix",
                "  };",
                "  var coreItem = Item.of('alienevo:prototype_core', 1, nbtData);",
                "  entity.give(coreItem);",
                "}",
                "function applyAllDNAFromPrototypeCore(entity) {",
                "  var mainHandItem = entity.getMainHandItem();",
                "  var username = entity.getGameProfile().getName();",
                "  var nbt = mainHandItem.nbt;",
                "  if (nbt && nbt.display && nbt.display.Name) {",
                "    entity.persistentData.putString('storedItemName', nbt.display.Name);",
                "  }",
                "  if (!nbt) {",
                "    return;",
                "  }",
                "  if (typeof nbt.storedWatch === 'string' && nbt.storedWatch) {",
                "    palladium.setProperty(entity, 'watch', nbt.storedWatch);",
                "  }",
                "  var watchPrefix = (typeof nbt.storedWatch === 'string' && nbt.storedWatch) ? nbt.storedWatch : getWatchPrefix(entity);",
                "  var storedAllDNAString = nbt.storedAllDNA || '';",
                "  if (nbt.storedMastercontrol) entity.tags.add('AlienEvo.MasterControl');",
                "",
                "  // Restore glow props",
                "  if (typeof nbt.storedGlowColor1 === 'string') writeGlowHex(entity, 1, nbt.storedGlowColor1, wgPrefix);",
                "}"));

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_CORE_REMOVAL);
        assertTrue(migrated.contains("readOmnitrixLayoutScore('AlienEvo.BasePrimary')"));
        assertTrue(migrated.contains("storedBasePrimaryScore: basePrimaryScore"));
        assertTrue(migrated.contains("storedBaseSecondaryScore: baseSecondaryScore"));
        assertTrue(migrated.contains("storedButtonsScore: buttonsScore"));
        assertTrue(migrated.contains("stored10kPhaseScore: phase10kScore"));
        assertTrue(migrated.contains(
                "restoreOmnitrixLayoutScore('AlienEvo.BasePrimary', 'storedBasePrimaryScore', -1)"));
        assertTrue(migrated.contains(
                "restoreOmnitrixLayoutScore('AlienEvo.BaseSecondary', 'storedBaseSecondaryScore', -1)"));
        assertTrue(migrated.contains("restoreOmnitrixLayoutScore('AlienEvo.10kStage', 'stored10kStageScore', 0)"));
        assertTrue(migrated.contains("coreItem.setCustomData(NBT.wrapCompound(nbtData));"));
        assertFalse(migrated.contains("coreItem.setNbt(nbtData);"));
        assertTrue(migrated.contains("var nbt = mainHandItem.getCustomData();"));
        assertTrue(migrated.contains("if (nbt.isEmpty()) {"));
        assertTrue(migrated.contains("if (nbt.contains('storedWatch') && nbt.getString('storedWatch'))"));
        assertTrue(migrated.contains("nbt.getBoolean('storedMastercontrol')"));
        assertTrue(migrated.contains("nbt.getString('storedGlowColor1')"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_CORE_REMOVAL));
    }

    @Test
    void migratesAlienEvoSlamAttributeOperationToCurrentEnumName() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLAM, String.join("\n",
                "p.modifyAttribute(gravityAttribute, \"gravity_slam\", 6, \"addition\")",
                "p.modifyAttribute(gravityAttribute, \"gravity_slam\", 6, \"addition\")",
                ""));

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLAM);
        assertEquals(2, occurrences(migrated,
                "p.modifyAttribute(gravityAttribute, \"gravity_slam\", 6, \"add_value\")"));
        assertFalse(migrated.contains("\"addition\""));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLAM));
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

        // This fixture intentionally contains only the scoreboard portion of stat_ability.js.
        // The Attribute_Mod enum migration must leave unrelated partial script fixtures alone.
        String statAbility = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_STAT_ABILITY);
        assertEquals(2, occurrences(statAbility, "entity.server.scoreboard"));
        assertFalse(statAbility.contains("legacyConnectorAttributeOperation"));

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
                entity.level.playSound(null, entity.x, entity.y, entity.z,
                        `alienevo:${watchType}_detransform`, entity.getSoundSource(), 1.0, 1.0);
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
        assertTrue(detransform.contains(
                "getAlienEvoSoundEvent('alienevo:' + String(watchType) + '_detransform')"));
        assertFalse(detransform.contains("`alienevo:${watchType}_detransform`"));

        String rolling = get(resources, "addon/alienevo/kubejs_scripts/rolling_mechanics.js");
        assertEquals(1, occurrences(rolling, "entity.server.scoreboard"));
        assertFalse(rolling.contains("Utils.server"));

        String xp = get(resources, "data/alienevo/kubejs_scripts/xp.js");
        assertEquals(1, occurrences(xp, "player.server.scoreboard"));
        assertFalse(xp.contains("Utils.server"));
        assertTrue(xp.contains("let player = source ? source.getActual() : null;"));
        assertTrue(xp.contains("if (player && player.isPlayer()) {"));
        assertFalse(xp.contains("source.getPlayer()"));
        assertFalse(xp.contains("source.player"));

        String level = get(resources, "data/alienevo/kubejs_scripts/level_command.js");
        assertFalse(level.contains("Utils.server"));
        assertTrue(level.contains("function maxAllAliens(playerName, server)"));
        assertTrue(level.contains("function giveAlienControl(playerName, server)"));
        assertTrue(level.contains("maxAllAliens(username, target.server)"));
        assertTrue(level.contains("return giveAlienControl(username, target.server)"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }



    @Test
    void detransformRestoresWatchStateWhenNoCooldownRemainsAndIsIdempotent() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, "addon/alienevo/kubejs_scripts/detransform.js", """
                if (!hasMasterControl) {
                    palladium.setProperty(entity, 'watch_state', "timeout");
                }
                if (!hasMasterControl) {
                    let scoreboardManager = entity.server.scoreboard;
                    let timerObjective = scoreboardManager.getObjective('AlienEvo.Timer');
                    let playerTimerScore = scoreboardManager.getOrCreatePlayerScore(playerUsername, timerObjective);
                    let currentTimerValue = playerTimerScore.getScore();
                    entity.server.runCommandSilent(`execute as ${playerUsername} at @s run scoreboard players set @s AlienEvo.Timer ${Math.floor(currentTimerValue / 2)}`);
                    entity.level.playSound(null, entity.x, entity.y, entity.z,
                            `alienevo:${watchType}_detransform`, entity.getSoundSource(), 1.0, 1.0);
                }
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String detransform = get(resources, "addon/alienevo/kubejs_scripts/detransform.js");
        assertTrue(detransform.contains("if (timerObjective != null) {"));
        assertTrue(detransform.contains("if (currentTimerValue <= 1) {"));
        assertTrue(detransform.contains(
                "palladium.setProperty(entity, 'watch_state', \"default\");"));
        assertTrue(detransform.contains("} else {"));
        assertEquals(2, occurrences(detransform,
                "palladium.setProperty(entity, 'watch_state', \"default\");"));
        assertTrue(detransform.contains(
                "Math.floor(currentTimerValue / 2)"));
        assertFalse(detransform.contains("getScore()"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }

    @Test
    void rechargeClearsTheFinalOneTickCooldownAndIsIdempotent() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_RECHARGE, """
                let currentTime = palladium.scoreboard.getScore(entity, TIMER_OBJECTIVE, 0);
                if (currentTime > 1) {
                    palladium.scoreboard.subtractScore(entity, TIMER_OBJECTIVE, RECHARGE_RATE);
                    let newTime = palladium.scoreboard.getScore(entity, TIMER_OBJECTIVE, 0);
                    if (newTime <= 1) {
                        palladium.scoreboard.setScore(entity, TIMER_OBJECTIVE, 0);
                        palladium.setProperty(entity, 'watch_state', "default");
                    }
                }
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String recharge = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_RECHARGE);
        assertTrue(recharge.contains("if (currentTime > 0) {"));
        assertFalse(recharge.contains("if (currentTime > 1) {"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }

    @Test
    void migratesAlienEvoStatAbilityServerAccessAndIsIdempotent() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_STAT_ABILITY, """
                StartupEvents.registry('palladium:abilities', event => {
                    event.create('alienevo:stat_ability')
                        .addProperty("Attribute_Mod", "string", "addition", "addition , multiply_total , multiply_base");
                });
                .lastTick((entity, entry, holder, enabled) => {
                    const scoreboard = Utils.server.scoreboard;
                    const uuid = entry.getPropertyByName('UUID');
                    const attribute_mod = entry.getPropertyByName('Attribute_Mod');
                    entity.removeAttribute(attribute, UUID);
                });
                .tick((entity, entry, holder, enabled) => {
                    const scoreboard = Utils.server.scoreboard;
                    const uuid = entry.getPropertyByName('UUID');
                    const attribute_mod = entry.getPropertyByName('Attribute_Mod');
                    entity.modifyAttribute(attribute, UUID, (value / division), attribute_mod);
                });
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_STAT_ABILITY);
        assertEquals(2, occurrences(migrated, "entity.server.scoreboard"));
        assertFalse(migrated.contains("Utils.server.scoreboard"));
        String modifierId = "'alienevo:stat_ability/' + String(uuid).toLowerCase().replace(/[^a-z0-9/._-]/g, '_')";
        assertTrue(migrated.contains("entity.removeAttribute(attribute, " + modifierId + ")"));
        assertTrue(migrated.contains("entity.modifyAttribute(attribute, " + modifierId + ","));
        assertFalse(migrated.contains("entity.removeAttribute(attribute, UUID)"));
        assertFalse(migrated.contains("entity.modifyAttribute(attribute, UUID,"));
        assertFalse(migrated.contains("entity.removeAttribute(attribute, uuid)"));
        assertFalse(migrated.contains("entity.modifyAttribute(attribute, uuid,"));
        assertTrue(migrated.contains("function legacyConnectorAttributeOperation(operation)"));
        assertTrue(migrated.contains("case \"addition\": return \"add_value\";"));
        assertTrue(migrated.contains("case \"multiply_base\": return \"add_multiplied_base\";"));
        assertTrue(migrated.contains("case \"multiply_total\": return \"add_multiplied_total\";"));
        assertEquals(2, occurrences(migrated,
                "const attribute_mod = legacyConnectorAttributeOperation(entry.getPropertyByName('Attribute_Mod'));"));
        assertTrue(migrated.contains(
                ".addProperty(\"Attribute_Mod\", \"string\", \"add_value\", \"add_value , add_multiplied_total , add_multiplied_base\")"));
        assertFalse(migrated.contains("case \"addition\": return \"addition\";"));
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
                    let data = event.data;
                    let colorValue = data.value;
                    palladium.setProperty(player, 'galvanic_mechamorph_beam_color', colorValue);
                });
                NetworkEvents.dataReceived('saveEnergyColorData', event => {
                    let player = event.player;
                    let data = event.data;
                    let energyColor = data.value;
                    palladium.setProperty(player, 'alien_energy_color', energyColor);
                });
                NetworkEvents.dataReceived('savePaletteData', event => {
                    let player = event.entity;
                    let propertiesToSave = event.data;
                    for (let propertyName in propertiesToSave) {
                        palladium.setProperty(player, propertyName, propertiesToSave[propertyName]);
                    }
                });
                NetworkEvents.dataReceived('alienevo_nickname', event => {
                    let player = event.entity;
                    let inputText = event.data.nickname;
                    player.persistentData.putString('nickname', inputText);
                    palladium.setProperty(player, 'current_alien_nickname', inputText);
                });
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_STUFF);
        assertEquals(4, occurrences(migrated, "let player = event.player;"));
        assertFalse(migrated.contains("let player = event.entity;"));
        assertTrue(migrated.startsWith("function legacyConnectorAlienEvoPacketString(value) {"));
        assertTrue(migrated.contains("legacyConnectorAlienEvoPacketString(data.value)"));
        assertTrue(migrated.contains("let propertyIterator = propertiesToSave.getAllKeys().iterator();"));
        assertTrue(migrated.contains("let propertyName = propertyIterator.next();"));
        assertTrue(migrated.contains("legacyConnectorAlienEvoPacketString(propertiesToSave.get(propertyName))"));
        assertFalse(migrated.contains("propertiesToSave[propertyName]"));
        assertTrue(migrated.contains("legacyConnectorAlienEvoPacketString(event.data.nickname)"));
        assertFalse(migrated.contains("let colorValue = data.value;"));
        assertFalse(migrated.contains("let energyColor = data.value;"));
        assertFalse(migrated.contains("let inputText = event.data.nickname;"));
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
    void migratesAlienEvoCycleSoundsOnTheActualResourcePathAndIsIdempotent() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, "addon/alienevo/kubejs_scripts/cycle_sounds.js", """
                entity.level.playSound(null, entity.x, entity.y, entity.z,
                        `alienevo:${soundToPlay}`, entity.getSoundSource(), soundVolume, soundPitch);
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String migrated = get(resources, "addon/alienevo/kubejs_scripts/cycle_sounds.js");
        assertTrue(migrated.contains("function getAlienEvoSoundEvent(soundId)"));
        assertTrue(migrated.contains("getAlienEvoSoundEvent('alienevo:' + String(soundToPlay))"));
        assertFalse(migrated.contains("`alienevo:${soundToPlay}`"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, "addon/alienevo/kubejs_scripts/cycle_sounds.js"));
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
    void addsMissingMachineScrollItemTagWithoutOverwritingExistingTags() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, "data/alienevo/advancements/obtain_scroll.json",
                "{\"criteria\":{\"machine_scroll\":{\"conditions\":{\"items\":[{\"tag\": \"alienevo:machine_scroll\"}]}}}}");
        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String tagPath = "data/alienevo/tags/item/machine_scroll.json";
        assertTrue(get(resources, tagPath).contains("\"alienevo:machine_scroll\""));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));

        Map<String, byte[]> existing = new HashMap<>();
        put(existing, "data/alienevo/advancements/obtain_scroll.json",
                "{\"criteria\":{\"machine_scroll\":{\"conditions\":{\"items\":[{\"tag\": \"alienevo:machine_scroll\"}]}}}}");
        put(existing, tagPath, "{\"replace\":true,\"values\":[\"example:other\"]}");
        assertEquals(0, PalladiumResourceCompat1201.apply(existing));
        assertTrue(get(existing, tagPath).contains("example:other"));
    }

    @Test
    void repairsAlienEvoGeckoLibAnimationFileNamesAndIsIdempotent() {
        Map<String, byte[]> resources = new HashMap<>();
        byte[] humongousaurAnimation = "{\"animations\":{\"animation.humungousaur.tail\":{}}}"
                .getBytes(StandardCharsets.UTF_8);
        byte[] xlr8Animation = "{\"animations\":{\"animation.xlr8.tail\":{}}}"
                .getBytes(StandardCharsets.UTF_8);
        resources.put("assets/afomni/animations/model.humongousaur.anim.json", humongousaurAnimation);
        resources.put("assets/alienevo/animations/xlr8.json", xlr8Animation);
        byte[] crystalsapienAnimation = "{\"animations\":{\"animation.Chromastone.arms\":{}}}"
                .getBytes(StandardCharsets.UTF_8);
        resources.put("assets/afomni/animations/crystalsapien.animation.json", crystalsapienAnimation);
        resources.put("assets/afomni/animations/crystalsapien.animation_copy.json", crystalsapienAnimation.clone());
        put(resources, "assets/afomni/palladium/render_layers/vaxasaurian.json",
                "{\"animation_file\":\"afomni:animations/model.humongousaur.anim.json\"}");
        put(resources, "assets/alienevo/palladium/render_layers/aliens/alien_4/kineceleran.json", """
                {
                  "layers": [
                    {"animation_file":"alienevo:animations/xlr8.json"},
                    {"animation_file":"alienevo:animations/xlr8.json"},
                    {"animation_file":"alienevo:animations/xlr8.json"},
                    {"type":"geckolib:default"},
                    {"type":"geckolib:default"}
                  ]
                }
                """);

        assertEquals(7, PalladiumResourceCompat1201.apply(resources));
        assertTrue(java.util.Arrays.equals(humongousaurAnimation,
                resources.get("assets/afomni/animations/model.humongousaur.animation.json")));
        assertTrue(java.util.Arrays.equals(xlr8Animation,
                resources.get("assets/alienevo/animations/xlr8.animation.json")));
        assertFalse(resources.containsKey("assets/afomni/animations/model.humongousaur.anim.json"));
        assertFalse(resources.containsKey("assets/alienevo/animations/xlr8.json"));
        assertTrue(resources.containsKey("assets/afomni/animations/crystalsapien.animation.json"));
        assertFalse(resources.containsKey("assets/afomni/animations/crystalsapien.animation_copy.json"));
        assertTrue(get(resources, "assets/afomni/palladium/render_layers/vaxasaurian.json")
                .contains("afomni:animations/model.humongousaur.animation.json"));
        String kineceleranLayer = get(
                resources, "assets/alienevo/palladium/render_layers/aliens/alien_4/kineceleran.json");
        assertEquals(3, occurrences(kineceleranLayer, "alienevo:animations/xlr8.animation.json"));
        assertEquals(0, occurrences(kineceleranLayer, "alienevo:animations/xlr8.json"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }

    @Test
    void removesObsoleteEditBoxTickCallAndFixesHexFilteringForAlienEvoPowerUi() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_POWER_UI, """
                nickTextBox.render(e.guiGraphics, mouseX, mouseY, e.partialTick);
                nickTextBox.tick();
                hexTextBox.render(e.guiGraphics, mouseX, mouseY, e.partialTick);
                let currentTime = Date.now();
                if (currentTime - lastTickTime >= TICK_INTERVAL) {
                    hexTextBox.tick();
                    lastTickTime = currentTime;
                }
                function hexToRgb(hex) {
                    hex = hex.replace(/^#/, '');
                    let bigint = parseInt(hex, 16);
                    return { r: (bigint >> 16) & 255, g: (bigint >> 8) & 255, b: bigint & 255 };
                }
                let rawTextboxValue = hexTextBox.getValue() || "";
                let cleanedHexString = rawTextboxValue.replace(/[^0-9A-Fa-f]/g, "").slice(0, 6);
                drawSaveButton();
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));
        String migrated = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_POWER_UI);
        assertTrue(migrated.contains("hexTextBox.render(e.guiGraphics, mouseX, mouseY, e.partialTick);"));
        assertTrue(migrated.contains(
                "Nickname EditBox input is ticked by the 1.21.1 screen; no manual tick() call."));
        assertTrue(migrated.contains(
                "HEX EditBox input is ticked by the 1.21.1 screen; no manual tick() call."));
        assertEquals(1, occurrences(migrated,
                "Nickname EditBox input is ticked by the 1.21.1 screen; no manual tick() call."));
        assertEquals(1, occurrences(migrated,
                "HEX EditBox input is ticked by the 1.21.1 screen; no manual tick() call."));
        assertFalse(migrated.contains("hexTextBox.tick()"));
        assertFalse(migrated.contains("nickTextBox.tick()"));
        assertTrue(migrated.contains("rawTextboxValue.replaceAll(\"[^0-9A-Fa-f]\", \"\")"));
        assertTrue(migrated.contains("cleanedHexString.substring(0, 6)"));
        assertTrue(migrated.contains(
                "hex = hex.startsWith(\"#\") ? hex.substring(1) : hex;"));
        assertFalse(migrated.contains("hex = hex.replace(/^#/, '');"));
        assertFalse(migrated.contains("rawTextboxValue.replace(/[^0-9A-Fa-f]/g"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_POWER_UI));
    }

    @Test
    void movesTemporaryCrystalBlockEventHandlersIntoKubeJsServerDataScripts() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_BLOCKS, """
                event.create('alienevo:crystal_block')
                    .blockEntity(blockEntity => {
                        blockEntity.enableSync();
                        blockEntity.serverTick(2500, 250, entity => {
                            entity.level.destroyBlock(entity.blockPos, false)
                        })
                    });
                event.create('alienevo:smooth_crystal_block')
                    .blockEntity(blockEntity => {
                        blockEntity.enableSync();
                        blockEntity.serverTick(2500, 250, entity => {
                            entity.level.destroyBlock(entity.blockPos, false)
                        })
                    });
                """);

        assertEquals(2, PalladiumResourceCompat1201.apply(resources));

        String startupScript = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_BLOCKS);
        assertEquals(2, occurrences(startupScript, "blockEntity.serverTicking();"));
        assertEquals(2, occurrences(startupScript, "blockEntity.tickFrequency(2500);"));
        assertEquals(2, occurrences(startupScript, "blockEntity.tickOffset(250);"));
        assertFalse(startupScript.contains("blockEntity.serverTick("));
        assertFalse(startupScript.contains("BlockEvents.blockEntityTick"));

        String serverScript = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_CRYSTAL_TICKS);
        assertTrue(serverScript.startsWith(
                "// Legacy Connector: Diamondhead temporary crystal expiry (SERVER script)"));
        assertTrue(serverScript.contains(
                "BlockEvents.blockEntityTick('alienevo:crystal_block', event => {"));
        assertTrue(serverScript.contains(
                "BlockEvents.blockEntityTick('alienevo:smooth_crystal_block', event => {"));
        assertEquals(2, occurrences(serverScript, "event.level.destroyBlock(event.block.pos, false);"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(startupScript, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_BLOCKS));
        assertEquals(serverScript, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_SERVER_CRYSTAL_TICKS));
    }


    @Test
    void guardsAfOmniAoeFireAgainstPalladiumMarkersAndNonLivingEntities() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.AFOMNI_AOE_DAMAGE, """
                StartupEvents.registry('palladium:abilities', (event) => {
                    event.create('afomni:aoe_damage')
                        .tick((entity, entry, holder, enabled) => {
                            if (enabled) {
                                for (let j = 0; j < targets.length; j++) {
                                    if (targets[j] !== entity
                                            && targets[j].type !== "minecraft:armor_stand"
                                            && targets[j].type !== "palladium:suit_stand") {
                                        entity.server.runCommandSilent("damage @s 3 minecraft:player_attack");
                                        targets[j].setSecondsOnFire(firetick);
                                    }
                                }
                            }
                        });
                });
                """);

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = get(resources, PalladiumResourceCompat1201.AFOMNI_AOE_DAMAGE);
        assertTrue(migrated.contains(
                "const LivingEntity = Java.loadClass('net.minecraft.world.entity.LivingEntity');"));
        assertTrue(migrated.contains(
                "targets[j].type !== \"palladium:suit_stand\" && targets[j].type !== \"minecraft:marker\""));
        assertTrue(migrated.contains(
                "if (firetick > 0 && targets[j] instanceof LivingEntity) {"));
        assertEquals(1, occurrences(migrated, "targets[j].igniteForSeconds(firetick);"));
        assertFalse(migrated.contains("targets[j].setSecondsOnFire(firetick);"));
        assertTrue(migrated.indexOf("if (firetick > 0 && targets[j] instanceof LivingEntity) {")
                < migrated.indexOf("targets[j].igniteForSeconds(firetick);"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(migrated, get(resources, PalladiumResourceCompat1201.AFOMNI_AOE_DAMAGE));
    }

    @Test
    void repairsKnownDiamondShardAndAfOmniParticleTextureReferences() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, "assets/alienevo/models/item/diamond_shard.json", """
                {"textures":{"0":"diamond/spike","particle":"diamond/spike"}}
                """);
        put(resources, "assets/afomni/particles/swamp_beam.json", """
                {"textures":["afomni:flames_00","afomni:flames_13"]}
                """);
        put(resources, "assets/afomni/particles/swamp_flame_fly.json", """
                {"textures":["afomni:flames_00b","afomni:flames_05"]}
                """);
        resources.put("assets/alienevo/textures/particle/flames_00.png", new byte[] {1});
        resources.put("assets/alienevo/textures/particle/flames_05.png", new byte[] {1});
        resources.put("assets/alienevo/textures/particle/flames_13.png", new byte[] {1});
        resources.put("assets/afomni/textures/particle/flames_00b.png", new byte[] {1});

        assertEquals(3, PalladiumResourceCompat1201.apply(resources));
        String shard = get(resources, "assets/alienevo/models/item/diamond_shard.json");
        assertEquals(2, occurrences(shard, "minecraft:diamond_spike/spike"));
        assertFalse(shard.contains("diamond/spike"));

        String beam = get(resources, "assets/afomni/particles/swamp_beam.json");
        assertTrue(beam.contains("alienevo:flames_00"));
        assertTrue(beam.contains("alienevo:flames_13"));
        assertFalse(beam.contains("afomni:flames_00\""));

        String fly = get(resources, "assets/afomni/particles/swamp_flame_fly.json");
        assertTrue(fly.contains("afomni:flames_00b"));
        assertTrue(fly.contains("alienevo:flames_05"));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
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
