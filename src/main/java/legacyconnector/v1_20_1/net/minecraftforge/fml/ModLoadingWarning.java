package legacyconnector.v1_20_1.net.minecraftforge.fml;

import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * Forge 1.20.1's {@code ModLoadingWarning}: a translation key, arguments and the mod it concerns. {@link ModLoader#addWarning} turns it into the
 * NeoForge loading issue of the same severity, which the loading screen lists like any other warning.
 */
public class ModLoadingWarning {
    private final IModInfo modInfo;
    private final ModLoadingStage stage;
    private final String i18nMessage;
    private final Object[] context;

    public ModLoadingWarning(IModInfo modInfo, ModLoadingStage stage, String i18nMessage, Object... context) {
        this.modInfo = modInfo;
        this.stage = stage;
        this.i18nMessage = i18nMessage;
        this.context = context == null ? new Object[0] : context;
    }

    public IModInfo getModInfo() {
        return modInfo;
    }

    public ModLoadingStage getStage() {
        return stage;
    }

    public String getI18nMessage() {
        return i18nMessage;
    }

    public Object[] getContext() {
        return context;
    }

    /** The key and arguments (translation happens on the loading screen). */
    public String formatToString() {
        return i18nMessage;
    }

    /** The NeoForge loading issue this warning stands for. */
    public ModLoadingIssue toIssue() {
        ModLoadingIssue issue = ModLoadingIssue.warning(i18nMessage, context);
        return modInfo == null ? issue : issue.withAffectedMod(modInfo);
    }
}
