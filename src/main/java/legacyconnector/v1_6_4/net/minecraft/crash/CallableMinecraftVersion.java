package legacyconnector.v1_6_4.net.minecraft.crash;

import java.util.concurrent.Callable;
import legacyconnector.v1_7_10.net.minecraft.crash.CrashReport;

/** 1.6.4's crash report detail naming the game version (mods add it to their own reports). */
public class CallableMinecraftVersion implements Callable<String> {
    final CrashReport theCrashReport;

    public CallableMinecraftVersion(CrashReport report) {
        this.theCrashReport = report;
    }

    public String minecraftVersion() {
        return "1.6.4";
    }

    @Override
    public String call() {
        return minecraftVersion();
    }
}
