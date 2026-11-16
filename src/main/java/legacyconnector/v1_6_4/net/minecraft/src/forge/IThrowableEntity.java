package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface IThrowableEntity {
    legacyconnector.v1_7_10.net.minecraft.entity.Entity getThrower();

    void setThrower(legacyconnector.v1_7_10.net.minecraft.entity.Entity thrower);
}
