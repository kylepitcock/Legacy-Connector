// Healing factor: two players at 10 hp with 19 food and no saturation (the 80-tick heal): the one in a regeneration suit heals faster (ASMHooks.getHealRate).
var A = join("PeterHealer"); flat(400, 400, 100); tp(400.5, 101, 400.5); wear("deadpool_xmen");
var Ap = P;
var B = join("PlainHealer"); tp(406.5, 101, 400.5);
var Bp = P;
function prep(pl) { pl.setHealth(10); pl.getFoodData().setFoodLevel(19); pl.getFoodData().setSaturation(0); }
scenario([function () { return 5; }, function () {
  P = Ap; out("worn " + worn() + " mods=" + hero("deadpool_xmen").getModifiers());
  prep(Ap); prep(Bp); return 170;
}, function () { out("after 170 ticks: suit hp " + Ap.getHealth() + " plain hp " + Bp.getHealth()); }]);
"heal probe started"
