# DateParser Kotlin KMP release design

Approved scope: user requested dateparser-kotlin rename, full KMP migration, Maven Central publication, Russian and English documentation on 2026-10-14.

## Outcome
A reusable offline RU/EN natural-language date/time parser consumed from commonMain with a normal Maven dependency. Preserve original language rules and MIT attribution. Repository is Alox1d/dateparser-kotlin. Proposed coordinates: io.github.alox1d:dateparser-kotlin:0.1.0, subject to Central namespace verification.

## Architecture
- :dateparser owns a commonMain rule engine and immutable public result API. Initial targets Android (API 26+), JVM (JDK 8+ bytecode), iosArm64 and iosSimulatorArm64. iOS must pass macOS CI before release.
- Canonical EN/RU/regex/separator CSV files generate bundled common Kotlin tables; no runtime Context, Android resource loading, filesystem or network.
- Adapt the existing algorithm behind internal types, common Kotlin Regex and kotlinx-datetime. Each parse creates independent working state and captures reference time once. No global mutable Context or current-time state.
- DateParser.parse(text, reference, timeZone, options) returns immutable DateMatch values with target LocalDateTime/Instant, timezone, nullable end date, nullable DateTimePeriod recurrence, remaining text and raw confidence score. Defaults may use injected Clock and system zone; explicit inputs are deterministic. Recurrence months/years remain calendar components.
- Retain a deprecated Android-only compatibility wrapper for prepareParse/parseDate/valueOf and Calendar consumers; document absence sentinels and legacy period conversion. Demo uses the new API.
- Whitespace-only input returns no matches. Numeric/date validation rejects invalid calendar dates and overflowing numbers. Relative elapsed time across DST and calendar rollover have dedicated tests.

## Distribution and documentation
Maven Central publication includes full multiplatform metadata/target artifacts, sources, documentation artifact, MIT/developer/SCM POM and signatures. Local test repository consumption must succeed before Central upload. Never commit credentials or signing keys. Central account creation/terms and any new credentials require user participation where the tool policy requires it. No claim of Central availability until artifact URL resolves.
English README plus README.ru.md contain equivalent setup, usage, supported platforms, migration, limits and origin. Documentation website has separate English/Russian URLs and alternate links. CI covers shared JVM/Android tests, iOS simulator tests and iOS device klib compilation; publication follows green checks. Repository metadata describes only validated capabilities.

## Verification
Baseline: 16 Android/JVM regression tests passed before changes (2026-10-14). Shared tests pin RU/EN clock, explicit dates, reference-time offsets, rollover, leap dates, end absence, calendar recurrence, empty/whitespace/no-date text, invalid numeric/date input, timezone isolation and DST. Test a standalone KMP consumer against a local Maven repository. Validate complete publication artifacts/POM and documentation links. Actual Central release depends on verified account, namespace and signing.
