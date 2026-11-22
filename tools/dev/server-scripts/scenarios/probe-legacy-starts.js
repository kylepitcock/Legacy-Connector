// Dev probe: generates a square of chunks in the overworld and lists the structure starts of legacy world generators
// (witchesandmore:legacy/... etc., recorded by LegacyGeneratorStructures) found in them, and the biomes seen.
if (typeof PROBED === "undefined" && tick >= 150) {
  PROBED = true;
  var RK = Java.type("net.minecraft.resources.ResourceKey"), RL = Java.type("net.minecraft.resources.ResourceLocation"), Regs = Java.type("net.minecraft.core.registries.Registries");
  var CS = Java.type("net.minecraft.world.level.chunk.status.ChunkStatus"), BP = Java.type("net.minecraft.core.BlockPos");
  var level = server.getLevel(RK.create(Regs.DIMENSION, RL.parse("minecraft:overworld")));
  var R = 18, found = {}, biomes = {}, t0 = Date.now();
  var reg = level.registryAccess().registryOrThrow(Regs.STRUCTURE);
  for (var cx = -R; cx <= R; cx++) for (var cz = -R; cz <= R; cz++) {
    var ch = level.getChunk(cx + 40, cz + 40, CS.FULL, true);
    var it = ch.getAllStarts().entrySet().iterator();
    while (it.hasNext()) {
      var e = it.next(), id = "" + reg.getKey(e.getKey());
      if (id.indexOf("minecraft:") == 0) continue;
      found[id] = (found[id] || 0) + 1;
    }
    var b = "" + level.getBiome(new BP((cx + 40) * 16 + 8, 64, (cz + 40) * 16 + 8)).unwrapKey().get().location();
    biomes[b] = (biomes[b] || 0) + 1;
  }
  var s = "";
  for (var k in found) s += k + "=" + found[k] + " ";
  var s2 = "";
  for (var k2 in biomes) s2 += k2 + "=" + biomes[k2] + " ";
  log.info("SERVERSCRIPT: starts (" + (Date.now() - t0) + " ms): " + s);
  log.info("SERVERSCRIPT: biomes: " + s2);
}
