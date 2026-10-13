# DateParser Kotlin KMP Release Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans in this session. Steps use checkbox syntax for tracking.

**Goal:** Rename and release a reusable RU/EN KMP parser with bilingual documentation.
**Architecture:** Preserve rules behind a common Kotlin engine, expose typed deterministic results, retain an Android compatibility adapter, publish all platform artifacts.
**Tech Stack:** Kotlin Multiplatform, kotlinx-datetime, Android KMP plugin, Gradle, Maven Central Portal, GitHub Actions.
**Spec:** docs/superpowers/specs/2026-10-14-kmp-release-design.md

## Global Constraints
- User authorized implementation, repository rename and publication on 2026-10-14. Execute inline in feat/kmp-library; no repeat scope approval.
- Preserve MIT notices, RU/EN CSV sources, no credentials in Git.
- Android API 26+, JVM 8 bytecode, iosArm64 and iosSimulatorArm64.
- Reference time/timezone fixed per call; nullable end and calendar recurrence in public API.

## Review Focus
- DST offsets must distinguish elapsed hours from calendar days/months.
- Month/year rollover and leap-day validation must not silently produce impossible dates.
- Different parser instances/timezones must not share mutable working state.
- Absent end/recurrence must remain distinguishable from actual values.
- Published KMP metadata must resolve in a separate commonMain consumer.

### Task 1: Shared parser and behavioral tests
**Files:** dateparser/build.gradle.kts; dateparser/src/commonMain/kotlin/io/github/alox1d/dateparser/**; dateparser/src/commonTest/**; dateparser/rules/**; scripts/generate_rules.py; root Gradle files.
**Interfaces:** Produce DateParser.parse(text, reference, timeZone, options): List<DateMatch>, ParseOptions, DateMatch with targetDate, instant, timeZone, recurrence, endDate, remainingText and confidence.
- [ ] Write shared behavior tests before porting, observe missing API failure.
- [ ] Configure KMP and bundled rules; port existing engine with per-call reference time and common Regex.
- [ ] Implement typed API and fix specified semantics using failing behavioral tests.
- [ ] Run JVM shared suite, Android host tests, device klib compile where available.
- [ ] Commit shared parser.

### Task 2: Android compatibility and consumer verification
**Files:** dateparser/src/androidMain/**; app/**; integration/consumer/**.
**Interfaces:** Consume DateParser; expose deprecated original ParsedDate facade only on Android. Standalone consumer imports io.github.alox1d.dateparser from published metadata.
- [ ] Write tests for old Calendar facade before implementation.
- [ ] Implement adapter, update demo and test Android compilation.
- [ ] Publish unsigned test artifacts to local repository and run standalone commonMain JVM consumer tests.
- [ ] Commit adapter and consumer.

### Task 3: Publication, CI and documentation
**Files:** .github/workflows/**; README.md; README.ru.md; docs/**; scripts/**.
**Interfaces:** Publish io.github.alox1d:dateparser-kotlin:0.1.0 with all KMP variants; build bilingual docs.
- [ ] Configure Central publication, POM, signatures and artifact documentation.
- [ ] Add CI and macOS iOS verification; add explicit release workflow.
- [ ] Write equivalent RU/EN installation, examples, platform matrix, migration and limits; build docs with localized URLs.
- [ ] Check generated rules, POMs, artifacts, docs links and all relevant test suites.
- [ ] Commit and push feature branch; run CI.

### Task 4: Review and release
**Files:** complete branch; GitHub metadata; Central deployment.
- [ ] Independent whole-branch review under executing-plans, fix important findings with regression tests.
- [ ] Verify green CI and user-authorized integration into main.
- [ ] Finish Central account/namespace/signing setup with user as required, upload validated artifacts and release.
- [ ] Verify public dependency URL, release, bilingual docs and repository metadata.
