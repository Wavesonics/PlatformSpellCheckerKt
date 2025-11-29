import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
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

// Publishing configuration
group = "com.darkrockstudios"
version = project.findProperty("library.version") as String? ?: "1.0.0-SNAPSHOT"

publishing {
    publications {
        publications.withType<MavenPublication> {
            artifactId = "platform-spellcheckerkt"

            pom {
                name.set("PlatformSpellChecker")
                description.set("A Kotlin Multiplatform library providing spell checking functionality for Android, iOS, and Desktop platforms")
                url.set("https://github.com/Wavesonics/PlatformSpellCheckerKt")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("wavesonics")
                        name.set("Adam Brown")
                        url.set("https://github.com/Wavesonics")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Wavesonics/PlatformSpellCheckerKt.git")
                    developerConnection.set("scm:git:ssh://github.com/Wavesonics/PlatformSpellCheckerKt.git")
                    url.set("https://github.com/Wavesonics/PlatformSpellCheckerKt")
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = System.getenv("OSSRH_USERNAME") ?: project.findProperty("ossrhUsername") as String?
                password = System.getenv("OSSRH_PASSWORD") ?: project.findProperty("ossrhPassword") as String?
            }
        }
    }
}

signing {
    // Support for in-memory key (CI) or keyring file (local)
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")

    if (signingKey != null && signingPassword != null) {
        // CI: Use in-memory key from environment variables
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    } else if (project.hasProperty("signing.keyId")) {
        // Local: Use keyring file from gradle.properties
        // Requires: signing.keyId, signing.password, signing.secretKeyRingFile
        sign(publishing.publications)
    } else {
        // Skip signing if no credentials are configured (useful for local testing)
        // Note: Maven Central requires signed artifacts for actual publishing
        isRequired = false
    }
}

