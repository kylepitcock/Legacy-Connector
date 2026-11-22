"""Prints a minecraft:overworld noise_settings JSON whose terrain is solid stone from the bottom (-64) to the top (319), for
tools/dev/ore-probe.sh (ORE_PROBE_TALL=1): ore veins only replace stone, so a normal terrain hides what happens above it."""
import json

zero = 0.0
router = {k: zero for k in (
    "barrier", "fluid_level_floodedness", "fluid_level_spread", "lava", "temperature", "vegetation", "continents",
    "erosion", "depth", "ridges", "initial_density_without_jaggedness", "vein_toggle", "vein_ridged", "vein_gap")}
router["final_density"] = 1.0
settings = {
    "sea_level": 63,
    "disable_mob_generation": False,
    "aquifers_enabled": False,
    "ore_veins_enabled": False,
    "legacy_random_source": False,
    "default_block": {"Name": "minecraft:stone"},
    "default_fluid": {"Name": "minecraft:water", "Properties": {"level": "0"}},
    "noise": {"min_y": -64, "height": 384, "size_horizontal": 1, "size_vertical": 2},
    "noise_router": router,
    "spawn_target": [],
    "surface_rule": {"type": "minecraft:block", "result_state": {"Name": "minecraft:stone"}},
}
print(json.dumps(settings))
