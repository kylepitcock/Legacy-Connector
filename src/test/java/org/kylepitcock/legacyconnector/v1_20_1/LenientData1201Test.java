package org.kylepitcock.legacyconnector.v1_20_1;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.LenientData1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LenientData1201Test {
    private static final Set<String> KNOWN = Set.of("minecraft:zombie", "m:bonnie");

    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void unknownSpawnerOfASingleObjectDisablesTheModifier() {
        var f = LenientData1201.filterBiomeModifier(json("""
                {"type":"neoforge:add_spawns","biomes":{"type":"neoforge:any"},
                 "spawners":{"type":"management_wanted_alpha:bonnie_statue","weight":20,"minCount":4,"maxCount":4}}"""), KNOWN::contains);
        assertEquals(json("{\"type\":\"neoforge:none\"}"), f.replacement());
        assertEquals(List.of("management_wanted_alpha:bonnie_statue"), f.dropped());
    }

    @Test
    void onlyTheUnknownEntriesOfAListGo() {
        var f = LenientData1201.filterBiomeModifier(json("""
                {"type":"neoforge:add_spawns","biomes":"#minecraft:is_forest","spawners":[
                 {"type":"m:bonnie","weight":1,"minCount":1,"maxCount":2},{"type":"x:gone","weight":1,"minCount":1,"maxCount":2}]}"""), KNOWN::contains);
        assertEquals(json("""
                {"type":"neoforge:add_spawns","biomes":"#minecraft:is_forest","spawners":[{"type":"m:bonnie","weight":1,"minCount":1,"maxCount":2}]}"""),
                f.replacement());
        assertEquals(List.of("x:gone"), f.dropped());
    }

    @Test
    void knownSpawnersAndOtherModifiersAreLeftAlone() {
        assertNull(LenientData1201.filterBiomeModifier(json("""
                {"type":"neoforge:add_spawns","biomes":"#a:b","spawners":{"type":"minecraft:zombie","weight":1,"minCount":1,"maxCount":1}}"""), KNOWN::contains));
        assertNull(LenientData1201.filterBiomeModifier(json("{\"type\":\"neoforge:add_features\",\"features\":\"x:y\"}"), KNOWN::contains));
    }

    @Test
    void removeSpawnsDropsUnknownTypesButKeepsTags() {
        var f = LenientData1201.filterBiomeModifier(json("""
                {"type":"neoforge:remove_spawns","biomes":"#a:b","entity_types":["minecraft:zombie","x:gone","#c:bosses"]}"""), KNOWN::contains);
        assertEquals(json("""
                {"type":"neoforge:remove_spawns","biomes":"#a:b","entity_types":["minecraft:zombie","#c:bosses"]}"""), f.replacement());
        assertEquals(List.of("x:gone"), f.dropped());
    }
}
