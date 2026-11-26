#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."

if [[ ${MINECRAFT_EULA:-} != true ]]; then
  echo 'Read https://aka.ms/MinecraftEULA and set MINECRAFT_EULA=true if you accept it.' >&2
  exit 2
fi

# Keep each run isolated from local worlds, mods, config, and logs from earlier runs.
mkdir -p build/devtools/server-smoke
run_dir=$(mktemp -d build/devtools/server-smoke/run.XXXXXXXX)
echo "Server smoke test logs: $run_dir"
printf 'eula=true\n' > "$run_dir/eula.txt"
cat > "$run_dir/server.properties" <<'EOF'
server-ip=127.0.0.1
server-port=0
online-mode=false
level-type=minecraft:flat
generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}]}
generate-structures=false
view-distance=3
simulation-distance=3
EOF

bash ./gradlew runServerSubTest --console=plain "-PsubTestDir=$run_dir" -PautostopTicks=100 2>&1 | tee "$run_dir/gradle.log"

# A dedicated server can exit with status 0 even when startup or shutdown is incomplete.
server_log="$run_dir/logs/latest.log"
if [[ ! -f $server_log || -d $run_dir/crash-reports ]] ||
   ! grep -Eq 'Done \([0-9.]+s\)! For help, type "help"' "$server_log" ||
   ! grep -Fq 'Test autostop after 100 ticks: saving and stopping' "$server_log" ||
   ! grep -Fq 'All dimensions are saved' "$server_log"; then
  echo "Server smoke test did not complete startup, 100 ticks, and a clean save. See $run_dir." >&2
  exit 1
fi

echo 'Server smoke test passed: started, ticked 100 times, and saved before exiting.'