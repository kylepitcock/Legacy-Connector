package legacyconnector.v1_12_2.net.minecraftforge.common.animation;

/** Time values of animation state machines: the identity, constants, values the code sets, and simple arithmetic on others. */
public final class TimeValues {
    private TimeValues() {
    }

    public static enum IdentityValue implements ITimeValue {
        INSTANCE;

        @Override
        public float apply(float input) {
            return input;
        }
    }

    public static final class ConstValue implements ITimeValue {
        private final float output;

        public ConstValue(float output) {
            this.output = output;
        }

        @Override
        public float apply(float input) {
            return output;
        }
    }

    /** A value the mod sets (a tile entity's progress, a lid's angle); the state machine reads it each frame. */
    public static final class VariableValue implements ITimeValue {
        private float output;

        public VariableValue(float initialValue) {
            this.output = initialValue;
        }

        public void setValue(float newValue) {
            this.output = newValue;
        }

        @Override
        public float apply(float input) {
            return output;
        }
    }

    /** {@code (input - offset) / scale} of another value. */
    public static final class SimpleExprValue implements ITimeValue {
        private final String operators;
        private final java.util.List<ITimeValue> args;

        public SimpleExprValue(String operators, java.util.List<ITimeValue> args) {
            this.operators = operators;
            this.args = args;
        }

        @Override
        public float apply(float input) {
            float ret = input;
            for (int i = 0; i < operators.length() && i < args.size(); i++) {
                float arg = args.get(i).apply(input);
                switch (operators.charAt(i)) {
                    case '+' -> ret += arg;
                    case '-' -> ret -= arg;
                    case '*' -> ret *= arg;
                    case '/' -> ret /= arg;
                    case 'm' -> ret = Math.min(ret, arg);
                    case 'M' -> ret = Math.max(ret, arg);
                    case 'r' -> ret = (float) Math.floor(ret / arg) * arg;
                    case 'R' -> ret = (float) Math.ceil(ret / arg) * arg;
                    case 'f' -> ret -= (float) Math.floor(ret / arg) * arg;
                    case 's' -> ret = (float) Math.signum(ret);
                    default -> {
                    }
                }
            }
            return ret;
        }
    }
}
