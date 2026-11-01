# launch-pack.py

Starts a Prism instance's Minecraft client without Prism (offline account, pure Python stdlib), so an unattended run can use a real pack.

- `python tools/dev/launch-pack.py --dry-run` prints the full java command and lists missing classpath jars (exit code 1 if any).
- It reads the instance's `mmc-pack.json`, `instance.cfg` (JavaPath, JvmArgs, MaxMemAlloc) and the Prism `meta/` + `libraries/` + `assets/`
  (default `C:/Users/Kyleb/AppData/Roaming/PrismLauncher`, override with `PRISM_DIR`). The original instance is only read.
- The game runs in `--game-dir` (default `run-atm10`, a copy of the pack's minecraft dir). `--mods-extra <jar>` copies a jar into its `mods/`.
- `--control` adds the screenshot-test properties for the remote-control session: `CONTROL=run-atm10/control tools/dev/ctl.sh "cmd time set noon" "shot a"`
  (also `--world-type`, `--render-distance`). Without it the pack starts normally.
- Other options: `--width/--height`, `--mem MB`, `--java`, `--username`, `--extra-jvm "-Dfoo=1"`.
- The legacyconnector mod jar must be built first (`./gradlew bundleJar`); remove an older copy from `run-atm10/mods` before copying a new one.
