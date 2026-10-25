package legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery.asm;

/** The scanned form of an annotation. */
public class ModAnnotation {
    /** An enum value inside scanned annotation data: the descriptor of the enum class and the constant name. */
    public static class EnumHolder {
        private final String desc;
        private final String value;

        public EnumHolder(String desc, String value) {
            this.desc = desc;
            this.value = value;
        }

        public String getDesc() {
            return desc;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return desc + "." + value;
        }
    }
}
