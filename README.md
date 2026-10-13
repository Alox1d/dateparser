# DateParser Kotlin

[English](README.md) | [Русский](README.ru.md) | [Documentation](https://alox1d.github.io/dateparser-kotlin/)

Offline Russian and English natural-language date/time parser for Kotlin Multiplatform. Extract dates, times, ranges and recurrence periods from reminder text on Android, JVM and iOS, without an account, API key or network service.

**Release status:** version 0.1.0 is prepared for Maven Central. Publication is pending publisher account/namespace and signing setup. The dependency below becomes available after that release; local installation is usable now.

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
    text = "купить через 5 часов",
    reference = reference,
    timeZone = TimeZone.UTC
)
val instant = matches.single().instant // 2024-02-28T15:15:00Z
```

`buy in 5 hours` produces the same instant. No Android `Context` or initialization is required. Passing reference time and timezone makes parsing reproducible. If omitted, the injected `Clock` (system clock by default) is read once and the system timezone is used. Results have second precision except elapsed offsets, which preserve reference subsecond precision.

## Verified expression examples

These examples use the reference time above and UTC:

| Input | Result |
| --- | --- |
| `встреча в 17:30` / `meeting at 17:30` | 2024-02-28 17:30 |
| `29.02.2024` | 2024-02-29 |
| `завтра` / `tomorrow` | 2024-02-29 |
| `через 5 часов` / `in 5 hours` | 2024-02-28 15:15 |
| `с 17:30 до 20:00` | Start 17:30, end 20:00 |
| `каждый месяц` / `every month` | Calendar recurrence: 1 month |
| `каждые 2 дня` / `every 2 days` | Calendar recurrence: 2 days |

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

```sh
./gradlew :dateparser:jvmTest :dateparser:testAndroidHostTest :app:assembleDebug
./gradlew :dateparser:iosSimulatorArm64Test :dateparser:compileKotlinIosArm64
python3 scripts/generate_rules.py --check
```

The same common behavior suite runs on JVM, Android host tests and iOS simulator. This is not Android device instrumentation coverage. Using the library in shared Kotlin iOS code is supported; a standalone Swift/XCFramework distribution is not part of this release.

## Install locally before Central publication

```sh
./gradlew :dateparser:publishKotlinMultiplatformPublicationToTestRepository :dateparser:publishJvmPublicationToTestRepository :dateparser:publishAndroidPublicationToTestRepository -PlocalPublication=true
./gradlew -p integration/consumer jvmTest
```

This writes an unsigned test Maven repository to `build/test-repository`. A separate KMP consumer resolves the real root metadata and platform artifacts. For a local iOS consumer, also run `:dateparser:publishAllPublicationsToTestRepository -PlocalPublication=true` on a Mac with Xcode. Do not use the local-publication flag for Central releases. See [publishing instructions](docs/PUBLISHING.md).

## Migration from the Android-only port

Use `io.github.alox1d.dateparser.DateParser` from shared code. The deprecated Android-only `com.huawei.wordnote.domain.services.dateparser.ParsedDate` facade retains `prepareParse`, `parseDate` and `valueOf`; `prepareParse` is now optional and retains no Context. It retains `Calendar`, the old UTC-epoch month/year duration convention and a **zero sentinel for an absent end date**. Prefer the common API for nullable boundaries and calendar recurrence. This is entry-point compatibility, not binary compatibility with old AARs or compatibility with every historical internal helper/type. Android minimum SDK rises from 16 to 26.

## Limits

This is a rule-based parser, not a guarantee for arbitrary text. Numeric dates use day-month-year rules; English does not imply US month/day ordering. Missing date/time fields inherit the reference, and a bare clock time is not automatically moved into the future. Ambiguous natural-language phrases can need additional rules. Relative hours/minutes/seconds are elapsed time; relative days/months/years use calendar arithmetic in the chosen timezone, including month-end clamping. Recurrence describes an interval, not a complete scheduling engine. DST gaps/overlaps for explicit local times follow kotlinx-datetime resolution rules. An overflowing numeric token causes that parse call to return no matches.

## License and origin

MIT. Port and adaptation of [Alexey Orazov's date-parser](https://github.com/alordash/date-parser), originally integrated into WordNote by Alexander Baranov. Original copyright and permission text are preserved in [LICENSE](LICENSE); attribution and the verified upstream commit are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Historical Android sources/tests are retained under `legacy/android` for comparison and are not built or published.
