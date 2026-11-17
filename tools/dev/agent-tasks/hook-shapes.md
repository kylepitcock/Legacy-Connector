# Hook shapes of the pre-1.7 forward layers (1.6.4, 1.5.2, 1.4.7)

A mod overrides a game method with the descriptor of its own version. When the 1.7.10 shim calls the method with another descriptor the override is
never called and nothing reports it (a "hook shape" gap). The bridge table `loader/.../forward/<ver>/bridge-methods.tsv` (class `BridgeMethods`) adds a
1.7.10-shaped method to the mod class that calls the original; calls of the old method on shim types are rewritten to the new one.

Table columns: `owner  oldName  oldDesc  newName  newDesc  [from]`. The optional sixth column lists, for each OLD parameter, the index of the NEW parameter
it takes its value from (`0,2` drops the new second parameter, `1` takes only the new second one). An old `void` hook whose new form answers a boolean is
bridged to "true" (handled). int/float/double, Block/Item/id and reference-type differences are converted.

## Enumerating the gaps (offline)

- `python tools/dev/hook_shapes.py <1.6.4|1.5.2|1.4.7>` (needs `build/generated/shimIndex`, i.e. one `tools/dev/gate.sh index`): every non-static, non-private
  vanilla method of the version's `legacy/<ver>/methods.txt` that the 1.7.10 shim (index) lacks under that descriptor, not covered by a rename, retarget or
  bridge row. `SAMENAME` = the shim has a method of that name with another descriptor (a hook shape gap or a call the facades cover), `NONE` = renamed/removed.
  Most `SAMENAME` lines are CALLS (World.setBlock with ids, packet handlers) that the `facade/` classes cover; only overridable hooks need a bridge row.
- `python tools/dev/forge_hook_shapes.py <ver> <forge universal jar>`: the same for the Forge/FML classes of the Forge universal jar (obfuscated names mapped
  through `legacy/<ver>/notch.txt`). Result 2026-11-16: only `IEntityAdditionalSpawnData` read/writeSpawnData (Guava streams, handled reflectively by
  `LegacySpawnData`) differs for hooks mods implement.

## Not bridgeable with the table (listed, not done)

- `ICommandSender.sendChatToPlayer(ChatMessageComponent)` (custom senders): the new `addChatMessage(IChatComponent)` has an unrelated parameter type.
- `Item.getArmorTexture(stack, entity, slot, int layer)` (Forge 1.6.4) vs `(..., String type)`: needs a derived value.
- `IBlockAccess` implementations with id based methods, `Block.getAmbientOcclusionLightValue(world, x, y, z)` (old has more parameters than the new one).
