package legacyconnector.v1_12_2.net.minecraft.client.util;

/** Whether tooltips are advanced (F3+H: ids and durability shown). */
public interface ITooltipFlag {
    boolean isAdvanced();

    /** The flag for the game's current setting (not a 1.12 method). */
    static ITooltipFlag mcc$of(boolean advanced) {
        return advanced ? TooltipFlags.ADVANCED : TooltipFlags.NORMAL;
    }

    enum TooltipFlags implements ITooltipFlag {
        NORMAL(false),
        ADVANCED(true);

        private final boolean advanced;

        TooltipFlags(boolean advanced) {
            this.advanced = advanced;
        }

        @Override
        public boolean isAdvanced() {
            return advanced;
        }
    }
}
