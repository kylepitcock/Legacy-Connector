package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.data.registries.VanillaRegistries;

/** Command argument factories whose 1.20.1 signature lost or gained a parameter (call rows in mc-calls.tsv). */
public final class CommandArgCompat {
    private static volatile CommandBuildContext lookup;

    private CommandArgCompat() {}

    /** 1.20.1 {@code ComponentArgument.textComponent()}; 1.21.1 needs a registry lookup to parse components. */
    public static ComponentArgument textComponent() {
        CommandBuildContext l = lookup;
        if (l == null) lookup = l = CommandBuildContext.simple(VanillaRegistries.createLookup(), net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
        return ComponentArgument.textComponent(l);
    }
}
