// 1.12.2 twin of probe-generator-args.js: what a legacy IWorldGenerator is handed as its `chunkGenerator` argument in each dimension.
if (typeof PGA === "undefined") PGA = { stage: 0, seen: {} };
if (PGA.stage === 0 && tick >= 150) {
  PGA.stage = 1;
  var IWG = Java.type("legacyconnector.v1_12_2.net.minecraftforge.fml.common.IWorldGenerator");
  var GR = Java.type("legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.GameRegistry");
  var kinds = ["Hell", "End", "Overworld", "Flat"].map(function (k) { return [k, Java.type("legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGenerator" + k).class]; });
  var Gen = Java.extend(IWG, {
    generate: function (r, cx, cz, w, g, p) {
      var id = w.provider.getDimension();
      if (PGA.seen[id] !== undefined) return;
      var s = String(g.getClass().getName()) + " instanceof:";
      kinds.forEach(function (k) { if (k[1].isInstance(g)) s += " " + k[0]; });
      PGA.seen[id] = s;
    }
  });
  GR.registerWorldGenerator(new Gen(), 1000);
}
if (PGA.stage === 1 && tick >= 153) {
  PGA.stage = 2;
  var RK = Java.type("net.minecraft.resources.ResourceKey"), RL = Java.type("net.minecraft.resources.ResourceLocation"), Regs = Java.type("net.minecraft.core.registries.Registries");
  var CS = Java.type("net.minecraft.world.level.chunk.status.ChunkStatus");
  ["minecraft:the_end", "minecraft:the_nether", "minecraft:overworld"].forEach(function (d) {
    server.getLevel(RK.create(Regs.DIMENSION, RL.parse(d))).getChunk(300, 300, CS.FULL, true);
  });
  var out = "";
  for (var id in PGA.seen) out += "\n   dimension " + id + ": generator arg " + PGA.seen[id];
  log.info("SERVERSCRIPT: GENERATOR ARGS 1.12.2" + (out || " (the probe generator never ran)"));
}
