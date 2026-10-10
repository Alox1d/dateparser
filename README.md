<p align="center"><img src="assets/poster.png" alt="DateParser — words become dates" width="100%"></p>

# DateParser

Kotlin / Android port of [Alexey Orazov's date-parser](https://github.com/alordash/date-parser). Extract dates and times from Russian and English reminder text using packaged rules, without a network service.

Originally adapted for WordNote by Alexander Baranov. This public version fixes crashes in rule loading, recurrence conversion, absolute end dates, and grouping of nearby time expressions.

## Try it

```kotlin
import com.huawei.wordnote.domain.services.dateparser.ParsedDate

ParsedDate.prepareParse(applicationContext)
val reminder = ParsedDate.parseDate("купить через 5 часов").firstOrNull()?.valueOf()
val calendar = reminder?.target_date
```

The historic package names are retained for compatibility. Initialize with an application context before parsing. `target_date` is a local `Calendar`, `max_date` is an epoch timestamp in milliseconds, and `period_time` is a duration in milliseconds. An absent recurrence is zero. Calendar month/year recurrences retain the upstream UTC-epoch convention, so they are **not** a scheduling engine that handles variable month lengths relative to each occurrence.

## Build and test

Requires JDK 17–21 and Android SDK 35. Point `local.properties` to your SDK (`sdk.dir=...`) or set `ANDROID_HOME`.

```sh
./gradlew :dateparser:testDebugUnitTest :app:assembleDebug :dateparser:assembleRelease
```

The library AAR is written to `dateparser/build/outputs/aar/`. The `app` module is a small parsing demo. Gradle 8.11.1, AGP 8.7.3 and Kotlin 2.1.0 are pinned. JUnit and Robolectric tests cover packaged Russian/English rules, clock and explicit dates, relative hours, empty input, recurrence durations in three time zones, leap-day timestamps, and non-mutating expression distances.

## Scope

This is a legacy rule-based parser with a maintained build and regression tests, not a guarantee that arbitrary natural-language text will parse correctly. Other ambiguous phrases and historical rules can need work. No account, API key or Huawei SDK is required by the library. The current test suite runs on the JVM; device instrumentation coverage is not included.

## License and origin

MIT. The original algorithm and language rules are derived from Alexey Orazov's MIT-licensed JavaScript project. Original copyright and permission text are retained in [LICENSE](LICENSE); port attribution and upstream reference are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
