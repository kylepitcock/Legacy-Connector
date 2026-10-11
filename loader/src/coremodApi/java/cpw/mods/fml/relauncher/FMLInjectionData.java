package cpw.mods.fml.relauncher;

import java.io.File;
import net.minecraft.launchwrapper.Launch;

public class FMLInjectionData {
    public static Object[] data() {
        return new Object[] {"7", "10", "99", "1614", "1.7.10", "9.05", Launch.minecraftHome, java.util.List.of()};
    }
}
