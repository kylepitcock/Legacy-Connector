package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.*;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.kylepitcock.legacyconnector.core.LegacyConfig;

class BetaGameplayConfigB173Test {
    @TempDir Path temporary;
    private Field configDirectory,values;
    private Object previousDirectory,previousValues;
    private Path file;
    @BeforeEach void isolateConfig()throws Exception{
        configDirectory=FMLPaths.class.getDeclaredField("absolutePath");configDirectory.setAccessible(true);
        values=LegacyConfig.class.getDeclaredField("values");values.setAccessible(true);
        previousDirectory=configDirectory.get(FMLPaths.CONFIGDIR);previousValues=values.get(null);
        Path directory=Files.createDirectory(temporary.resolve("config"));configDirectory.set(FMLPaths.CONFIGDIR,directory);values.set(null,null);
        file=directory.resolve("legacyconnector.properties");BetaGameplayConfigB173.clearServerOverride();
    }
    @AfterEach void restoreConfig()throws Exception{
        BetaGameplayConfigB173.clearServerOverride();values.set(null,previousValues);configDirectory.set(FMLPaths.CONFIGDIR,previousDirectory);
    }
    @Test void existingConfigTemplateIncludesHungerByDefault()throws Exception{
        var entry=LegacyConfig.entries().stream().filter(e->e.key().equals(BetaGameplayConfigB173.FOOD_KEY)).findFirst().orElseThrow();
        assertTrue(entry.isBoolean());assertEquals("true",entry.defaultValue());assertTrue(entry.description().contains("32-tick"));assertTrue(entry.description().contains("0.6"));
        assertTrue(BetaGameplayConfigB173.localFoodUsesHunger());assertTrue(BetaGameplayConfigB173.foodUsesHunger());assertFalse(BetaGameplayConfigB173.hasServerOverride());
        assertTrue(Files.readString(file).contains("b173.foodUsesHunger = true"));
        try(var files=Files.list(file.getParent())){assertEquals(1,files.count());}
    }
    @Test void existingPropertiesSaveAndReloadSelectsBothModesWithoutChangingOtherSettings()throws Exception{
        Files.writeString(file,"# retained comment\nb173.foodUsesHunger = false\nenergy.fePerEu = 7\n");
        assertFalse(BetaGameplayConfigB173.foodUsesHunger());assertEquals(7,LegacyConfig.fePerEu());
        LegacyConfig.save(Map.of(BetaGameplayConfigB173.FOOD_KEY,"true"));values.set(null,null);
        assertTrue(BetaGameplayConfigB173.foodUsesHunger());assertEquals(7,LegacyConfig.fePerEu());
        assertTrue(Files.readString(file).contains("# retained comment"));
        LegacyConfig.save(Map.of(BetaGameplayConfigB173.FOOD_KEY,"false"));values.set(null,null);
        assertFalse(BetaGameplayConfigB173.foodUsesHunger());assertEquals(7,LegacyConfig.fePerEu());
    }
    @Test void missingAndMalformedFoodKeysKeepHungerDefault()throws Exception{
        Files.writeString(file,"energy.fePerEu = 7\n");assertTrue(BetaGameplayConfigB173.foodUsesHunger());
        Files.writeString(file,"b173.foodUsesHunger = invalid\n");values.set(null,null);assertTrue(BetaGameplayConfigB173.foodUsesHunger());
    }
    @Test void remoteOverrideDoesNotChangeLocalFileOrAuthoritativeServerMode()throws Exception{
        Files.writeString(file,"b173.foodUsesHunger = false\n");assertFalse(BetaGameplayConfigB173.localFoodUsesHunger());
        BetaGameplayConfigB173.applyServerOverride(true);assertTrue(BetaGameplayConfigB173.hasServerOverride());assertTrue(BetaGameplayConfigB173.foodUsesHunger());assertFalse(BetaGameplayConfigB173.localFoodUsesHunger());
        var serverMode=new FutureTask<Boolean>(BetaGameplayConfigB173::foodUsesHunger);
        new Thread(SidedThreadGroups.SERVER,serverMode,"b173-food-config-server-test").start();assertFalse(serverMode.get(5,TimeUnit.SECONDS));
        assertEquals("b173.foodUsesHunger = false\n",Files.readString(file));
        BetaGameplayConfigB173.clearServerOverride();assertFalse(BetaGameplayConfigB173.hasServerOverride());assertFalse(BetaGameplayConfigB173.foodUsesHunger());
    }
}
