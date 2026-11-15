// Generates a square of chunks around the origin (one ring per tick) and reports chests (empty vs filled) and spawners (mob id) found
// in the loaded chunks, plus ore blocks straddling y=0 (a vein cut at y=0 shows as ore at y=0 with no ore at y=-1 below it).
// run: ./gradlew runServerSubTest -PsubTestDir=run-wb -PautostopTicks=900 -PserverTickScriptFrom=100 -PserverTickScript=<abs path>
var R = 14;
var T_ = function (n) { return Java.type(n); };
var level = server.overworld();
if (typeof ring === "undefined") { ring = 0; chests = 0; emptyChests = 0; spawners = 0; emptySpawners = 0; mobs = {}; ores0 = 0; oresBelow = 0; }
if (ring <= R && tick % 2 == 0) {
  for (var cx = -ring; cx <= ring; cx++) for (var cz = -ring; cz <= ring; cz++) {
    if (Math.max(Math.abs(cx), Math.abs(cz)) != ring) continue;
    var chunk = level.getChunk(cx, cz);
    var it = chunk.getBlockEntities().values().iterator();
    while (it.hasNext()) {
      var be = it.next();
      var cn = String(be.getClass().getName());
      if (be instanceof T_("net.minecraft.world.level.block.entity.ChestBlockEntity")) {
        chests++;
        var empty = true;
        for (var i = 0; i < be.getContainerSize(); i++) if (!be.getItem(i).isEmpty()) { empty = false; break; }
        if (empty) emptyChests++;
      } else if (be instanceof T_("net.minecraft.world.level.block.entity.SpawnerBlockEntity")) {
        spawners++;
        var nbt = be.saveWithFullMetadata(level.registryAccess());
        var s = String(nbt);
        var m = /id:"([^"]+)"/.exec(s.substring(s.indexOf("SpawnData")));
        var id = m ? m[1] : "none";
        if (id == "none" || id == "minecraft:pig" && s.indexOf("SpawnData") < 0) emptySpawners++;
        mobs[id] = (mobs[id] || 0) + 1;
      }
    }
  }
  ring++;
}
if (ring == R + 1) {
  ring++;
  "SCAN chests=" + chests + " empty=" + emptyChests + " spawners=" + spawners + " noMob=" + emptySpawners + " mobs=" + JSON.stringify(mobs);
}
