#!/bin/sh
# Downloads legacy reference data used by tools/legacy_api.py and the shim audit into build/devtools/.
set -e
cd "$(dirname "$0")/.."
D=build/devtools/mcp
mkdir -p "$D"
M=https://maven.minecraftforge.net
curl -sfo "$D/mcp_stable-12-1.7.10.zip" $M/de/oceanlabs/mcp/mcp_stable/12-1.7.10/mcp_stable-12-1.7.10.zip
curl -sfo "$D/mcp_stable-39-1.12.zip" $M/de/oceanlabs/mcp/mcp_stable/39-1.12/mcp_stable-39-1.12.zip
curl -sfo "$D/forge1710-userdev.jar" $M/net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/forge-1.7.10-10.13.4.1614-1.7.10-userdev.jar
curl -sfo "$D/srg1710.zip" $M/de/oceanlabs/mcp/mcp/1.7.10/mcp-1.7.10-srg.zip
curl -sfo "$D/srg1122.zip" $M/de/oceanlabs/mcp/mcp_config/1.12.2/mcp_config-1.12.2.zip
echo "Reference data in $D"
