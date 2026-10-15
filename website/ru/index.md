---
title: DateParser Kotlin - парсер дат и времени из текста
---

# DateParser Kotlin

[![Maven Central](https://img.shields.io/maven-central/v/io.github.alox1d/dateparser-kotlin?strategy=releaseProperty&style=for-the-badge&label=Maven%20Central&color=7928CA)](https://central.sonatype.com/artifact/io.github.alox1d/dateparser-kotlin)
[![CI main](https://img.shields.io/github/actions/workflow/status/Alox1d/dateparser-kotlin/ci.yml?branch=main&event=push&style=for-the-badge&label=CI%20main&color=7928CA)](https://github.com/Alox1d/dateparser-kotlin/actions/workflows/ci.yml?query=branch%3Amain%20event%3Apush)
[![MIT license](https://img.shields.io/github/license/Alox1d/dateparser-kotlin?style=for-the-badge&label=License&color=7928CA)](https://github.com/Alox1d/dateparser-kotlin/blob/main/LICENSE)
[![Featured on DevShelf](https://img.shields.io/badge/Featured%20on-DevShelf-7928CA?style=for-the-badge&logo=googlechrome&logoColor=white)](https://devshelf.ritualdev.in/tools/dateparser-kotlin.html)

[English](../en/index.md) | [Русский](../ru/index.md) | [Документация](https://alox1d.github.io/dateparser-kotlin/)

Парсер дат и времени из текста на русском и английском для Kotlin Multiplatform. Извлекает даты, время, диапазоны и интервалы повторения из напоминаний. Работает локально на Android, JVM и iOS, без аккаунта, API-ключа и сетевого сервиса.

**Версия:** 0.1.2 | [Maven Central](https://central.sonatype.com/artifact/io.github.alox1d/dateparser-kotlin/0.1.2) | [Изменения](https://github.com/Alox1d/dateparser-kotlin/blob/main/CHANGELOG.md).

## Подключение

Добавь `mavenCentral()` и зависимость в общий модуль:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.alox1d:dateparser-kotlin:0.1.2")
        }
    }
}
```

Те же координаты подходят для Kotlin/JVM и Android. Gradle автоматически выбирает артефакт нужной платформы.

## Запускаемый пример KMP

[Демо общего кода](https://github.com/Alox1d/dateparser-kotlin/blob/main/integration/consumer/README.md) и [практическая статья на английском](https://alox1d.github.io/dateparser-kotlin/en/kmp-reminder-demo/) используют опубликованную 0.1.2 из Maven Central. Код предпросмотра напоминания находится в `commonMain`; он проверяется на JVM, Android и iOS.

```sh
./gradlew -p integration/consumer runDemo -PcentralOnly=true --console=plain
```

Показаны MDY/DMY, часовые пояса и границы диапазона. Для этого проекта нужны JDK 17-21 и Android SDK 35; запуск и нативные проверки описаны в README демо.

## Использование

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

`buy in 5 hours` даёт тот же результат. Android `Context` и предварительная инициализация не нужны. Явные время отсчёта и часовой пояс обеспечивают воспроизводимость. Если их не указать, внедрённый `Clock` (по умолчанию системный) читается один раз, используется системный часовой пояс. Точность результата - секунды; смещения в часах, минутах и секундах сохраняют долю секунды исходного времени.

## Новое в 0.1.2: первый день недели следующего месяца

`next Friday` поддерживается начиная с 0.1.1: это ближайшая пятница, причём текущая пятница пропускается.

Версия 0.1.2 дополнительно распознаёт `the first Monday of next month` для любого полного английского названия дня недели. `the` необязательно, допускаются `first` и `1st`, регистр не важен.

Расчёт начинается с первого числа следующего календарного месяца в заданном часовом поясе. При reference `2024-02-28T10:15:00Z` и UTC фраза `Pay rent on the first Monday of next month` даёт `2024-03-04T10:15:00Z` и оставляет название `Pay rent`. Явное `at 5PM` задаёт `17:00:00`; без времени сохраняются часы reference.

Это разовая дата, не правило повторения. Распознанные неподдерживаемые формы (`second` - `fifth`, `last` и числовые порядковые формы, кроме `1st`) и `every/each first ... of next month` отклоняются, чтобы не выдавать частично распознанные день недели или месяц. Другие английские порядковые формулировки в это дополнение не входят. Грамматика расширяется отдельными документированными правилами и регрессионными тестами; произвольный контекст не угадывается.

## Английские даты для США и Европы

Поддерживаются оба порядка названия месяца и дня, сокращения месяцев и порядковые числительные. Примеры используют указанное выше время отсчёта и UTC:

| Вход | Результат |
| --- | --- |
| `December 31, 2026 at 5PM` / `31st Dec 2026 at 5 pm` | 2026-12-31 17:00:00 |
| `2026-12-31` | 2026-12-31 10:15:00 |
| `13/04/2026` / `04/13/2026` | 2026-04-13 10:15:00 |
| `today` / `yesterday` | 2024-02-28 / 2024-02-27, часы из времени отсчёта |
| `Friday` / `on Friday` | 2024-03-01, часы из времени отсчёта |
| `next week` / `next month` | 2024-03-06 / 2024-03-28 |
| `in 1 month and 2 days` | 2024-03-30 10:15:00 |
| `every 1 month and 2 days` | Календарный интервал: 1 месяц и 2 дня |
| `tomorrow from 5PM to 8PM` | 2024-02-29 17:00:00 - 20:00:00 |

Для неоднозначной числовой даты явно выбери порядок полей:

```kotlin
import io.github.alox1d.dateparser.DateOrder

val us = parser.parse("03/04/2026", DateOrder.MDY, reference, TimeZone.UTC)
// 4 марта 2026
val eu = parser.parse("03/04/2026", DateOrder.DMY, reference, TimeZone.UTC)
// 3 апреля 2026
```

Без `DateOrder` неоднозначные даты со слешами дают пустой результат. Однозначные даты со слешами и ISO распознаются автоматически. `DateOrder.YMD` задаёт год/месяц/день. Формат с точками сохраняет порядок день/месяц/год независимо от выбранного порядка слешей.

Русские примеры: `завтра`, `через 5 часов`, `29.02.2024`, `завтра с 17:30 до 20:00`, `каждый месяц`, `каждые 2 дня`. `in 5 hours` и `через 5 часов` дают один момент времени.

## Результат и параметры

`DateMatch` неизменяемый:

- `targetDate`: вычисленные локальные дата и время, `LocalDateTime`.
- `instant`, `timeZone`: момент `kotlin.time.Instant` и использованный часовой пояс.
- `endDate`, `endInstant`: необязательная верхняя граница; `null`, если не задана.
- `recurrence`: необязательный `kotlinx.datetime.DateTimePeriod`; месяцы и годы остаются календарными единицами.
- `remainingText`: оставшийся текст напоминания по правилам алгоритма; это не диапазон позиций в исходной строке.
- `confidence`: сумма весов правил, не вероятность и не величина, ограниченная 100.

`ParseOptions(errorLimit = 1.0, minimumPrevalence = 50.0)` управляет нечётким сопоставлением и порогом правил. Допустимы `errorLimit` от 0 до 1 и `minimumPrevalence` от 0 до 100. Пустая строка или пробелы дают пустой список. Рабочее состояние каждого вызова независимо.

## Платформы и сборка

Целевые платформы: Android API 26+, JVM (Java 8+), iOS arm64 и симулятор на Apple Silicon. Зафиксированы Kotlin 2.3.20, kotlinx-datetime 0.8.0, AGP 8.13.2 и Gradle 8.13. Для сборки нужны JDK 17-21 и Android SDK 35. Для компиляции и тестов iOS дополнительно нужен полный Xcode на macOS; CI использует macOS arm64.

```sh
./gradlew :dateparser:jvmTest :dateparser:testAndroidHostTest :app:assembleDebug
./gradlew :dateparser:iosSimulatorArm64Test :dateparser:compileKotlinIosArm64
python3 scripts/generate_rules.py --check
```

Один общий набор поведенческих тестов выполняется на JVM, Android host и симуляторе iOS. Тестов на устройстве Android в этом выпуске нет. Подключение в общий Kotlin-код приложения iOS предусмотрено; отдельный выпуск Swift/XCFramework в эту версию не входит.

## Локальная разработка и проверка публикации

```sh
./gradlew :dateparser:publishKotlinMultiplatformPublicationToTestRepository :dateparser:publishJvmPublicationToTestRepository :dateparser:publishAndroidPublicationToTestRepository -PlocalPublication=true
./gradlew -p integration/consumer jvmTest testAndroidHostTest
```

Команда создаёт неподписанный тестовый Maven-репозиторий в `build/test-repository`. Отдельный KMP-потребитель разрешает настоящие metadata и платформенные артефакты. Для локального iOS-потребителя дополнительно выполни `:dateparser:publishAllPublicationsToTestRepository -PlocalPublication=true` на Mac с Xcode. Флаг локальной публикации нельзя использовать для выпуска в Central. См. [инструкцию публикации](https://github.com/Alox1d/dateparser-kotlin/blob/main/docs/PUBLISHING.ru.md).

## Переход со старой Android-версии

В общем коде используй `io.github.alox1d.dateparser.DateParser`. Устаревший Android-фасад `com.huawei.wordnote.domain.services.dateparser.ParsedDate` сохраняет `prepareParse`, `parseDate` и `valueOf`; вызов `prepareParse` теперь необязателен и не сохраняет Context. Фасад сохраняет `Calendar`, старое преобразование месяцев/лет в длительность от UTC epoch и **ноль как признак отсутствия даты окончания**. Для необязательных границ и календарных повторов используй общий API. Это совместимость точек входа по исходному коду, а не бинарная совместимость со старым AAR или поддержка всех прежних внутренних типов. Минимальный Android SDK повышен с 16 до 26.

## Ограничения

Парсер основан на правилах; произвольному тексту могут потребоваться дополнительные правила. Пропущенные поля берутся из времени отсчёта, кроме явного английского времени AM/PM: `5PM` означает `17:00:00`, а `5:30PM` - `17:30:00`. Время без даты автоматически не переносится в будущее. Правило часа 24 преобразует `24:30` в `00:30` следующего дня. День недели означает ближайшее наступление, включая сегодня; `next Friday` пропускает текущую пятницу.

Относительные часы, минуты и секунды означают прошедшее время; дни, месяцы и годы используют календарную арифметику часового пояса с ограничением последним днём месяца. `next week/month/year` означает сдвиг на одну календарную единицу. Интервал повторения должен быть положительным; он не заменяет планировщик.

Окончание диапазона без даты наследует календарную дату начала. Более ранние часы окончания переносятся на следующий день; явно заданная дата окончания раньше начала отклоняется. Относительные смещения в качестве окончания диапазона пока отклоняются. DST разрешается по правилам kotlinx-datetime; обе локальные границы соответствуют итоговым моментам времени. Распознанная некорректная дата или AM/PM-время, неоднозначная дата со слешами без выбора порядка и переполнение числа дают пустой результат всего вызова. `remainingText` сохраняет слова и регистр напоминания, но не воспроизводит исходник побайтово и не содержит позиций символов.

## Лицензия и происхождение

MIT. Порт и адаптация [date-parser Алексея Оразова](https://github.com/alordash/date-parser), первоначально встроенные Александром Барановым в WordNote. Исходный copyright и текст разрешения сохранены в [LICENSE](https://github.com/Alox1d/dateparser-kotlin/blob/main/LICENSE); атрибуция и проверенный upstream-коммит - в [THIRD_PARTY_NOTICES.md](https://github.com/Alox1d/dateparser-kotlin/blob/main/THIRD_PARTY_NOTICES.md). Исторические Android-исходники и тесты сохранены в `legacy/android` для сравнения и не собираются/не публикуются.
