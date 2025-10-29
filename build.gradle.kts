plugins {
    kotlin("multiplatform") version "2.2.10"
}

repositories {
    mavenCentral()
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "mdless"
            }
            executable("test") {
                entryPoint = "TestMainKt.main"
                baseName = "mdless-test"
            }
        }
    }

    linuxArm64 {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "mdless-arm64"
            }
            executable("test") {
                entryPoint = "TestMainKt.main"
                baseName = "mdless-arm64-test"
            }
        }
    }
}

// Task to build both targets
tasks.register("buildAll") {
    dependsOn("linkDebugExecutableLinuxX64", "linkDebugExecutableLinuxArm64")
}

// Task to build both test targets
tasks.register("buildTests") {
    dependsOn("linkDebugTestLinuxX64", "linkDebugTestLinuxArm64")
}

// Task to build x64 version
tasks.register("buildX64") {
    dependsOn("linkDebugExecutableLinuxX64")
}

// Task to build ARM64 version
tasks.register("buildArm64") {
    dependsOn("linkDebugExecutableLinuxArm64")
}