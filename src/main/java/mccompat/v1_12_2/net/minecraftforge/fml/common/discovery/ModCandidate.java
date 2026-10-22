package mccompat.v1_12_2.net.minecraftforge.fml.common.discovery;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import mccompat.v1_12_2.net.minecraftforge.fml.common.Loader;
import mccompat.v1_12_2.net.minecraftforge.fml.common.ModContainer;

/** One mod file as FML's discovery saw it: its classes (internal names), packages and mods. */
public class ModCandidate {
    private final File modContainer;
    private final Set<String> classList = new LinkedHashSet<>();
    private final List<String> packages = new ArrayList<>();

    public ModCandidate(File modContainer) {
        this.modContainer = modContainer;
    }

    void mcc$addClass(String internalName) {
        classList.add(internalName);
        int slash = internalName.lastIndexOf('/');
        if (slash > 0) {
            String pkg = internalName.substring(0, slash).replace('/', '.');
            if (!packages.contains(pkg)) packages.add(pkg);
        }
    }

    public File getModContainer() {
        return modContainer;
    }

    public Set<String> getClassList() {
        return classList;
    }

    public List<String> getContainedPackages() {
        return packages;
    }

    public List<ModContainer> getContainedMods() {
        List<ModContainer> out = new ArrayList<>();
        if (modContainer == null) return out;
        for (ModContainer c : Loader.instance().getActiveModList()) {
            if (modContainer.equals(c.getSource())) out.add(c);
        }
        return out;
    }

    public boolean isClasspath() {
        return false;
    }

    public boolean isMinecraftJar() {
        return false;
    }
}
