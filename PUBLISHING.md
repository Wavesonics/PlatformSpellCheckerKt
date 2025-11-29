# Publishing PlatformSpellChecker to Maven Central

This document explains how to publish the PlatformSpellChecker library to Maven Central.

## Configuration Summary

**Group ID:** `com.darkrockstudios`
**Artifact ID:** `platform-spellcheckerkt`
**License:** MIT
**Version:** Configured in `gradle.properties` as `library.version`

## Prerequisites

### 1. Maven Central Account

Create an account at [https://central.sonatype.com](https://central.sonatype.com) (recommended) or [https://s01.oss.sonatype.org](https://s01.oss.sonatype.org) (legacy).

### 2. Domain Verification

Since you're using the `com.darkrockstudios` group ID, you'll need to verify ownership of the `darkrockstudios.com` domain with Maven Central. Alternatively, you can use `io.github.wavesonics` which requires GitHub verification.

### 3. GPG Signing Key

Generate a GPG key pair for signing artifacts:

```bash
# Generate key
gpg --gen-key

# Export the key (for GitHub Secrets)
gpg --export-secret-keys YOUR_KEY_ID | base64

# Get your key ID (last 8 characters)
gpg --list-secret-keys --keyid-format=long
```

## Local Testing

You can test publishing locally without signing:

```bash
./gradlew publishToMavenLocal
```

This will publish unsigned artifacts to your local Maven repository at `~/.m2/repository/`.

## Publishing via GitHub Actions

### GitHub Secrets Configuration

Add the following secrets to your GitHub repository (Settings → Secrets and variables → Actions):

1. **OSSRH_USERNAME**: Your Sonatype username
2. **OSSRH_PASSWORD**: Your Sonatype password
3. **SIGNING_KEY**: Your GPG private key (base64 encoded)
4. **SIGNING_PASSWORD**: Your GPG key passphrase

### Example GitHub Actions Workflow

Create `.github/workflows/publish.yml`:

```yaml
name: Publish to Maven Central

on:
  push:
    tags:
      - 'v*'
  workflow_dispatch:

jobs:
  publish:
    runs-on: macos-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Publish to Maven Central
        env:
          OSSRH_USERNAME: \${{ secrets.OSSRH_USERNAME }}
          OSSRH_PASSWORD: \${{ secrets.OSSRH_PASSWORD }}
          SIGNING_KEY: \${{ secrets.SIGNING_KEY }}
          SIGNING_PASSWORD: \${{ secrets.SIGNING_PASSWORD }}
        run: ./gradlew publish --no-daemon --stacktrace

      - name: Close and release repository
        if: success()
        run: |
          echo "Build successful! Visit Sonatype to release the staging repository."
```

### Version Management

Update the version in `gradle.properties` before publishing:

```properties
library.version=1.0.0
```

For releases, use semantic versioning (e.g., `1.0.0`).
For snapshots, append `-SNAPSHOT` (e.g., `1.0.0-SNAPSHOT`).

Snapshots are automatically published to the snapshots repository.
Release versions are staged and require manual release in Sonatype.

## Manual Publishing (Local)

If you prefer to publish from your local machine:

1. Add credentials to `~/.gradle/gradle.properties`:

```properties
ossrhUsername=your_username
ossrhPassword=your_password
signing.keyId=your_key_id
signing.password=your_gpg_passphrase
signing.secretKeyRingFile=/path/to/secring.gpg
```

2. Run the publish command:

```bash
./gradlew publish
```

## Troubleshooting

### "Cannot perform signing task because it has no configured signatory"

This means signing credentials are not configured. The library is configured to skip signing for local testing (`publishToMavenLocal`), but Maven Central requires signed artifacts.

**Solution:** Configure GPG signing as described above.

### "Multiple publications with coordinates... will overwrite each other"

This warning is expected for Kotlin Multiplatform libraries. The publications are differentiated by their classifiers and platform-specific artifacts.

### Dokka V1 Deprecation Warning

The current configuration uses Dokka V1. To upgrade to V2, add this to `gradle.properties`:

```properties
org.jetbrains.dokka.experimental.gradle.pluginMode=V2EnabledWithHelpers
```

## Publishing Checklist

Before publishing a release:

- [ ] Update version in `gradle.properties` (remove `-SNAPSHOT`)
- [ ] Update CHANGELOG or README with release notes
- [ ] Test locally: `./gradlew publishToMavenLocal`
- [ ] Create and push git tag: `git tag v1.0.0 && git push origin v1.0.0`
- [ ] Wait for GitHub Actions to complete
- [ ] Visit Sonatype and release the staging repository
- [ ] Wait 10-30 minutes for sync to Maven Central
- [ ] Verify on [search.maven.org](https://search.maven.org/)

## Using the Published Library

Once published, users can add the dependency:

### Kotlin Multiplatform (build.gradle.kts)

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.darkrockstudios:platform-spellcheckerkt:1.0.0")
        }
    }
}
```

### Android (build.gradle.kts)

```kotlin
dependencies {
    implementation("com.darkrockstudios:platform-spellcheckerkt:1.0.0")
}
```

## Resources

- [Maven Central Publishing Guide](https://central.sonatype.org/publish/publish-guide/)
- [Gradle Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html)
- [Kotlin Multiplatform Publishing](https://kotlinlang.org/docs/multiplatform-publish-lib.html)
