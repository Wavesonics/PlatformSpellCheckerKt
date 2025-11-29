import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // iOS targets
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.napier)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.jna)
                implementation(libs.jna.platform)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.junit)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "com.darkrockstudios.libs.platformspellchecker"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Task to build the native macOS library
tasks.register("buildNativeLibrary") {
    group = "build"
    description = "Build the native NSSpellChecker JNI library for macOS"

    doLast {
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("mac")) {
            exec {
                workingDir = file("src/desktopMain/native")
                commandLine("bash", "build.sh")
            }
        } else {
            println("Native library build is only supported on macOS")
        }
    }
}

// Make desktop compilation depend on native library build
tasks.matching { it.name.contains("compileKotlinDesktop") }.configureEach {
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        dependsOn("buildNativeLibrary")
    }
}

