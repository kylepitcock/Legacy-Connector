package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.Holder;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

/**
 * Stands in for the enum {@code MapDecoration.Type} (1.20.1); 1.21.1 has data driven {@code MapDecorationType}s kept as holders in {@link MapDecorationTypes}.
 * Calls that take the type convert it with {@link #holder()}.
 */
public enum LegacyMapDecorationType {
    PLAYER("PLAYER"), FRAME("FRAME"), RED_MARKER("RED_MARKER"), BLUE_MARKER("BLUE_MARKER"), TARGET_X("TARGET_X"), TARGET_POINT("TARGET_POINT"),
    PLAYER_OFF_MAP("PLAYER_OFF_MAP"), PLAYER_OFF_LIMITS("PLAYER_OFF_LIMITS"), MANSION("WOODLAND_MANSION"), MONUMENT("OCEAN_MONUMENT"),
    BANNER_WHITE("WHITE_BANNER"), BANNER_ORANGE("ORANGE_BANNER"), BANNER_MAGENTA("MAGENTA_BANNER"), BANNER_LIGHT_BLUE("LIGHT_BLUE_BANNER"),
    BANNER_YELLOW("YELLOW_BANNER"), BANNER_LIME("LIME_BANNER"), BANNER_PINK("PINK_BANNER"), BANNER_GRAY("GRAY_BANNER"),
    BANNER_LIGHT_GRAY("LIGHT_GRAY_BANNER"), BANNER_CYAN("CYAN_BANNER"), BANNER_PURPLE("PURPLE_BANNER"), BANNER_BLUE("BLUE_BANNER"),
    BANNER_BROWN("BROWN_BANNER"), BANNER_GREEN("GREEN_BANNER"), BANNER_RED("RED_BANNER"), BANNER_BLACK("BLACK_BANNER"), RED_X("RED_X");

    private final String field;

    LegacyMapDecorationType(String field) {
        this.field = field;
    }

    @SuppressWarnings("unchecked")
    public Holder<MapDecorationType> holder() {
        try {
            return (Holder<MapDecorationType>) MapDecorationTypes.class.getField(field).get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no map decoration type " + field, e);
        }
    }

    public static Holder<MapDecorationType> holder(LegacyMapDecorationType type) {
        return type.holder();
    }

    public byte getIcon() {
        return (byte) ordinal();
    }

    public boolean isRenderedOnFrame() {
        return holder().value().showOnItemFrame();
    }

    public boolean hasMapColor() {
        return holder().value().mapColor() >= 0;
    }
}
