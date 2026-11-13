package dev.latvian.mods.kubejs.event;

/** Minimal fixture matching KubeJS 7's Java event callback contract. */
@FunctionalInterface
public interface IEventHandler {
    Object onEvent(KubeEvent event);
}
