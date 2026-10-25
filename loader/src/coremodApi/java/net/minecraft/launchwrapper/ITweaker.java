package net.minecraft.launchwrapper;

import java.io.File;
import java.util.List;

/** LaunchWrapper's hook for tweakers. Legacy coremod plugins that also act as tweakers (CodeChickenCore) implement it; the layer does not run tweakers. */
public interface ITweaker {
    void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile);

    void injectIntoClassLoader(LaunchClassLoader classLoader);

    String getLaunchTarget();

    String[] getLaunchArguments();
}
