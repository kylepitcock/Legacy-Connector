package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.ProjectileImpactEvent;

/** Forge's ForgeEventFactory.onProjectileImpact for the legacy projectiles that fly themselves (mods' arrows and throwables). */
public final class ProjectileEvents1122 {
    private ProjectileEvents1122() {}

    /** @return true when a handler cancelled the impact */
    public static boolean impact(Entity projectile, RayTraceResult result) {
        var bus = MinecraftForge.EVENT_BUS;
        ProjectileImpactEvent event;
        if (projectile instanceof legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityArrow a) event = new ProjectileImpactEvent.Arrow(a, result);
        else if (projectile instanceof legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityThrowable t) event = new ProjectileImpactEvent.Throwable(t, result);
        else event = new ProjectileImpactEvent(projectile, result);
        if (!bus.mcc$hasListeners(event.getClass())) return false;
        try {
            // Forge's ForgeHooks.onThrowableImpact posts the old ThrowableImpactEvent next to the ProjectileImpactEvent
            boolean old = false;
            if (projectile instanceof legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityThrowable t) {
                var oldEvent = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.ThrowableImpactEvent(t, result);
                old = bus.mcc$hasListeners(oldEvent.getClass()) && bus.post(oldEvent);
            }
            return bus.post(event) | old;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
