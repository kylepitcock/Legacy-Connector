package legacyconnector.v1_20_1.net.minecraftforge.fml;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Forge 1.20.1's server-list "display test" extension point (what the client shows when the mod is missing on the other side). NeoForge 1.21.1
 * negotiates by payload registration instead, so the value is accepted and ignored ({@code ForgeApi1201.registerExtensionPoint}).
 */
public class IExtensionPoint$DisplayTest implements net.neoforged.fml.IExtensionPoint {
    public final Supplier<String> suppliedVersion;
    public final BiPredicate<String, Boolean> remoteVersionTest;

    public IExtensionPoint$DisplayTest(Supplier<String> suppliedVersion, BiPredicate<String, Boolean> remoteVersionTest) {
        this.suppliedVersion = suppliedVersion;
        this.remoteVersionTest = remoteVersionTest;
    }

    public IExtensionPoint$DisplayTest(String ignoredServerOnly) {
        this(() -> ignoredServerOnly, (a, b) -> true);
    }
}
