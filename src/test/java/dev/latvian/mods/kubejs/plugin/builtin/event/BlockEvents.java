package dev.latvian.mods.kubejs.plugin.builtin.event;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;

/** Minimal fixture for KubeJS 7's targeted block-entity tick event handler. */
public final class BlockEvents {
    public static final RecordingEventHandler BLOCK_ENTITY_TICK = new RecordingEventHandler();

    private BlockEvents() {
    }

    public static final class RecordingEventHandler {
        public Object[] lastArguments;
        public Context lastContext;
        public int calls;

        public Object call(Context context, Scriptable scope, Scriptable thisObject, Object[] arguments) {
            lastContext = context;
            lastArguments = arguments;
            calls++;
            return null;
        }

        public void reset() {
            lastArguments = null;
            lastContext = null;
            calls = 0;
        }
    }
}
