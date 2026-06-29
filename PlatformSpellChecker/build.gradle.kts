import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.android.library)
	alias(libs.plugins.dokka)
	alias(libs.plugins.maven.central.publish)
}

dokka {
	moduleName.set("PlatformSpellChecker")
	dokkaSourceSets.configureEach {
		includes.from("Module.md")
		sourceLink {
			localDirectory.set(rootDir)
			remoteUrl("https://github.com/Darkrock-Studios/PlatformSpellCheckerKt/blob/main")
			remoteLineSuffix.set("#L")
		}
	}
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

		androidInstrumentedTest.dependencies {
			implementation(libs.kotlin.test)
			implementation(libs.kotlinx.coroutines.test)
			implementation(libs.androidx.junit)
			implementation(libs.androidx.test.runner)
		}
	}
}

android {
	namespace = "com.darkrockstudios.libs.platformspellchecker"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
		url.set("https://github.com/Darkrock-Studios/PlatformSpellCheckerKt")

		licenses {
			license {
				name.set("MIT License")
				url.set("https://opensource.org/licenses/MIT")
			}
		}

		developers {
			developer {
				id.set("darkrock-studios")
				name.set("Adam Brown")
				url.set("https://github.com/Darkrock-Studios")
			}
		}

		scm {
			connection.set("scm:git:git://github.com/Darkrock-Studios/PlatformSpellCheckerKt.git")
			developerConnection.set("scm:git:ssh://github.com/Darkrock-Studios/PlatformSpellCheckerKt.git")
			url.set("https://github.com/Darkrock-Studios/PlatformSpellCheckerKt")
		}
	}

	// Automatically signs artifacts when publishing
	signAllPublications()

	// Publishes to Maven Central via central.sonatype.com
	publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
}

