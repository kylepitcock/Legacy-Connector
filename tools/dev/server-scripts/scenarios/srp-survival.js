// Survival soak: a real player walks around at night with the tick rate raised; logs the entity census per type (SRParasites runaway hunt, #161).
if (typeof G === "undefined") G = {};
function cmd(c) { server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c); }
if (tick == 0) {
  G.sp = realPlayer("Soak");
  G.sp.setGameMode(T("net.minecraft.world.level.GameType").SURVIVAL);
  cmd("tick rate 200"); cmd("gamerule doDaylightCycle false"); cmd("time set midnight"); cmd("difficulty hard");
  cmd("effect give Soak minecraft:resistance 99999 4 true"); cmd("effect give Soak minecraft:regeneration 99999 4 true");
  G.x = 0; G.z = 0;
}
if (tick > 0 && tick % 60 == 0) {
  var a = tick / 60 * 0.7;
  G.x = Math.cos(a) * 150 + tick / 40; G.z = Math.sin(a) * 150;
  var lv = server.overworld();
  var y = lv.getHeight(T("net.minecraft.world.level.levelgen.Heightmap").Types.MOTION_BLOCKING, Math.floor(G.x), Math.floor(G.z)) + 1;
  G.sp.teleportTo(lv, G.x, y, G.z, 0, 0);
}
if (tick > 0 && tick % 600 == 0) {
  var counts = {}, total = 0;
  server.getAllLevels().forEach(function (lv) {
    var it = lv.getAllEntities().iterator();
    while (it.hasNext()) {
      var e = it.next();
      var k = "" + T("net.minecraft.core.registries.BuiltInRegistries").ENTITY_TYPE.getKey(e.getType());
      counts[k] = (counts[k] || 0) + 1; total++;
    }
  });
  var top = Object.keys(counts).sort(function (a, b) { return counts[b] - counts[a]; }).slice(0, 6).map(function (k) { return k + "=" + counts[k]; });
  var rt = T("java.lang.Runtime").getRuntime();
  log.info("SERVERSCRIPT: t" + tick + " entities=" + total + " heapMB=" + Math.round((rt.totalMemory() - rt.freeMemory()) / 1048576) + " top: " + top.join(", "));
}
