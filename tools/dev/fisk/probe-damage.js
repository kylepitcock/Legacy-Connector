// Taking damage in a suit: fall damage with FALL_RESISTANCE (Iron Man 8), arrow immunity (Captain Cold? no: Citizen Steel / Iron Man projectile immunity), fire immunity, generic damage resistance.
var DamageTypes = Java.type("net.minecraft.world.damagesource.DamageTypes");
function src(key) { return server.overworld().damageSources().source(key); }
function setup(name, hero) { var p = join(name); flat(1500, 1500, 100); tp(1500.5, 101, 1500.5); if (hero) wear(hero); return p; }
var plain = setup("DmgPlain2", null), iron = setup("DmgIron2", "iron_man");
function trial(p, label, f) { p.setHealth(20); p.invulnerableTime = 0; p.hurtTime = 0; var h = p.getHealth(); try { f(p); } catch (e) { return label + " EXC " + e; } return label + ": " + (h - p.getHealth()).toFixed(2); }
scenario([function () { return 70; }, function () {
  [["plain", plain], ["iron_man", iron]].forEach(function (e) {
    var p = e[1]; P = p;
    out(e[0] + " " + trial(p, "fall 20 blocks", function (q) { q.causeFallDamage(20, 1, src(DamageTypes.FALL)); }));
    out(e[0] + " " + trial(p, "arrow 6", function (q) { q.hurt(src(DamageTypes.ARROW), 6); }));
    out(e[0] + " " + trial(p, "fire 4", function (q) { q.hurt(src(DamageTypes.IN_FIRE), 4); }));
    out(e[0] + " " + trial(p, "generic 6", function (q) { q.hurt(src(DamageTypes.GENERIC), 6); }));
    out(e[0] + " " + trial(p, "explosion 10", function (q) { q.hurt(src(DamageTypes.EXPLOSION), 10); }));
  });
}]);
"damage probe started"
