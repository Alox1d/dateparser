# Публикация в Maven Central

[English](PUBLISHING.md)

1. Создай аккаунт Central Portal, предпочтительно через GitHub, и самостоятельно прими условия сервиса. Проверь namespace `io.github.alox1d`. При входе через GitHub он может подтверждаться автоматически; сверяй фактический статус.
2. Создай пользовательский токен публикации в Central Portal. Его логин и пароль не должны попадать в Git или чат.
3. Создай PGP-ключ подписи, безопасно сохрани приватный ключ и опубликуй открытый ключ на поддерживаемом сервере ключей.
4. Настрой секреты GitHub Actions: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`, при необходимости `SIGNING_KEY_PASSWORD`. `SIGNING_KEY` - приватный ключ в ASCII-armored формате. Доступ для публикации выдаётся только при намеренной настройке процесса выпуска.
5. Дождись CI для конкретного коммита main, включая тесты симулятора iOS и полную локальную публикацию. Проверь POM, metadata, платформенные артефакты, исходники и документационные JAR.
6. Запусти workflow Publish на main с версией `0.1.1`. Он повторит тесты, подпишет артефакты и выполнит `publishAndReleaseToMavenCentral`. Неподписанный режим `localPublication` предназначен только для тестового репозитория.
7. Убедись, что deployment получил статус PUBLISHED и корневой POM доступен: `https://repo1.maven.org/maven2/io/github/alox1d/dateparser-kotlin/0.1.1/dateparser-kotlin-0.1.1.pom`. После этого обнови статус README, создай совпадающие GitHub-тег/релиз и проверь чистого потребителя из Maven Central.

Выпущенные версии Central неизменяемы. Не загружай другое содержимое под уже опубликованной версией. Учётные данные и ключи подписи хранятся вне репозитория. Для подписанного выпуска локально передай Gradle-свойства через переменные окружения `ORG_GRADLE_PROJECT_mavenCentralUsername`, `ORG_GRADLE_PROJECT_mavenCentralPassword`, `ORG_GRADLE_PROJECT_signingInMemoryKey`, `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` и выполни задачу выпуска на macOS с Xcode.
