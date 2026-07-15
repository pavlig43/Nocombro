# Отчёт по правкам после кодревью

Дата проверки: 14 июля 2026 года.

## Результат

План выполнен. Сборка и тесты проходят. Миграция выбранной YDB применена и проверена.

## Изменённые подсистемы

- Репозиторий и CI: оставлен ручной запуск `workflow_dispatch` с четырьмя независимыми задачами.
- Detekt: включён `ignoreFailures`, удалены baseline-файлы, отключено их создание, исключена генерация.
- Mobile Gradle: `generateMobileSyncConfig` запускается для assets/APK, но не для unit-тестов.
- Sync и Doctor: равные версии с разными данными остаются конфликтом; добавлены подробное сравнение, подтверждение выбора и ограниченный повтор YDB push.
- Время: UTC используется для sync-версий. Пользовательские и служебные локальные даты остаются локальными. Версия монотонна и при откате часов растёт на одну наносекунду.
- Файлы: desktop и mobile отклоняют пустые, абсолютные, drive-prefixed и выходящие за корень ключи до локального или удалённого ввода-вывода.
- Email: одна доставка на digest, автоматический повтор `SENDING`/`FAILED`, максимум три попытки, стабильный `Message-ID`, защита от устаревшего воркера.
- YDB: начальная схема обновлена; миграция существующей таблицы разделена на изменение схемы и заполнение данных.

Маршруты, повторное открытие вкладок, DataStore и пользовательский `log4j.properties` не менялись в этой работе.

## YDB-миграция

До миграции в `reminder_email_delivery` не было колонок `attempt_count` и `status_changed_at`. В таблице было 29 строк.

Применены два отдельных CLI-вызова:

1. `002_delivery_claim_state.sql` — добавлены колонки.
2. `003_delivery_claim_state_backfill.sql` — заполнены старые строки.

DDL и DML разделены, потому что YDB не разрешает смешивать их в одном запросе. Это ограничение описано в [документации YDB](https://ydb.tech/docs/en/concepts/transactions).

Проверка после миграции:

| Показатель | Результат |
|---|---:|
| `attempt_count` | `Uint64?` |
| `status_changed_at` | `Utf8?` |
| Строк всего | 29 |
| Строк с `NULL` в новых колонках | 0 |
| Минимальный `attempt_count` | 1 |
| Максимальный `attempt_count` | 1 |

Миграция не запускается при старте приложения. Данные доступа в вывод не записывались.

## Проверки

| Команда или проверка | Фактический результат |
|---|---|
| `.\gradlew detektAll --continue --console=plain --no-daemon --no-parallel --max-workers=1` | `BUILD SUCCESSFUL`, 533 задачи |
| `.\gradlew smokeDesktop :features:doctor:desktopTest :app:nocombroMobile:testDebugUnitTest --console=plain --no-daemon --no-parallel --max-workers=1` | `BUILD SUCCESSFUL`, 301 задача |
| `:database:desktopTest` | 83 теста, 10 окруженческих тестов пропущены, ошибок нет |
| `:features:doctor:desktopTest` | 1 тест прошёл |
| `:app:nocombroMobile:testDebugUnitTest` | 20 тестов прошли |
| `python -m unittest discover -s cloud/reminder-email/tests -v` | 19 тестов прошли |
| `:datastore:desktopTest` | `BUILD SUCCESSFUL`, `NO-SOURCE` |
| Mobile task graph | unit: генератора нет; merge assets/package APK: генератор есть |
| YDB schema/data check | типы верны, `NULL` нет |
| `git diff --check` | ошибок пробелов нет |

`DataStoreMigrationTest` в текущей ветке отсутствует. Новый тест не добавлялся по условию задачи.

Тесты покрывают:

- одинаковые версии с одинаковыми и разными данными;
- выбор локальной и удалённой записи в конфликте;
- откат часов и монотонную sync-версию;
- один remote reread и один retry после YDB reject;
- второй reject с безопасной ошибкой `table`/`sync_id`;
- допустимые и недопустимые файловые ключи без ввода-вывода при ошибке;
- свежий и просроченный `SENDING`;
- автоматический повтор `FAILED` и остановку после трёх ошибок;
- защиту финального статуса доставки от устаревшего воркера.

## Detekt

- XML-отчётов: 56.
- Реальных замечаний после удаления дублей: 840.
- Замечаний по сгенерированным исходникам: 0.
- Baseline-файлов: 0.
- Корневая задача `detektAutoFix` не запускалась.

### По модулям

| Модуль | Замечания |
|---|---:|
| `database` | 516 |
| `features/form/product` | 68 |
| `features/experiments` | 56 |
| `app/nocombroMobile` | 47 |
| `features/files` | 28 |
| `features/analytic/profitability` | 24 |
| `features/table/core` | 24 |
| `features/form/transaction` | 18 |
| `features/doctor` | 12 |
| `features/storage` | 10 |
| `rootnocombro` | 10 |
| `features/table/immutable` | 9 |
| `features/table/mutable` | 8 |
| `test/database-kit` | 3 |
| `features/label/thermal` | 2 |
| `app/desktopApp` | 1 |
| `features/form/declaration` | 1 |
| `features/form/document` | 1 |
| `features/form/expense` | 1 |
| `features/form/vendor` | 1 |

### По правилам

| Правило | Замечания |
|---|---:|
| `MagicNumber` | 606 |
| `UnreachableCode` | 97 |
| `LongMethod` | 23 |
| `ReturnCount` | 23 |
| `RedundantSuspendModifier` | 19 |
| `TooManyFunctions` | 18 |
| `CyclomaticComplexMethod` | 13 |
| `LongParameterList` | 13 |
| `UnusedPrivateMember` | 7 |
| `UnsafeCallOnNullableType` | 6 |
| `LoopWithTooManyJumpStatements` | 5 |
| `ImplicitDefaultLocale` | 2 |
| `MatchingDeclarationName` | 2 |
| `NestedBlockDepth` | 2 |
| `ThrowsCount` | 2 |
| `LargeClass` | 1 |
| `UnnecessarySafeCall` | 1 |

## Оставшиеся ограничения

- 840 замечаний Detekt оставлены как отдельный технический долг. Массовое автоисправление не запускалось.
- Реальная отправка через SMTP и развёртывание cloud-функции не выполнялись.
- Полный disaster-сценарий с реальными S3/YDB устройствами не запускался; окруженческие database-тесты остались пропущенными.
- `DataStoreMigrationTest` отсутствует, а задача модуля возвращает `NO-SOURCE`.
