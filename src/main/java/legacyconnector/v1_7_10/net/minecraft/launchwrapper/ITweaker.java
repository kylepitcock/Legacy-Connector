package legacyconnector.v1_7_10.net.minecraft.launchwrapper;

import java.io.File;
import java.util.List;

/** LaunchWrapper's tweaker interface. Coremod plugins that double as tweakers (and FML's plugin wrapper) implement it; nothing runs tweakers here. */
public interface ITweaker {
    void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile);

    void injectIntoClassLoader(LaunchClassLoader classLoader);

    String getLaunchTarget();

    String[] getLaunchArguments();
}
