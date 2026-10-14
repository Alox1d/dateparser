---
title: DateParser Kotlin - парсер дат и времени из текста
---

# DateParser Kotlin

[English](../en/index.md) | [Русский](../ru/index.md) | [Документация](https://alox1d.github.io/dateparser-kotlin/)

Парсер дат и времени из текста на русском и английском для Kotlin Multiplatform. Извлекает даты, время, диапазоны и интервалы повторения из напоминаний. Работает локально на Android, JVM и iOS, без аккаунта, API-ключа и сетевого сервиса.

**Статус выпуска:** [версия 0.1.0 опубликована в Maven Central](https://central.sonatype.com/artifact/io.github.alox1d/dateparser-kotlin/0.1.0).

## Подключение

Добавь `mavenCentral()` и зависимость в общий модуль:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.alox1d:dateparser-kotlin:0.1.0")
        }
    }
}
```

Те же координаты подходят для Kotlin/JVM и Android. Gradle автоматически выбирает артефакт нужной платформы.

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

## Проверенные примеры

Для указанного выше времени отсчёта и UTC:

| Вход | Результат |
| --- | --- |
| `встреча в 17:30` / `meeting at 17:30` | 2024-02-28 17:30 |
| `29.02.2024` | 2024-02-29 |
| `завтра` / `tomorrow` | 2024-02-29 |
| `через 5 часов` / `in 5 hours` | 2024-02-28 15:15 |
| `с 17:30 до 20:00` | Начало 17:30, окончание 20:00 |
| `каждый месяц` / `every month` | Календарный повтор: 1 месяц |
| `каждые 2 дня` / `every 2 days` | Календарный повтор: 2 дня |

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
./gradlew -p integration/consumer jvmTest
```

Команда создаёт неподписанный тестовый Maven-репозиторий в `build/test-repository`. Отдельный KMP-потребитель разрешает настоящие metadata и платформенные артефакты. Для локального iOS-потребителя дополнительно выполни `:dateparser:publishAllPublicationsToTestRepository -PlocalPublication=true` на Mac с Xcode. Флаг локальной публикации нельзя использовать для выпуска в Central. См. [инструкцию публикации](https://github.com/Alox1d/dateparser-kotlin/blob/main/docs/PUBLISHING.ru.md).

## Переход со старой Android-версии

В общем коде используй `io.github.alox1d.dateparser.DateParser`. Устаревший Android-фасад `com.huawei.wordnote.domain.services.dateparser.ParsedDate` сохраняет `prepareParse`, `parseDate` и `valueOf`; вызов `prepareParse` теперь необязателен и не сохраняет Context. Фасад сохраняет `Calendar`, старое преобразование месяцев/лет в длительность от UTC epoch и **ноль как признак отсутствия даты окончания**. Для необязательных границ и календарных повторов используй общий API. Это совместимость точек входа по исходному коду, а не бинарная совместимость со старым AAR или поддержка всех прежних внутренних типов. Минимальный Android SDK повышен с 16 до 26.

## Ограничения

Парсер основан на правилах и не гарантирует разбор произвольного текста. Числовые даты используют порядок день-месяц-год; английский язык не включает американский порядок месяц/день. Пропущенные поля берутся из времени отсчёта; время без даты автоматически не переносится в будущее. Сохранённое правило часа 24 преобразует `24:30` в `00:30` следующего дня. Неоднозначные фразы могут потребовать новых правил. Относительные часы, минуты и секунды означают прошедшее время; дни, месяцы и годы используют календарную арифметику выбранного часового пояса с ограничением последним днём месяца. Повторение описывает интервал, но не заменяет планировщик. Неоднозначное или отсутствующее локальное время при DST разрешается по правилам kotlinx-datetime. Число с переполнением приводит к пустому результату всего вызова.

## Лицензия и происхождение

MIT. Порт и адаптация [date-parser Алексея Оразова](https://github.com/alordash/date-parser), первоначально встроенные Александром Барановым в WordNote. Исходный copyright и текст разрешения сохранены в [LICENSE](https://github.com/Alox1d/dateparser-kotlin/blob/main/LICENSE); атрибуция и проверенный upstream-коммит - в [THIRD_PARTY_NOTICES.md](https://github.com/Alox1d/dateparser-kotlin/blob/main/THIRD_PARTY_NOTICES.md). Исторические Android-исходники и тесты сохранены в `legacy/android` для сравнения и не собираются/не публикуются.
