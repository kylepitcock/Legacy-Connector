package org.kylepitcock.legacyconnector.api.event;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/** Phase {@code REGISTERED}: every legacy mod ran preInit, init and postInit; all their blocks, items and entities are registered. */
public class LegacyRegistrationFinishedEvent extends Event implements IModBusEvent {}
