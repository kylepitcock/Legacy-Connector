package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Regression tests for inspected AlienEvo script compatibility rewrites. */
class KubeJSScriptScopeCompatTest {
    @Test
    void renamesOnlyTheAlienCodexComponentAliasAndItsReferences() {
        String original = """
                const Component = Java.loadClass("net.minecraft.network.chat.Component");

                global.getAlienDescription = function(id) {
                    return Component.translatable(`alienevo.alien_desc.${id}`);
                };
                """;
        byte[] input = original.getBytes(StandardCharsets.UTF_8);

        byte[] result = rewrite("kubejs_scripts/alien_codex.js", input);
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertEquals(2, count(rewritten, "AlienEvoComponent"));
        assertFalse(rewritten.contains("const Component"));
        assertTrue(rewritten.contains("AlienEvoComponent.translatable"));
        assertTrue(rewritten.contains("global.getAlienDisplayName ="));
    }

    @Test
    void renamesOnlyTheDisplayPlayerQuaternionAliasAndItsReference() {
        String original = """
                const InventoryScreen = Java.loadClass('net.minecraft.client.gui.screens.inventory.InventoryScreen');
                const Quaternionf = Java.loadClass('org.joml.Quaternionf');

                const rotation = new Quaternionf(1, 0, 0, 0);
                """;
        byte[] input = original.getBytes(StandardCharsets.UTF_8);

        byte[] result = rewrite("kubejs_scripts/display_player.js", input);
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertEquals(2, count(rewritten, "AlienEvoQuaternionf"));
        assertFalse(rewritten.contains("const Quaternionf"));
        assertTrue(rewritten.contains("new AlienEvoQuaternionf"));
    }

    @Test
    void movesFailsafeHelpersOutOfTheReadOnlyGlobalMap() {
        String original = """
                global.sound = (entity, soundId, volume, pitch, shift) => {
                    entity.level.runCommandSilent(soundId);
                };

                global.applyKnockback = (entity, target, strength) => {
                    target.knockback(strength, 0, 0);
                };

                global.applyKnockback(entity, target, 3);
                global.sound(entity, 'example:sound', 1, 1, 0);
                global.sound(entity, 'example:other', 1, 1, 0);
                """;
        byte[] result = rewrite("kubejs_scripts/failsafe.js", original.getBytes(StandardCharsets.UTF_8));
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertNotSame(original, rewritten);
        assertTrue(rewritten.contains("function alienevoSound("));
        assertTrue(rewritten.contains("function alienevoApplyKnockback("));
        assertFalse(rewritten.contains("global.sound"));
        assertFalse(rewritten.contains("global.applyKnockback"));
    }

    @Test
    void keepsLevelCommandDisplayNameHelperLocalInsteadOfWritingGlobal() {
        String original = """
                if (!global[defaultNameKey]) {
                  let defaultName = simpleName;
                  global[defaultNameKey] = defaultName;
                }

                global.getAlienDisplayName = (player, alienNumber) => {
                  return `Alien #${alienNumber}`;
                };
                """;
        byte[] result = rewrite("kubejs_scripts/level_command.js", original.getBytes(StandardCharsets.UTF_8));
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertTrue(rewritten.contains("const getAlienDisplayName ="));
        assertFalse(rewritten.contains("global[defaultNameKey] ="));
        assertFalse(rewritten.contains("global.getAlienDisplayName ="));
    }

    @Test
    void migratesTheLegacyHurtEventNameToBeforeHurt() {
        String original = """
                EntityEvents.hurt(event => event.cancel());
                EntityEvents.hurt(event => event.cancel());
                EntityEvents.hurt(event => event.cancel());
                EntityEvents.hurt(event => event.cancel());
                """;
        byte[] result = rewrite("kubejs_scripts/fall.js", original.getBytes(StandardCharsets.UTF_8));
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertEquals(4, count(rewritten, "EntityEvents.beforeHurt("));
        assertFalse(rewritten.contains("EntityEvents.hurt("));
    }


    @Test
    void migratesAllHitHandlersToBeforeHurt() {
        String original = """
                EntityEvents.hurt(event => {});
                EntityEvents.hurt(event => {});
                EntityEvents.hurt(event => {});
                EntityEvents.hurt(event => {});
                EntityEvents.hurt(event => {});
                EntityEvents.hurt(event => {});
                EntityEvents.hurt(event => {});
                EntityEvents.hurt(event => {});
                """;
        byte[] result = rewrite("kubejs_scripts/hit.js", original.getBytes(StandardCharsets.UTF_8));
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertEquals(8, count(rewritten, "EntityEvents.beforeHurt("));
        assertFalse(rewritten.contains("EntityEvents.hurt("));
    }

    @Test
    void removesInvalidWildcardFromUnfilteredEntityInteractionRegistration() {
        String original = "ItemEvents.entityInteracted('*', event => { /* body */ });";
        byte[] result = rewrite("kubejs_scripts/workbench.js", original.getBytes(StandardCharsets.UTF_8));
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertEquals("ItemEvents.entityInteracted(event => { /* body */ });", rewritten);
    }

    @Test
    void renamesOnlyTheConflictingVectorAlias() {
        String original = """
                const Vec3f = Java.loadClass('org.joml.Vector3f');
                function vector() {
                    return new Vec3f(1, 2, 3);
                }
                """;
        byte[] result = rewrite("kubejs_scripts/slam.js", original.getBytes(StandardCharsets.UTF_8));
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertTrue(rewritten.contains("const AlienEvoVector3f"));
        assertTrue(rewritten.contains("new AlienEvoVector3f"));
        assertFalse(rewritten.contains("const Vec3f"));
    }


    @Test
    void keepsCodeHintPositionSetsLocalToTheServerScript() {
        String original = """
                ItemEvents.rightClicked('alienevo:code_tablet', event => {
                    if (!global.usedPositions) {
                        global.usedPositions = new Set();
                    }
                    global.usedPositions.has(0);
                    global.usedPositions.clear();
                    global.usedPositions.add(1);
                });
                ItemEvents.rightClicked('alienevo:code_tablet_selfdestruct', event => {
                    if (!global.usedSelfDestructPositions) {
                        global.usedSelfDestructPositions = new Set();
                    }
                    global.usedSelfDestructPositions.has(0);
                    global.usedSelfDestructPositions.clear();
                    global.usedSelfDestructPositions.add(2);
                });
                """;
        byte[] result = rewrite("kubejs_scripts/code_hints.js", original.getBytes(StandardCharsets.UTF_8));
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertTrue(rewritten.startsWith("let alienevoUsedPositions = new Set();"));
        assertTrue(rewritten.contains("let alienevoUsedSelfDestructPositions = new Set();"));
        assertFalse(rewritten.contains("global.usedPositions"));
        assertFalse(rewritten.contains("global.usedSelfDestructPositions"));
        assertTrue(rewritten.contains("alienevoUsedPositions.add(1)"));
        assertTrue(rewritten.contains("alienevoUsedSelfDestructPositions.add(2)"));
    }

    @Test
    void leavesOtherNamespacesAndResourcesUnchanged() {
        byte[] input = "const Component = Java.loadClass('example.Component');".getBytes(StandardCharsets.UTF_8);

        assertSame(input, KubeJSScriptScopeCompat.rewriteKnownIdentifierCollisions(
                "afomni", "kubejs_scripts/alien_codex.js", input));
        assertSame(input, KubeJSScriptScopeCompat.rewriteKnownIdentifierCollisions(
                "alienevo", "kubejs_scripts/other.js", input));
    }

    @Test
    void leavesUnknownScriptRevisionsUnchanged() {
        byte[] input = """
                global.sound = (entity, soundId, volume, pitch, shift) => {};
                global.sound(entity, 'example:sound', 1, 1, 0);
                global.sound(entity, 'example:sound', 1, 1, 0);
                """.getBytes(StandardCharsets.UTF_8);

        assertSame(input, rewrite("kubejs_scripts/failsafe.js", input));
    }

    private static byte[] rewrite(String path, byte[] input) {
        return KubeJSScriptScopeCompat.rewriteKnownIdentifierCollisions("alienevo", path, input);
    }

    private static int count(String text, String value) {
        return text.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
