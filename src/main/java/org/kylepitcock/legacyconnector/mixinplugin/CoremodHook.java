package org.kylepitcock.legacyconnector.mixinplugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a layer mixin that replays a legacy coremod's patch site (a {@code *HookMixin}, {@code *CoremodSites*}): its injectors use {@code require = 0} on purpose,
 * because a Minecraft or NeoForge update that moves the site only drops the coremod effect and leaves vanilla behaviour. {@link MixinSelfCheck} does not warn
 * about an unapplied injector of such a mixin (it only counts it). Every mixin WITHOUT this annotation is a feature mixin: an unapplied injector is logged as
 * a warning. The list is mirrored in {@code docs/wiki/Mixin-Audit.md} (checked by {@code MixinSelfCheckTest}).
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface CoremodHook {}
