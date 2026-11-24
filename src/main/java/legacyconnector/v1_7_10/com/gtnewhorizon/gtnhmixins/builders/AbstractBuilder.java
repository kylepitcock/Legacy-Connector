package legacyconnector.v1_7_10.com.gtnewhorizon.gtnhmixins.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** GTNH Mixins' base of {@link MixinBuilder} and {@link TransformerBuilder}: class names per side, required and excluded mods, a phase and a condition. */
public abstract class AbstractBuilder {
    protected List<String> commonClasses = new ArrayList<>();
    protected List<String> clientClasses = new ArrayList<>();
    protected List<String> serverClasses = new ArrayList<>();
    protected List<ITargetMod> requiredMods = new ArrayList<>();
    protected List<ITargetMod> excludedMods = new ArrayList<>();
    protected IBaseTransformer.Phase phase = IBaseTransformer.Phase.LATE;
    protected Supplier<Boolean> applyIf = () -> true;

    public AbstractBuilder() {}

    protected AbstractBuilder addCommonClasses(String... classes) {
        commonClasses.addAll(List.of(classes));
        return this;
    }

    protected AbstractBuilder addClientClasses(String... classes) {
        clientClasses.addAll(List.of(classes));
        return this;
    }

    protected AbstractBuilder addServerClasses(String... classes) {
        serverClasses.addAll(List.of(classes));
        return this;
    }

    protected AbstractBuilder addSidedClasses(IBaseTransformer.Side side, String... classes) {
        switch (side) {
            case CLIENT -> addClientClasses(classes);
            case SERVER -> addServerClasses(classes);
            default -> addCommonClasses(classes);
        }
        return this;
    }

    protected AbstractBuilder setApplyIf(Supplier<Boolean> applyIf) {
        this.applyIf = applyIf;
        return this;
    }

    protected AbstractBuilder addRequiredMod(ITargetMod mod) {
        requiredMods.add(mod);
        return this;
    }

    protected AbstractBuilder addExcludedMod(ITargetMod mod) {
        excludedMods.add(mod);
        return this;
    }

    protected void addClassesForCurrentSide(List<String> common, List<String> side) {
        common.addAll(commonClasses);
        boolean client = legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient();
        side.addAll(client ? clientClasses : serverClasses);
    }

    protected void addAllClassesTo(List<String> out) {
        out.addAll(commonClasses);
        out.addAll(clientClasses);
        out.addAll(serverClasses);
    }

    protected void addAllTargetsTo(Set<ITargetMod> out) {
        out.addAll(requiredMods);
        out.addAll(excludedMods);
    }

    protected boolean shouldLoad(Set<ITargetMod> loaded) {
        return Boolean.TRUE.equals(applyIf.get()) && allRequiredModsPresent(loaded) && noExcludedModsPresent(loaded);
    }

    protected boolean allRequiredModsPresent(Set<ITargetMod> loaded) {
        return loaded.containsAll(requiredMods);
    }

    protected boolean noExcludedModsPresent(Set<ITargetMod> loaded) {
        for (ITargetMod m : excludedMods) if (loaded.contains(m)) return false;
        return true;
    }

    protected static void loadClasses(List<AbstractBuilder> builders, Set<ITargetMod> loaded, List<String> common, List<String> side) {
        for (AbstractBuilder b : builders) {
            if (!b.shouldLoad(loaded)) continue;
            b.addClassesForCurrentSide(common, side);
        }
    }

    /** The target mods of the builders of the phase that are installed. */
    protected static Set<ITargetMod> getLoadedTargetedMods(List<AbstractBuilder> builders, IBaseTransformer.Phase phase, Set<String> loadedCoreMods,
            Set<String> loadedMods) {
        Set<ITargetMod> all = new java.util.LinkedHashSet<>();
        for (AbstractBuilder b : builders) if (phase == null || b.phase == phase) b.addAllTargetsTo(all);
        Set<ITargetMod> present = new java.util.LinkedHashSet<>();
        for (ITargetMod m : all) {
            TargetModBuilder t = m.getBuilder();
            if (t != null && t.isPresent()) present.add(m);
        }
        return present;
    }
}
