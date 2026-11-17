// Dev probe: what a legacy 1.7.10 IWorldGenerator is handed as its `chunkGenerator` argument in each dimension. A generator registered late
// (so it runs from the catch-all decorator) records the class of the argument and which vanilla provider classes it is an instance of;
// mods that tell the dimensions apart this way (`chunkGenerator instanceof ChunkProviderHell`, falling to a default surface branch)
// generate overworld content in the End and the Nether when the argument is a plain placeholder.
if (typeof PGA === "undefined") PGA = { stage: 0, seen: {} };
if (PGA.stage === 0 && tick >= 150) {
  PGA.stage = 1;
  var IWG = Java.type("legacyconnector.v1_7_10.cpw.mods.fml.common.IWorldGenerator");
  var GR = Java.type("legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry");
  var W = Java.type("legacyconnector.v1_7_10.net.minecraft.world.World");
  var kinds = ["Hell", "End", "Generate", "Flat"].map(function (k) { return [k, Java.type("legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProvider" + k).class]; });
  var Gen = Java.extend(IWG, {
    generate: function (r, cx, cz, w, g, p) {
      var id = W.mcc$get$provider(w).dimensionId;
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
  log.info("SERVERSCRIPT: GENERATOR ARGS" + (out || " (the probe generator never ran)"));
}
