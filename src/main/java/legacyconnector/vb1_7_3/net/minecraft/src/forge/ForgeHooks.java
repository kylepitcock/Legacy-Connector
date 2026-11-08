package legacyconnector.vb1_7_3.net.minecraft.src.forge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Forge 1.0's tool tables for Beta 1.7.3: tool class and level per item id ({@code toolClasses}), harvest level per (block id, metadata,
 * tool class) ({@code toolHarvestLevels}) and which of those a tool is effective against ({@code toolEffectiveness}). Filled through
 * {@link MinecraftForge}; mods also read them by reflection (IndustrialCraft 2). Version 1.0.7.
 */
public class ForgeHooks {
    public static final int majorVersion = 1;
    public static final int minorVersion = 0;
    public static final int revisionVersion = 7;

    static LinkedList<Object> craftingHandlers = new LinkedList<>();
    static LinkedList<Object> destroyToolHandlers = new LinkedList<>();
    static LinkedList<Object> sleepHandlers = new LinkedList<>();
    static boolean toolInit;
    static HashMap<Object, Object> toolClasses = new HashMap<>();
    static HashMap<Object, Object> toolHarvestLevels = new HashMap<>();
    static HashSet<Object> toolEffectiveness = new HashSet<>();

    public ForgeHooks() {}

    static void initTools() {
        toolInit = true;
    }
}
