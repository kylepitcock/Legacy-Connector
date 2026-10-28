package legacyconnector.v1_12_2.net.minecraft.world.storage;

/** 1.12.2 map icon: type, map-local position (-128..127) and rotation (0-15). */
public class MapDecoration {
    private final Type type;
    private byte x;
    private byte y;
    private byte rotation;

    public MapDecoration(Type typeIn, byte xIn, byte yIn, byte rotationIn) {
        this.type = typeIn;
        this.x = xIn;
        this.y = yIn;
        this.rotation = rotationIn;
    }

    public byte getImage() {
        return type.getIcon();
    }

    public Type getType() {
        return type;
    }

    public byte getX() {
        return x;
    }

    public byte getY() {
        return y;
    }

    public byte getRotation() {
        return rotation;
    }

    public boolean renderOnFrame() {
        return type.isRenderedOnFrame();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof MapDecoration d && type == d.type && rotation == d.rotation && x == d.x && y == d.y;
    }

    @Override
    public int hashCode() {
        int i = type.getIcon();
        i = 31 * i + x;
        i = 31 * i + y;
        return 31 * i + rotation;
    }

    public enum Type {
        PLAYER(false),
        FRAME(true),
        RED_MARKER(false),
        BLUE_MARKER(false),
        TARGET_X(true),
        TARGET_POINT(true),
        PLAYER_OFF_MAP(false),
        PLAYER_OFF_LIMITS(false),
        MANSION(true, 5393476),
        MONUMENT(true, 3830373);

        private final byte icon;
        private final boolean renderedOnFrame;
        private final int mapColor;

        Type(boolean renderedOnFrame) {
            this(renderedOnFrame, -1);
        }

        Type(boolean renderedOnFrame, int mapColor) {
            this.icon = (byte) ordinal();
            this.renderedOnFrame = renderedOnFrame;
            this.mapColor = mapColor;
        }

        public byte getIcon() {
            return icon;
        }

        public boolean isRenderedOnFrame() {
            return renderedOnFrame;
        }

        public boolean hasMapColor() {
            return mapColor >= 0;
        }

        public int getMapColor() {
            return mapColor;
        }

        public static Type byIcon(byte icon) {
            return values()[Math.floorMod(icon, values().length)];
        }

        /** The modern decoration type of this icon. */
        public net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mcc$modern() {
            return switch (this) {
                case PLAYER -> net.minecraft.world.level.saveddata.maps.MapDecorationTypes.PLAYER;
                case FRAME -> net.minecraft.world.level.saveddata.maps.MapDecorationTypes.FRAME;
                case RED_MARKER -> net.minecraft.world.level.saveddata.maps.MapDecorationTypes.RED_MARKER;
                case BLUE_MARKER -> net.minecraft.world.level.saveddata.maps.MapDecorationTypes.BLUE_MARKER;
                case TARGET_X -> net.minecraft.world.level.saveddata.maps.MapDecorationTypes.TARGET_X;
                case TARGET_POINT -> net.minecraft.world.level.saveddata.maps.MapDecorationTypes.TARGET_POINT;
                case PLAYER_OFF_MAP -> net.minecraft.world.level.saveddata.maps.MapDecorationTypes.PLAYER_OFF_MAP;
                case PLAYER_OFF_LIMITS -> net.minecraft.world.level.saveddata.maps.MapDecorationTypes.PLAYER_OFF_LIMITS;
                case MANSION -> net.minecraft.world.level.saveddata.maps.MapDecorationTypes.WOODLAND_MANSION;
                case MONUMENT -> net.minecraft.world.level.saveddata.maps.MapDecorationTypes.OCEAN_MONUMENT;
            };
        }
    }
}
