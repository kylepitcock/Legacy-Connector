package org.kylepitcock.legacyconnector.mixinplugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LegacyMixinPluginGuardTest {
    @Test
    void guardedListMatchesTheJsonAndIsNotListedAsPlainMixin() throws Exception {
        String json = Files.readString(Path.of("src/main/resources/legacyconnector.mixins.json"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("\"legacyconnector_guarded\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
        assertEquals(true, m.find());
        List<String> names = new ArrayList<>();
        Matcher n = Pattern.compile("\"([^\"]+)\"").matcher(m.group(1));
        while (n.find()) names.add(n.group(1));
        List<String> guarded = new ArrayList<>();
        for (String[] g : LegacyMixinPlugin.GUARDED) guarded.add(g[0]);
        assertEquals(names, guarded);
        String plain = json.substring(0, json.indexOf("\"legacyconnector_guarded\""));
        for (String g : guarded) assertFalse(plain.contains("\"" + g + "\""), g + " must not be a plain json mixin (Mixin throws when its target is already loaded)");
    }

    @Test
    void unknownClassIsReportedAsNotLoaded() {
        assertFalse(LegacyMixinPlugin.isLoaded("does.not.Exist"));
    }
}
