// Dev probe: what a 1.7.10 mod sees through its World of the End and the Nether (dimension id, provider, biome, heights over the void).
if (typeof PROBED === "undefined" && tick >= 150) {
  PROBED = true;
  var RK = Java.type("net.minecraft.resources.ResourceKey"), RL = Java.type("net.minecraft.resources.ResourceLocation"), Regs = Java.type("net.minecraft.core.registries.Registries");
  var W = Java.type("legacyconnector.v1_7_10.net.minecraft.world.World");
  ["minecraft:the_end", "minecraft:the_nether", "minecraft:overworld"].forEach(function (d) {
    var level = server.getLevel(RK.create(Regs.DIMENSION, RL.parse(d)));
    var CS = Java.type("net.minecraft.world.level.chunk.status.ChunkStatus");
    level.getChunk(125, 125, CS.FULL, true);
    var w = W.mcc$wrap(level), p = W.mcc$get$provider(w), x = 2000, z = 2000, s = d + ":";
    s += " dimId=" + p.dimensionId + " provider=" + p.getClass().getName() + " surface=" + p.isSurfaceWorld() + " hasNoSky=" + p.hasNoSky + " world.getHeight=" + w.getHeight() + " actualHeight=" + w.getActualHeight() + " horizon=" + p.getHorizon();
    s += " biome=" + w.getBiomeGenForCoords(x, z).biomeName + "(" + w.getBiomeGenForCoords(x, z).biomeID + ")";
    s += " heightValue=" + w.getHeightValue(x, z) + " topSolid=" + w.getTopSolidOrLiquidBlock(x, z) + " precip=" + w.getPrecipitationHeight(x, z);
    s += " block(y=0)=" + w.getBlock(x, 0, z) + " block(y=-1)=" + w.getBlock(x, -1, z) + " block(y=63)=" + w.getBlock(x, 63, z);
    log.info("SERVERSCRIPT: PROBE " + s);
  });
}
