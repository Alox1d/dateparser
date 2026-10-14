# DateParser Kotlin

[English](README.md) | [Russian](README.ru.md) | [Documentation](https://alox1d.github.io/dateparser-kotlin/)

Offline Russian and English natural-language date/time parser for Kotlin Multiplatform. Extract dates, times, ranges and recurrence periods from reminder text on Android, JVM and iOS, without an account, API key or network service.

**Release status:** the signed 0.1.0 release has been submitted to Maven Central; public availability is being verified. The dependency below becomes available when publication completes; local installation is usable now.

## Installation

After the Central release, add `mavenCentral()` and the dependency to your shared module:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.alox1d:dateparser-kotlin:0.1.0")
        }
    }
}
```

The same coordinates work in a Kotlin/JVM or Android project. Gradle selects the platform artifact automatically.

## Usage

```kotlin
import io.github.alox1d.dateparser.DateParser
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

val parser = DateParser()
val reference = Instant.parse("2024-02-28T10:15:00Z")
val matches = parser.parse(
    text = "buy in 5 hours",
    reference = reference,
    timeZone = TimeZone.UTC
)
val instant = matches.single().instant // 2024-02-28T15:15:00Z
```

The Russian phrase `купить через 5 часов` produces the same instant. No Android `Context` or initialization is required. Passing reference time and timezone makes parsing reproducible. If omitted, the injected `Clock` (system clock by default) is read once and the system timezone is used. Results have second precision except elapsed offsets, which preserve reference subsecond precision.

## Verified expression examples

These examples use the reference time above and UTC:

- **Time:** English `meeting at 17:30` or Russian `встреча в 17:30` resolves to February 28, 2024 at 17:30.
- **Numeric date:** `29.02.2024` resolves to February 29, 2024.
- **Tomorrow:** English `tomorrow` or Russian `завтра` resolves to February 29, 2024.
- **Relative time:** English `in 5 hours` or Russian `через 5 часов` resolves to February 28, 2024 at 15:15.
- **Time range:** Russian `с 17:30 до 20:00` gives a start time of 17:30 and an end time of 20:00.
- **Monthly recurrence:** English `every month` or Russian `каждый месяц` gives a calendar interval of one month.
- **Two-day recurrence:** English `every 2 days` or Russian `каждые 2 дня` gives a calendar interval of two days.

## Result and options

`DateMatch` is immutable:

- `targetDate`: resolved local `LocalDateTime`.
- `instant`, `timeZone`: resolved `kotlin.time.Instant` and the timezone used.
- `endDate`, `endInstant`: optional upper boundary; `null` when absent.
- `recurrence`: optional `kotlinx.datetime.DateTimePeriod`; months/years remain calendar units.
- `remainingText`: the rule engine's remaining reminder text, not a character-offset span.
- `confidence`: accumulated rule score, not a probability or a value limited to 100.

`ParseOptions(errorLimit = 1.0, minimumPrevalence = 50.0)` controls fuzzy matching and rule thresholds. `errorLimit` accepts 0..1; `minimumPrevalence` accepts 0..100. Blank text returns an empty list. Working state is independent per call.

## Platforms and build

Targets: Android API 26+, JVM (Java 8+), iOS arm64 device and Apple Silicon simulator. Kotlin 2.3.20, kotlinx-datetime 0.8.0, AGP 8.13.2 and Gradle 8.13 are pinned. Building requires JDK 17-21 and Android SDK 35. iOS compilation/tests additionally require full Xcode on macOS; CI uses an arm64 macOS runner.

Run the relevant checks from the repository root:

- JVM behavior tests: `./gradlew :dateparser:jvmTest`.
- Android host tests: `./gradlew :dateparser:testAndroidHostTest`.
- Android demo build: `./gradlew :app:assembleDebug`.
- iOS simulator tests: `./gradlew :dateparser:iosSimulatorArm64Test`.
- iOS device compilation: `./gradlew :dateparser:compileKotlinIosArm64`.
- Generated rules: `python3 scripts/generate_rules.py --check`.

The same common behavior suite runs on JVM, Android host tests and iOS simulator. This is not Android device instrumentation coverage. Using the library in shared Kotlin iOS code is supported; a standalone Swift/XCFramework distribution is not part of this release.

## Install locally before Central publication

Run these commands from the repository root:

- Root metadata: `./gradlew :dateparser:publishKotlinMultiplatformPublicationToTestRepository -PlocalPublication=true`.
- JVM artifact: `./gradlew :dateparser:publishJvmPublicationToTestRepository -PlocalPublication=true`.
- Android artifact: `./gradlew :dateparser:publishAndroidPublicationToTestRepository -PlocalPublication=true`.
- Consumer verification: `./gradlew -p integration/consumer jvmTest`.

This writes an unsigned test Maven repository to `build/test-repository`. A separate KMP consumer resolves the real root metadata and platform artifacts. For a local iOS consumer, also run `:dateparser:publishAllPublicationsToTestRepository -PlocalPublication=true` on a Mac with Xcode. Do not use the local-publication flag for Central releases. See [publishing instructions](docs/PUBLISHING.md).

## Migration from the Android-only port

Use `io.github.alox1d.dateparser.DateParser` from shared code. The deprecated Android-only `com.huawei.wordnote.domain.services.dateparser.ParsedDate` facade retains `prepareParse`, `parseDate` and `valueOf`; `prepareParse` is now optional and retains no Context. It retains `Calendar`, the old UTC-epoch month/year duration convention and a **zero sentinel for an absent end date**. Prefer the common API for nullable boundaries and calendar recurrence. This is entry-point compatibility, not binary compatibility with old AARs or compatibility with every historical internal helper/type. Android minimum SDK rises from 16 to 26.

## Limits

This is a rule-based parser, not a guarantee for arbitrary text. Numeric dates use day-month-year rules; English does not imply US month/day ordering. Missing date/time fields inherit the reference, and a bare clock time is not automatically moved into the future. The inherited hour-24 rule normalizes `24:30` to next-day `00:30`. Ambiguous natural-language phrases can need additional rules. Relative hours/minutes/seconds are elapsed time; relative days/months/years use calendar arithmetic in the chosen timezone, including month-end clamping. Recurrence describes an interval, not a complete scheduling engine. DST gaps/overlaps for explicit local times follow kotlinx-datetime resolution rules. An overflowing numeric token causes that parse call to return no matches.

## License and origin

MIT. Port and adaptation of [Alexey Orazov's date-parser](https://github.com/alordash/date-parser), originally integrated into WordNote by Alexander Baranov. Original copyright and permission text are preserved in [LICENSE](LICENSE); attribution and the verified upstream commit are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Historical Android sources/tests are retained under `legacy/android` for comparison and are not built or published.
