// Dev scan (run-server-script.sh): force-generates a square of chunks in a dimension far from the main island and counts the blocks in
// y -8..40 that are not what the dimension makes itself. Dimensions: lines of <gameDir>/scan-dim.txt, "<dimension> <side in chunks> [first chunk x z]" (default minecraft:the_end 20).
if (typeof G === "undefined") G = {};
if (typeof SCAN === "undefined") {
  var dimFile = java.nio.file.Paths.get("scan-dim.txt"); // one "<dimension> <side in chunks> [first chunk x z]" per line
  var lines = java.nio.file.Files.exists(dimFile) ? java.nio.file.Files.readAllLines(dimFile) : java.util.Collections.singletonList("minecraft:the_end 20");
  SCAN_DIMS = [];
  for (var li = 0; li < lines.size(); li++) { var parts = String(lines.get(li)).trim().split(/\s+/); if (parts[0]) SCAN_DIMS.push({ dim: parts[0], n: parseInt(parts[1] || "20"), cx: parseInt(parts[2] || "125"), cz: parseInt(parts[3] || "125") }); }
  SCAN = null;
}
if ((SCAN === null || !SCAN.done) && tick >= 150) {
  if (SCAN === null) { var nd = SCAN_DIMS.shift(); SCAN = { i: 0, n: nd.n, counts: {}, chunksWith: 0, dim: nd.dim, cx: nd.cx, cz: nd.cz, done: false }; }
  var RK = Java.type("net.minecraft.resources.ResourceKey"), RL = Java.type("net.minecraft.resources.ResourceLocation"), Regs = Java.type("net.minecraft.core.registries.Registries");
  var level = server.getLevel(RK.create(Regs.DIMENSION, RL.parse(SCAN.dim)));
  var CS = Java.type("net.minecraft.world.level.chunk.status.ChunkStatus");
  var BP = Java.type("net.minecraft.core.BlockPos");
  var BR = Java.type("net.minecraft.core.registries.BuiltInRegistries");
  var allowed = {};
  ["air", "cave_air", "void_air", "end_stone", "obsidian", "netherrack", "bedrock", "lava", "nether_quartz_ore", "glowstone", "magma_block", "soul_sand",
    "gravel", "nether_gold_ore", "chorus_plant", "chorus_flower", "fire", "nether_wart_block", "warped_wart_block", "crimson_nylium", "warped_nylium",
    "crimson_stem", "warped_stem", "shroomlight", "basalt", "blackstone", "soul_soil", "crimson_fungus", "warped_fungus", "crimson_roots", "warped_roots",
    "nether_sprouts", "weeping_vines", "twisting_vines", "weeping_vines_plant", "twisting_vines_plant", "ancient_debris", "red_mushroom", "brown_mushroom",
    "soul_fire", "nether_bricks", "nether_brick_fence", "nether_brick_stairs", "spawner", "chest"].forEach(function (n) { allowed["minecraft:" + n] = 1; });
  var per = 12, k = 0;
  while (k < per && SCAN.i < SCAN.n * SCAN.n) {
    var cx = SCAN.cx + (SCAN.i % SCAN.n), cz = SCAN.cz + Math.floor(SCAN.i / SCAN.n);
    SCAN.i++; k++;
    var ch = level.getChunk(cx, cz, CS.FULL, true);
    var found = 0, mp = new BP.MutableBlockPos();
    for (var x = 0; x < 16; x++) for (var z = 0; z < 16; z++) for (var y = Math.max(-8, level.getMinBuildHeight()); y < 40; y++) {
      var st = ch.getBlockState(mp.set(cx * 16 + x, y, cz * 16 + z));
      if (st.isAir()) continue;
      var id = String(BR.BLOCK.getKey(st.getBlock()));
      if (allowed[id]) continue;
      SCAN.counts[id] = (SCAN.counts[id] || 0) + 1; found++;
    }
    if (found) SCAN.chunksWith++;
  }
  if (SCAN.i >= SCAN.n * SCAN.n) {
    SCAN.done = true;
    var ks = Object.keys(SCAN.counts).sort(function (a, b) { return SCAN.counts[b] - SCAN.counts[a]; }), s = "";
    for (var j = 0; j < ks.length && j < 40; j++) s += "\n   " + ks[j] + " " + SCAN.counts[ks[j]];
    var rep = SCAN;
    log.info("SERVERSCRIPT: SCAN " + rep.dim + " from chunk " + rep.cx + "," + rep.cz + " " + rep.n + "x" + rep.n + " chunks, " + rep.chunksWith + " with foreign blocks, " + ks.length + " ids" + s);
    if (SCAN_DIMS.length > 0) SCAN = null;
  }
}
