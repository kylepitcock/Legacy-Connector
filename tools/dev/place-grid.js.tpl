// place.js: names in global NAMES (comma list) placed in a grid at y=200 with a platform
var names = "__NAMES__".split(",");
var src = server.createCommandSourceStack().withSuppressedOutput();
function run(c) { server.getCommands().performPrefixedCommand(src, c); }
run("fill -14 199 -4 14 199 20 minecraft:smooth_stone");
run("fill -14 200 -4 14 212 20 minecraft:air");
var x = -7, z = 2;
for (var i = 0; i < names.length; i++) {
  run("setblock " + x + " 200 " + z + " __NS__:" + names[i]);
  x += 2; if (x > 7) { x = -7; z += 2; }
}
"placed " + names.length;
