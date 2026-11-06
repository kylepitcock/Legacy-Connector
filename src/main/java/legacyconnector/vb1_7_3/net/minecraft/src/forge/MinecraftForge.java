package legacyconnector.vb1_7_3.net.minecraft.src.forge;

import java.util.Arrays;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;

/**
 * Forge 1.0 for Beta 1.7.3: the tool and harvest-level registrations (kept in {@link ForgeHooks}) and the version checks, which pass as
 * Forge 1.0.7. Handler registrations that take Forge's interfaces are synthesized for now.
 */
public class MinecraftForge {
    public MinecraftForge() {}

    public static void setToolClass(Item tool, String toolClass, int harvestLevel) {
        ForgeHooks.initTools();
        ForgeHooks.toolClasses.put(tool.id, Arrays.asList(toolClass, harvestLevel));
    }

    public static void setBlockHarvestLevel(Block block, int meta, String toolClass, int harvestLevel) {
        ForgeHooks.initTools();
        var key = Arrays.asList(block.id, meta, toolClass);
        ForgeHooks.toolHarvestLevels.put(key, harvestLevel);
        ForgeHooks.toolEffectiveness.add(key);
    }

    public static void setBlockHarvestLevel(Block block, String toolClass, int harvestLevel) {
        for (int meta = 0; meta < 16; meta++) setBlockHarvestLevel(block, meta, toolClass, harvestLevel);
    }

    public static void removeBlockEffectiveness(Block block, int meta, String toolClass) {
        ForgeHooks.initTools();
        ForgeHooks.toolEffectiveness.remove(Arrays.asList(block.id, meta, toolClass));
    }

    public static void removeBlockEffectiveness(Block block, String toolClass) {
        for (int meta = 0; meta < 16; meta++) removeBlockEffectiveness(block, meta, toolClass);
    }

    public static void addPickaxeBlockEffectiveAgainst(Block block) {
        setBlockHarvestLevel(block, "pickaxe", 0);
    }

    /** A mod that needs another Forge version stopped the game here; this is Forge 1.0.7 and every 1.0.x mod is let through. */
    public static void versionDetect(String modName, int major, int minor, int revision) {}

    public static void versionDetectStrict(String modName, int major, int minor, int revision) {}

    public static void killMinecraft(String modName, String message) {
        throw new RuntimeException(modName + ": " + message);
    }
}
