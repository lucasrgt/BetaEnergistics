#!/bin/bash
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
MCP_ROOT="${BE_MCP_ROOT:-$BASE/mcp}"
cd "$BASE"

# Convert MSYS /c/ paths to Windows C:/ for javac/java compatibility
win_path() {
    echo "$1" | sed 's|^/\([a-zA-Z]\)/|\1:/|'
}

echo "=== Transpiling ==="
bash scripts/transpile.sh

echo "=== Recompiling ==="
cd "$MCP_ROOT"
echo "recompile" | java -jar RetroMCP-Java-CLI.jar
cd "$BASE"

# Windows paths for javac/java classpath
WBIN="$(win_path "$MCP_ROOT/minecraft/bin")"
WJUNIT="$(win_path "$BASE/tests/libs/junit-4.13.2.jar")"
WHAMCREST="$(win_path "$BASE/tests/libs/hamcrest-core-1.3.jar")"
WTEST_OUT="$(win_path "$BASE/tests/out")"

# Clean and compile tests
rm -rf "$BASE/tests/out"
mkdir -p "$BASE/tests/out"

echo "=== Compiling tests ==="
TEST_FILES=()
for f in "$BASE"/tests/src/net/minecraft/src/*Test.java; do
    [ -f "$f" ] || continue
    TEST_FILES+=("$(win_path "$f")")
done

if [ ${#TEST_FILES[@]} -eq 0 ]; then
    echo "No test files found."
    exit 0
fi

javac -source 1.8 -target 1.8 \
    -cp "$WBIN;$WJUNIT;$WHAMCREST" \
    -d "$WTEST_OUT" \
    "${TEST_FILES[@]}"

echo "=== Running tests ==="
# Auto-discover test classes
TEST_CLASSES=""
for f in "$BASE"/tests/src/net/minecraft/src/*Test.java; do
    [ -f "$f" ] || continue
    CLASS=$(basename "$f" .java)
    TEST_CLASSES="$TEST_CLASSES net.minecraft.src.$CLASS"
done

java -cp "$WTEST_OUT;$WBIN;$WJUNIT;$WHAMCREST" \
    org.junit.runner.JUnitCore \
    $TEST_CLASSES
