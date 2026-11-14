#!/usr/bin/env bash
# Scripted client scenarios in a real game window. See docs/wiki/Development.md ("Client scenarios").
# usage: tools/dev/verify-client.sh [scenario ...]    (no argument = all of tools/dev/verify/scenarios/*.txt; one client at a time)
# Env: JAR_ROOT (main checkout with the gitignored jar folders and run-perfmods), NOPERF=1 (no performance mods), PYTHON.
cd "$(dirname "$0")/../.." || exit 1
case "${JAVA_HOME:-}" in *21*) ;; *) export JAVA_HOME="C:/Program Files/Java/jdk-21";; esac  # the build needs JDK 21
# Windows Python cannot run /usr/bin/bash (and plain "bash" may be WSL): hand it Git Bash's Windows path
export BASH_EXE="$(cygpath -w /usr/bin/bash.exe 2>/dev/null || echo bash)"
exec "${PYTHON:-python}" tools/dev/verify/client.py run "$@"
