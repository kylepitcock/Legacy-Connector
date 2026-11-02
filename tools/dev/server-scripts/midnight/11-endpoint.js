var r3 = "";
var md = sp.level();
md.getAllEntities().forEach(function (e) { var k = String(e.getType().builtInRegistryHolder().key().location()); if (k == "midnight:rift") r3 += "rift " + e.position() + " bb " + e.getBoundingBox() + "\n"; });
var H = T("net.minecraft.world.level.levelgen.Heightmap.Types");
r3 += "mb height " + md.getHeight(H.MOTION_BLOCKING, 46, 41) + " ws " + md.getHeight(H.WORLD_SURFACE, 46, 41) + " blocks at y=106,105,104,100: " + [106, 105, 104, 100].map(function (y) { return md.getBlockState(bp(46, y, 41)).getBlock().builtInRegistryHolder().key().location().getPath(); }) + "\n";
log.info("SSOUT " + r3);
r3;
