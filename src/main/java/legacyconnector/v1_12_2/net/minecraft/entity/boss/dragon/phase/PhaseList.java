package legacyconnector.v1_12_2.net.minecraft.entity.boss.dragon.phase;

/** The ender dragon's phases, by the ids the modern phases share. */
public class PhaseList<T extends IPhase> {
    private static final java.util.List<PhaseList<?>> PHASES = new java.util.ArrayList<>();

    public static final PhaseList<IPhase> HOLDING_PATTERN = create("HoldingPattern");
    public static final PhaseList<IPhase> STRAFE_PLAYER = create("StrafePlayer");
    public static final PhaseList<IPhase> LANDING_APPROACH = create("LandingApproach");
    public static final PhaseList<IPhase> LANDING = create("Landing");
    public static final PhaseList<IPhase> TAKEOFF = create("Takeoff");
    public static final PhaseList<IPhase> SITTING_FLAMING = create("SittingFlaming");
    public static final PhaseList<IPhase> SITTING_SCANNING = create("SittingScanning");
    public static final PhaseList<IPhase> SITTING_ATTACKING = create("SittingAttacking");
    public static final PhaseList<IPhase> CHARGING_PLAYER = create("ChargingPlayer");
    public static final PhaseList<IPhase> DYING = create("Dying");
    public static final PhaseList<IPhase> HOVER = create("Hover");

    private final int id;
    private final String name;

    private PhaseList(int id, String name) {
        this.id = id;
        this.name = name;
    }

    private static PhaseList<IPhase> create(String name) {
        PhaseList<IPhase> p = new PhaseList<>(PHASES.size(), name);
        PHASES.add(p);
        return p;
    }

    /** The phase instance of this kind the dragon's manager holds. */
    @SuppressWarnings("unchecked")
    public T createPhase(legacyconnector.v1_12_2.net.minecraft.entity.boss.EntityDragon dragon) {
        return (T) new PhaseManager(dragon).getPhase((PhaseList<IPhase>) this);
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return name + " (#" + id + ")";
    }

    public static PhaseList<?> getById(int id) {
        return id >= 0 && id < PHASES.size() ? PHASES.get(id) : HOLDING_PATTERN;
    }

    public static int getTotalPhases() {
        return PHASES.size();
    }

    /** The modern phase of the same id. */
    public net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase<?> mcc$modern() {
        return net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.getById(id);
    }

    public static PhaseList<?> mcc$of(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase<?> modern) {
        return modern == null ? null : getById(modern.getId());
    }
}
