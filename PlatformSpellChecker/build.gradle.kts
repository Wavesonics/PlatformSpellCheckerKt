plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

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

// Task to run the spell check test
tasks.register<JavaExec>("runSpellCheckTest") {
    group = "verification"
    description = "Run the Windows Spell Checker test"
    mainClass.set("com.darkrockstudios.libs.platformspellchecker.SpellCheckTestKt")

    val desktopMain = kotlin.targets.getByName("desktop").compilations.getByName("main")
    classpath = (desktopMain.output.allOutputs + desktopMain.runtimeDependencyFiles) as FileCollection
}
