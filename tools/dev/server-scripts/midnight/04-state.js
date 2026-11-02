var res = "";
var es = [];
server.getAllLevels().forEach(function (l) {
  l.getAllEntities().forEach(function (e) { if (!(e instanceof T("net.minecraft.server.level.ServerPlayer"))) es.push(String(l.dimension().location()) + ":" + e.getType().builtInRegistryHolder().key().location() + "@" + e.blockPosition().toShortString()); });
});
res += "entities " + es.length + ": " + es.filter(function (s) { return s.indexOf(":midnight:") >= 0 }).join(" ") + "\n";
res += "players " + server.getPlayerList().getPlayers() + " daytime " + server.overworld().getDayTime() + "\n";
res;
