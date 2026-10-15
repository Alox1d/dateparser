# Published KMP reminder demo

This standalone Kotlin Multiplatform consumer uses **DateParser Kotlin 0.1.2 from Maven Central**. Its reminder preview lives in `src/commonMain`; the same code compiles for JVM, Android API 26+, and iOS arm64/device and simulator. A small JVM entry point prints the results without requiring an emulator.

From the repository root:

```sh
./gradlew -p integration/consumer runDemo -PcentralOnly=true --console=plain
```

Build prerequisites: JDK 17-21 and Android SDK 35. Full Xcode on macOS is needed only for the iOS checks. The repository's Gradle wrapper is used; no separate Gradle installation is necessary.

The fixed reference is `2026-07-03T10:15:00Z`. The demo covers named US/European dates, AM/PM, reminder text, range boundaries, timezone conversion, and explicit MDY/DMY. An unresolved ambiguous date or a recurring interval goes to manual review. This example does not save or schedule a reminder.

Try your own input:

```sh
./gradlew -p integration/consumer runDemo -PcentralOnly=true \
  '-PdemoText=Meet on 4th July 2026 at 5 pm' \
  -PdemoTimeZone=Europe/Berlin --console=plain

./gradlew -p integration/consumer runDemo -PcentralOnly=true \
  -PdemoText=03/04/2026 -PdemoDateOrder=DMY --console=plain
```

Optional properties: `demoReference` (ISO instant), `demoTimeZone` (IANA zone), and `demoDateOrder` (`NONE`, `MDY`, `DMY`, or `YMD`; default `NONE`). Dates without a clock inherit the reference clock. The example does not automatically move past dates into the future.

Verify the shared preview against the public artifacts:

```sh
./gradlew -p integration/consumer jvmTest testAndroidHostTest -PcentralOnly=true
# On macOS with full Xcode:
./gradlew -p integration/consumer iosSimulatorArm64Test compileKotlinIosArm64 -PcentralOnly=true
```

Without `centralOnly=true`, this consumer first tries the root project's unsigned `build/test-repository` for library development. Keep the flag when following the public demo.

Read the [KMP walkthrough](https://alox1d.github.io/dateparser-kotlin/en/kmp-reminder-demo/) for installation, shared code, user confirmation, and US/EU date handling.
