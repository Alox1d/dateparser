# Publishing to Maven Central

[Русский](PUBLISHING.ru.md)

1. Create a Central Portal account, preferably using GitHub, and accept its terms yourself. Verify namespace `io.github.alox1d` in the portal. GitHub login may verify it automatically; check the actual status.
2. Create a publishing user token in Central Portal. Keep its username/password out of Git and chat.
3. Generate a PGP signing key, retain the private key securely, and upload its public key to a supported keyserver.
4. Configure GitHub Actions secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`, optional `SIGNING_KEY_PASSWORD`. `SIGNING_KEY` is the ASCII-armored private key. Grant publishing access only when intentionally setting up the release workflow.
5. Run CI on the exact main commit, including iOS simulator tests and complete local artifact publication. Inspect the POM, metadata, platform artifacts, sources and documentation jars.
6. Dispatch the Publish workflow on main with version `0.1.0`. It reruns tests, signs all artifacts and calls `publishAndReleaseToMavenCentral`. The unsigned `localPublication` flag is for test repositories only.
7. Confirm the deployment is PUBLISHED in Central and the root POM resolves at `https://repo1.maven.org/maven2/io/github/alox1d/dateparser-kotlin/0.1.0/dateparser-kotlin-0.1.0.pom`. Only then mark README status released, create the matching GitHub tag/release and verify a clean consumer against Maven Central.

Central versions are immutable. Never upload replacement content under an already released version. Keep credentials/signing keys outside this repository. For a local signed release, provide the same Gradle properties via environment variables `ORG_GRADLE_PROJECT_mavenCentralUsername`, `ORG_GRADLE_PROJECT_mavenCentralPassword`, `ORG_GRADLE_PROJECT_signingInMemoryKey`, `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` and run the release task on macOS with Xcode.
