package legacyconnector.v1_20_1.cpw.mods.modlauncher.api;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * ModLauncher 10's name mapping service (1.20.1), whose {@code Domain} enum later versions dropped. Mixin plugins and coremods name its domains when they ask
 * for the SRG name of a class, method or field; there is no SRG mapping in this game, so no service of this type is ever provided.
 */
public interface INameMappingService {
    enum Domain {
        CLASS,
        METHOD,
        FIELD
    }

    String mappingName();

    String mappingVersion();

    Map.Entry<String, String> understanding();

    BiFunction<Domain, String, String> namingFunction();
}
