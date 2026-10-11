package net.minecraft.launchwrapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Launch {
    public static File minecraftHome;
    public static File assetsDir;
    public static Map<String, Object> blackboard = new HashMap<>();
    public static LaunchClassLoader classLoader;
}
