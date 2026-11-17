package legacyconnector.v1_20_1.net.minecraftforge.fml;

import net.neoforged.neoforgespi.language.IModInfo;

/**
 * Forge 1.20.1's {@code ModLoadingException}: a mod aborts loading with a translation key and context. Thrown out of the mod's constructor or event
 * handler it fails the mod load like any exception (NeoForge's loading screen lists it with the exception's message, the translation key and its arguments).
 */
public class ModLoadingException extends RuntimeException {
    private final IModInfo modInfo;
    private final ModLoadingStage stage;
    private final String i18nMessage;
    private final Object[] context;

    public ModLoadingException(IModInfo modInfo, ModLoadingStage stage, String i18nMessage, Throwable originalException, Object... context) {
        super(i18nMessage + (context == null || context.length == 0 ? "" : " " + java.util.Arrays.toString(context)), originalException);
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
}
