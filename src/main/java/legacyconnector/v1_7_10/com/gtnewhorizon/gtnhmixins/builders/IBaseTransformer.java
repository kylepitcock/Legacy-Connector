package legacyconnector.v1_7_10.com.gtnewhorizon.gtnhmixins.builders;

/** GTNH Mixins' base of the enum catalogues of mixins and transformers ({@link IMixins}, {@link ITransformers}). */
public interface IBaseTransformer {
    enum Phase {
        EARLY, LATE
    }

    enum Side {
        COMMON, CLIENT, SERVER
    }
}
