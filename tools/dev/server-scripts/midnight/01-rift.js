var out = "";
sp = realPlayer("Tester");
out += "player " + sp + "\n";
levels = {};
server.getAllLevels().forEach(function (l) { levels[String(l.dimension().location())] = l; });
out += Object.keys(levels).join(",") + "\n";
out;
