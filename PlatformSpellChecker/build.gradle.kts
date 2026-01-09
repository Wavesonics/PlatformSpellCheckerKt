import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.android.library)
	alias(libs.plugins.dokka)
	alias(libs.plugins.maven.central.publish)
}

kotlin {
	androidTarget {
		compilerOptions {
			jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
		}
	}

	jvm("desktop") {
		compilerOptions {
			jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
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
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
	}

	compileOptions {
		val jvmVersion = libs.versions.jvm.get()
		sourceCompatibility = JavaVersion.toVersion(jvmVersion)
		targetCompatibility = JavaVersion.toVersion(jvmVersion)
	}
}

// Publishing configuration
group = "com.darkrockstudios"
version = project.findProperty("library.version") as String? ?: "1.0.0-SNAPSHOT"

mavenPublishing {
	coordinates(
		groupId = "com.darkrockstudios",
		artifactId = "platform-spellcheckerkt",
		version = version.toString()
	)

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

	// Automatically signs artifacts when publishing
	signAllPublications()

	// Publishes to Maven Central via central.sonatype.com
	publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
}

