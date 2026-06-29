// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.dokka)
}

dependencies {
    dokka(project(":PlatformSpellChecker"))
}

dokka {
    moduleName.set("PlatformSpellCheckerKt")
    dokkaPublications.html {
        outputDirectory.set(rootDir.resolve("docs/api"))
    }
}

tasks.register("updateDocs") {
    group = "documentation"
    description = "Generates the aggregated API docs into docs/api."
    dependsOn("dokkaGenerate")
}
