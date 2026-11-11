package dev.latvian.mods.kubejs.plugin.builtin.event;

import dev.latvian.mods.kubejs.event.IEventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Minimal fixture for KubeJS 7's targeted block-entity tick event handler. */
public final class BlockEvents {
    public static final RecordingEventHandler BLOCK_ENTITY_TICK = new RecordingEventHandler();

    private BlockEvents() {
    }

    public static final class RecordingEventHandler {
        private final Map<ScriptType, Map<Object, IEventHandler>> listeners = new EnumMap<>(ScriptType.class);
        public int listenJavaCalls;
        public int callCalls;
        public boolean canListenEvents;

        public void listenJava(ScriptType type, Object target, IEventHandler handler) {
            if (type == ScriptType.STARTUP) {
                throw new IllegalArgumentException("BlockEvents.blockEntityTick does not support STARTUP");
            }
            listeners.computeIfAbsent(type, ignored -> new HashMap<>()).put(target, handler);
            listenJavaCalls++;
        }

        /** Mirrors the script-facing path, which is closed after script loading. */
        public Object call(Context context, Scriptable scope, Scriptable thisObject, Object[] arguments) {
            callCalls++;
            if (!canListenEvents) {
                throw new IllegalStateException("Event handler can only be registered during script loading");
            }
            return null;
        }

        public IEventHandler listener(ScriptType type, Object target) {
            Map<Object, IEventHandler> byTarget = listeners.get(type);
            return byTarget == null ? null : byTarget.get(target);
        }

        public int listenerCount() {
            return listeners.values().stream().mapToInt(Map::size).sum();
        }

        public void reset() {
            listeners.clear();
            listenJavaCalls = 0;
            callCalls = 0;
            canListenEvents = false;
        }
    }
}
