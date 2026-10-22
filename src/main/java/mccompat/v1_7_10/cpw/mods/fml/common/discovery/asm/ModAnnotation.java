package mccompat.v1_7_10.cpw.mods.fml.common.discovery.asm;

/** FML's annotation-value holder types; mods read {@code EnumHolder.value} reflectively. */
public class ModAnnotation {
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
    }
}
