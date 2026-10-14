# Changelog

## 0.1.1

- Add English named dates in US and European order, month abbreviations, ordinals and four-digit years; ISO and slash dates with explicit `DateOrder` for ambiguous inputs.
- Recognize attached/spaced AM/PM, `today`, `yesterday`, bare weekdays and `next week/month/year`. Explicit AM/PM clocks now zero omitted minutes and seconds.
- Keep compound duration and recurrence expressions together while preserving independent expressions and reminder words.
- Fix range end calendar inheritance, overnight clocks and DST boundary consistency. Reject inverted explicit ranges and unsupported relative range ends.
- Fix reminder text around separators and punctuation on numeric tokens; scope relative offsets to their owning dates.
- Reject non-positive recurrence and add shared regression tests plus separate JVM, Android and iOS published-artifact consumer checks.
- Preserve existing `DateParser.parse`, `ParseOptions` and `DateMatch` signatures. Android API26+, JVM Java8+ and KMP iOS targets remain supported.

## 0.1.0

- Initial Maven Central release with offline English/Russian parsing, Kotlin Multiplatform targets, immutable results, calendar recurrence and bilingual documentation.
