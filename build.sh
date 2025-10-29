#!/usr/bin/env bash
set -euo pipefail

# Build script using Gradle for cross-compilation
# Usage:
#   ./build.sh          # Build x64 version (default)
#   ./build.sh arm64    # Build ARM64 version for Raspberry Pi
#   ./build.sh all      # Build both versions

TARGET="${1:-x64}"

case "$TARGET" in
    "x64")
        echo "[build] Building x64 version..."
        ./gradlew buildX64
        echo "[build] Done: build/bin/linuxX64/debugExecutable/mdless.kexe"
        ;;
    "arm64")
        echo "[build] Building ARM64 version for Raspberry Pi..."
        ./gradlew buildArm64
        echo "[build] Done: build/bin/linuxArm64/debugExecutable/mdless-arm64.kexe"
        ;;
    "all")
        echo "[build] Building both x64 and ARM64 versions..."
        ./gradlew buildAll
        echo "[build] Done:"
        echo "  x64:   build/bin/linuxX64/debugExecutable/mdless.kexe"
        echo "  ARM64: build/bin/linuxArm64/debugExecutable/mdless-arm64.kexe"
        ;;
    *)
        echo "Usage: $0 [x64|arm64|all]"
        echo "  x64   - Build x64 version (default)"
        echo "  arm64 - Build ARM64 version for Raspberry Pi"
        echo "  all   - Build both versions"
        exit 1
        ;;
esac
