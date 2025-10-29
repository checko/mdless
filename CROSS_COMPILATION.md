# Cross-Compilation Setup for mdless

This document explains how to build the mdless Markdown viewer for multiple platforms (Linux x64 and ARM64 for Raspberry Pi) using Gradle and Kotlin Multiplatform.

## Table of Contents

1. [Why Gradle for Multi-Platform Builds](#why-gradle-for-multi-platform-builds)
2. [Migration from Direct Kotlin/Native](#migration-from-direct-kotlinnative)
3. [Source Code Structure Changes](#source-code-structure-changes)
4. [Platform-Specific Code Handling](#platform-specific-code-handling)
5. [Build Configuration](#build-configuration)
6. [Build Commands](#build-commands)
7. [Binary Files in Git](#binary-files-in-git)

## Why Gradle for Multi-Platform Builds

### Previous Approach (Direct Kotlin/Native)
The original build system used direct `kotlinc-native` compilation:

```bash
# Old build.sh
KOTLINC="/path/to/kotlinc-native"
"$KOTLINC" -opt -o "build/mdless" src/**/*.kt
```

**Limitations:**
- Single target platform only (x64)
- Manual dependency management
- No cross-compilation support
- Hard-coded compiler paths
- Limited to command-line scripting

### Why Gradle Multiplatform?

**1. Cross-Compilation Support**
- Build for multiple architectures from single machine
- Automatic toolchain management
- Platform-specific optimizations

**2. Dependency Management**
- Automatic download of Kotlin Native toolchains
- Version management for dependencies
- Plugin ecosystem integration

**3. Build Automation**
- Declarative build configuration
- Task dependencies and parallel execution
- IDE integration (IntelliJ IDEA, VS Code)
- Standardized build lifecycle

**4. Multi-Platform Abstraction**
- `expect`/`actual` pattern for platform differences
- Shared common code with platform-specific implementations
- Type-safe platform detection

**5. Scalability**
- Easy to add new target platforms
- Reusable build logic
- Community support and documentation

## Migration from Direct Kotlin/Native

### Before: Direct Compilation
```bash
# build.sh (old)
kotlinc-native -opt -o build/mdless src/**/*.kt
```

### After: Gradle Multiplatform
```kotlin
// build.gradle.kts
kotlin {
    linuxX64 { /* x64 configuration */ }
    linuxArm64 { /* ARM64 configuration */ }
}
```

**Key Changes:**
1. **Build Tool**: `kotlinc-native` → Gradle + Kotlin Multiplatform plugin
2. **Source Structure**: Flat `src/**/*.kt` → Hierarchical `src/commonMain/`, `src/linuxX64Main/`, etc.
3. **Platform Handling**: Manual workarounds → `expect`/`actual` declarations
4. **Dependency Management**: Manual → Automatic via Gradle

## Source Code Structure Changes

### Before Migration
```
src/
├── cli/
│   ├── Main.kt
│   ├── App.kt
│   ├── Keys.kt
│   └── Options.kt
├── ir/
│   └── Ir.kt
├── layout/
│   ├── Layout.kt
│   ├── LayoutStyled.kt
│   └── Width.kt
├── pager/
│   ├── Pager.kt
│   └── Search.kt
├── parser/
│   └── Parser.kt
├── render/
│   └── Renderer.kt
├── style/
│   ├── Styler.kt
│   └── Theme.kt
└── tty/
    └── Tty.kt
```

### After Migration (Gradle Multiplatform)
```
src/
├── commonMain/kotlin/          # Shared code (expect declarations)
│   ├── cli/
│   │   ├── Main.kt
│   │   ├── App.kt
│   │   ├── Keys.kt
│   │   └── Options.kt
│   ├── ir/
│   │   └── Ir.kt
│   ├── layout/
│   │   ├── Layout.kt
│   │   ├── LayoutStyled.kt
│   │   └── Width.kt          # expect object Width
│   ├── pager/
│   ├── parser/
│   ├── render/
│   ├── style/
│   └── tty/
├── linuxX64Main/kotlin/        # x64-specific implementations
│   └── layout/
│       └── Width.kt            # actual object Width (x64 version)
├── linuxArm64Main/kotlin/      # ARM64-specific implementations
│   └── layout/
│       └── Width.kt            # actual object Width (ARM64 version)
└── nativeTest/kotlin/          # Tests
    ├── cli/
    ├── ir/
    ├── layout/
    ├── pager/
    ├── parser/
    ├── render/
    ├── run/
    ├── smoke/
    └── style/
```

### Key Structural Changes

1. **Source Set Hierarchy**:
   - `commonMain`: Code shared across all targets
   - `linuxX64Main`: x64-specific code
   - `linuxArm64Main`: ARM64-specific code

2. **Test Structure**: `test/` → `src/nativeTest/kotlin/`

3. **Entry Point**: `cli.MainKt.main` (no package prefix needed)

## Platform-Specific Code Handling

### The Problem: POSIX API Differences

Different architectures have different POSIX C API bindings in Kotlin Native:

```kotlin
// x64 expects: platform.posix.wcwidth(Int)
// ARM64 expects: platform.posix.wcwidth(UInt)
val width = platform.posix.wcwidth(codePoint)
```

### Solution: expect/actual Pattern

**1. Common Declaration (src/commonMain/kotlin/layout/Width.kt)**
```kotlin
expect object Width {
    var usePosix: Boolean
    fun charWidth(cp: Int): Int
    fun stringWidth(s: String): Int
    fun takePrefixByColumns(s: String, maxCols: Int): Int
}
```

**2. x64 Implementation (src/linuxX64Main/kotlin/layout/Width.kt)**
```kotlin
actual object Width {
    actual var usePosix: Boolean = true

    actual fun charWidth(cp: Int): Int {
        // ... common logic ...
        val w = platform.posix.wcwidth(cp)  // Int parameter for x64
        return if (w < 0) 1 else w
    }
    // ... other functions ...
}
```

**3. ARM64 Implementation (src/linuxArm64Main/kotlin/layout/Width.kt)**
```kotlin
actual object Width {
    actual var usePosix: Boolean = true

    actual fun charWidth(cp: Int): Int {
        // ... common logic ...
        val w = platform.posix.wcwidth(cp.toUInt())  // UInt parameter for ARM64
        return if (w < 0) 1 else w
    }
    // ... other functions ...
}
```

### Benefits of expect/actual

1. **Type Safety**: Compile-time guarantees for platform implementations
2. **IDE Support**: IntelliJ IDEA shows missing actual implementations
3. **Maintainability**: Clear separation of platform-specific code
4. **Extensibility**: Easy to add new platforms (e.g., Windows, macOS)

## Build Configuration

### build.gradle.kts
```kotlin
plugins {
    kotlin("multiplatform") version "2.2.10"
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "mdless"
            }
        }
    }

    linuxArm64 {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "mdless-arm64"
            }
        }
    }
}

tasks.register("buildAll") {
    dependsOn("linkDebugExecutableLinuxX64", "linkDebugExecutableLinuxArm64")
}
```

### settings.gradle.kts
```kotlin
rootProject.name = "mdless"
```

### gradle.properties
```properties
kotlin.mpp.applyDefaultHierarchyTemplate=false
```

## Build Commands

### Using Gradle Directly
```bash
# Build x64 version
./gradlew buildX64

# Build ARM64 version
./gradlew buildArm64

# Build both
./gradlew buildAll
```

### Using Build Script
```bash
# Build x64 (default)
./build.sh

# Build ARM64
./build.sh arm64

# Build both
./build.sh all
```

### Output Locations
- **x64**: `build/bin/linuxX64/debugExecutable/mdless.kexe`
- **ARM64**: `build/bin/linuxArm64/debugExecutable/mdless-arm64.kexe`

## Binary Files in Git

### What Got Added (Problem)
The initial commit included these binary/downloaded files:
- `.gradle/` (Gradle cache)
- `gradle.zip` (Gradle distribution)
- `gradle/` (extracted Gradle)
- `gradlew` (Gradle wrapper script)
- `gradle/wrapper/gradle-wrapper.jar`

### Are They Necessary?
**No, these files should not be in Git because:**
1. **Large Size**: Gradle wrapper and cache can be several MB
2. **Environment-Specific**: Generated for specific machine/OS
3. **Regeneratable**: Can be recreated by running `./gradlew wrapper`
4. **Security**: Binary files can contain vulnerabilities

### Solution: Updated .gitignore
```gitignore
# Gradle
.gradle/
gradle.zip
gradle/
gradlew
gradlew.bat
```

### What Should Be in Git
- `gradle/wrapper/gradle-wrapper.properties` (specifies Gradle version)
- `build.gradle.kts` (build configuration)
- `settings.gradle.kts` (project settings)
- `gradle.properties` (Gradle properties)

### How to Clean Repository
```bash
# Remove binary files from Git history
git rm -r --cached .gradle/
git rm gradle.zip gradle/ gradlew
git commit -m "Remove Gradle binary files from version control"

# These files will be regenerated when needed:
# - ./gradlew wrapper (regenerates gradlew and gradle/wrapper/)
# - ./gradlew build (downloads Gradle and creates .gradle/ cache)
```

## Summary

The migration to Gradle Multiplatform enables:

1. **Cross-compilation** from x64 to ARM64 for Raspberry Pi
2. **Clean architecture** with shared/common code and platform-specific implementations
3. **Type-safe platform handling** using expect/actual pattern
4. **Automated build process** with dependency management
5. **Future extensibility** for additional platforms

The key insight is that different CPU architectures can have different C API bindings in Kotlin Native, requiring platform-specific code that Gradle Multiplatform handles elegantly through its expect/actual system.