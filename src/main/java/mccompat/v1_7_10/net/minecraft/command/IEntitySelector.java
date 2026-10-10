package mccompat.v1_7_10.net.minecraft.command;

import mccompat.v1_7_10.net.minecraft.entity.Entity;

public interface IEntitySelector {
    IEntitySelector selectAnything = e -> e.isEntityAlive();

    boolean isEntityApplicable(Entity entity);
}
