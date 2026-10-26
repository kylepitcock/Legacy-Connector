package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;

/** Vanilla's flowers: the two flower blocks and the kinds each has. */
public abstract class BlockFlower extends Block {
    protected BlockFlower() {
        super(Material.PLANTS);
    }

    protected legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum<EnumFlowerType> type;

    public abstract EnumFlowerColor getBlockType();

    /** The flower kinds of this block's color, as a state property (made on first use, as vanilla did). */
    public legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty<EnumFlowerType> getTypeProperty() {
        if (type == null) {
            type = legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum.create("type", EnumFlowerType.class,
                    t -> t != null && t.getBlockType() == getBlockType());
        }
        return type;
    }

    public enum EnumFlowerColor {
        YELLOW, RED;

        private BlockFlower block;

        /** The vanilla flower block of this color. */
        public synchronized BlockFlower getBlock() {
            if (block == null) {
                final EnumFlowerColor color = this;
                block = new BlockFlower() {
                    @Override
                    public EnumFlowerColor getBlockType() {
                        return color;
                    }
                };
                String name = this == YELLOW ? "yellow_flower" : "red_flower";
                block.setRegistryName("minecraft", name);
                block.mcc$modern = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                        .get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(this == YELLOW ? "dandelion" : "poppy"));
            }
            return block;
        }
    }

    public enum EnumFlowerType implements IStringSerializable {
        DANDELION(EnumFlowerColor.YELLOW, 0, "dandelion"), POPPY(EnumFlowerColor.RED, 0, "poppy"), BLUE_ORCHID(EnumFlowerColor.RED, 1, "blue_orchid"),
        ALLIUM(EnumFlowerColor.RED, 2, "allium"), HOUSTONIA(EnumFlowerColor.RED, 3, "houstonia"), RED_TULIP(EnumFlowerColor.RED, 4, "red_tulip"),
        ORANGE_TULIP(EnumFlowerColor.RED, 5, "orange_tulip"), WHITE_TULIP(EnumFlowerColor.RED, 6, "white_tulip"), PINK_TULIP(EnumFlowerColor.RED, 7, "pink_tulip"),
        OXEYE_DAISY(EnumFlowerColor.RED, 8, "oxeye_daisy");

        private final EnumFlowerColor color;
        private final int meta;
        private final String name;

        EnumFlowerType(EnumFlowerColor color, int meta, String name) {
            this.color = color;
            this.meta = meta;
            this.name = name;
        }

        public EnumFlowerColor getBlockType() {
            return color;
        }

        public int getMeta() {
            return meta;
        }

        public static EnumFlowerType getType(EnumFlowerColor color, int meta) {
            for (EnumFlowerType t : values()) if (t.color == color && t.meta == meta) return t;
            for (EnumFlowerType t : values()) if (t.color == color) return t;
            return DANDELION;
        }

        public static EnumFlowerType[] getTypes(EnumFlowerColor color) {
            return java.util.Arrays.stream(values()).filter(t -> t.color == color).toArray(EnumFlowerType[]::new);
        }

        @Override
        public String getName() {
            return name;
        }

        public String getTranslationKey() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
