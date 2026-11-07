// Chunk-churn soak (#161): a survival player teleports to far-apart spots every 60 ticks so thousands of chunks load and unload; logs heap, loaded chunks, entities.
if (typeof G === "undefined") G = {};
function cmd(c) { server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c); }
if (tick == 0) {
  G.sp = realPlayer("Soak");
  G.sp.setGameMode(T("net.minecraft.world.level.GameType").SURVIVAL);
  cmd("tick rate 100"); cmd("gamerule doDaylightCycle false"); cmd("time set midnight"); cmd("difficulty hard");
  cmd("effect give Soak minecraft:resistance 99999 4 true"); cmd("effect give Soak minecraft:regeneration 99999 4 true");
  G.i = 0;
}
if (tick > 0 && tick % 60 == 0) {
  G.i++;
  var a = G.i * 2.399963, r = 400 + (G.i * 53) % 4000;
  var x = Math.floor(Math.cos(a) * r), z = Math.floor(Math.sin(a) * r);
  var lv = server.overworld();
  try { G.sp.teleportTo(lv, x, 100, z, 0, 0); } catch (e) { log.info("SERVERSCRIPT: teleport failed " + e); }
  if (G.i % 10 == 0) log.info("SERVERSCRIPT: player at " + Math.floor(G.sp.getX()) + "," + Math.floor(G.sp.getZ()) + " wanted " + x + "," + z);
}
if (tick > 0 && tick % 600 == 0) {
  var total = 0, chunks = 0;
  server.getAllLevels().forEach(function (lv) {
    var it = lv.getAllEntities().iterator(); while (it.hasNext()) { it.next(); total++; }
    chunks += lv.getChunkSource().getLoadedChunksCount();
  });
  var rt = T("java.lang.Runtime").getRuntime();
  log.info("SERVERSCRIPT: t" + tick + " entities=" + total + " chunks=" + chunks + " heapMB=" + Math.round((rt.totalMemory() - rt.freeMemory()) / 1048576));
}
