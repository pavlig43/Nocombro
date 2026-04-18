# Анализ архитектуры по `aalekhReport`

Дата анализа: 2026-04-18

Источники:
- `build/reports/aalekh/index.html`
- `settings.gradle.kts`
- `app/desktopApp/src/desktopMain/kotlin/ru/pavlig43/nocombro/Main.kt`
- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/component/RootNocombroComponent.kt`
- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabConfig.kt`
- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabChild.kt`
- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabNavigationComponent.kt`
- `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/di/RootNocombroModule.kt`
- `rootnocombro/build.gradle.kts`
- `features/files/build.gradle.kts`
- `features/manageitem/update/build.gradle.kts`
- `features/table/core/build.gradle.kts`
- `features/table/immutable/build.gradle.kts`
- `features/table/mutable/build.gradle.kts`
- `features/form/document/build.gradle.kts`
- `features/form/product/build.gradle.kts`
- `features/doctor/build.gradle.kts`

## Короткий вывод

У проекта нет циклов main-кода и нет формальных architectural violations в отчете. Это хороший базовый сигнал: граф пока еще контролируемый.

Главная проблема не в циклах, а в концентрации знаний и зависимостей в нескольких узлах:
- `:rootnocombro` как orchestration-модуль знает слишком много конкретных feature-реализаций
- связка `:features:table:*` + `:features:manageitem:*` + `:features:files` стала общим инфраструктурным пластом для многих экранов
- UI/features часто зависят от `:database` напрямую, а не через более узкие контракты
- формы повторяют один и тот же набор зависимостей, из-за чего изменение в инфраструктурной части размазывается по многим модулям

Итог: архитектура сейчас скорее не "ломается", а "срастается". Чтобы уменьшить связанность, нужно не бороться с циклами, а резать слишком широкие compile-time границы.

## Что показал отчет

### Общие метрики

- Всего модулей: `41`
- Всего зависимостей: `199`
- Main cycles: `0`
- Violations: `0`
- Max fan-out: `22`
- Max fan-in: `27`
- Average instability: `0.495`
- Critical path length: `11`
- God modules: `5`
- Isolated modules: `8`

### Critical path

Самая длинная цепочка зависимостей:

`:app:desktopApp` -> `:rootnocombro` -> `:features:doctor` -> `:features:files` -> `:features:manageitem:update` -> `:features:table:immutable` -> `:features:table:core` -> `:database` -> `:datetime` -> `:coreui` -> `:core`

Это важный симптом: путь от desktop entrypoint до базовых shared-модулей проходит через несколько feature-узлов, которые фактически стали инфраструктурой.

### God modules

Отчет выделил 5 hotspot-модулей:

- `:features:files`
- `:features:manageitem:update`
- `:features:table:core`
- `:features:table:immutable`
- `:features:table:mutable`

Это не просто "сложные" модули. Это модули, через которые проходит слишком много входящих и исходящих зависимостей одновременно.

### Изолированные модули

Изолированными отмечены:

- `:app`
- `:features`
- `:features:analytic`
- `:features:form`
- `:features:manageitem`
- `:features:sign`
- `:features:table`
- `:test`

Судя по именам, это в основном агрегирующие Gradle-узлы, а не реальная проблема архитектуры приложения.

## Где именно высокая связанность

### 1. `:rootnocombro` перегружен compile-time знанием о feature-модулях

По отчету:
- fan-out `22`
- fan-in `1`

`rootnocombro/build.gradle.kts` напрямую тянет почти все feature-модули. Это ожидаемо для orchestration-слоя, но проблема в том, что он зависит не только от API-контрактов, а местами знает конкретные реализации.

`MainTabNavigationComponent.kt`:
- импортирует большое количество конкретных компонент и builder-типов
- вручную маппит `DrawerDestination -> MainTabConfig`
- вручную маппит `MainTabConfig -> MainTabChild`
- вручную создает конкретные feature-компоненты
- держит в себе `TabOpener` и правила переходов между feature-модулями

Это означает, что новый экран или изменение сигнатуры конкретной фичи почти гарантированно требует правки в root orchestration.

Архитектурный эффект:
- высокий fan-out у root-модуля
- слабая локальность изменений
- риск, что root начинает решать доменные вопросы, а не только композицию

### 2. Табличный стек стал скрытым shared-platform слоем

По отчету:
- `:features:table:core` fan-in `10`, fan-out `5`
- `:features:table:immutable` fan-in `10`, fan-out `7`
- `:features:table:mutable` fan-in `7`, fan-out `9`

Сейчас таблицы уже не выглядят как "одна фича". Они стали кросс-фичевой платформой отображения и редактирования.

Симптомы:
- формы массово зависят от `table:core`, `table:immutable`, `table:mutable`
- `storage` и analytics тоже заходят в этот стек
- `table:immutable` и `table:mutable` экспортируют зависимости через `api(...)`

Особенно важно:
- `features/table/immutable/build.gradle.kts` содержит `api(projects.features.table.core)` и `api(projects.datetime)`
- `features/table/mutable/build.gradle.kts` содержит тот же паттерн

Это расширяет transitive surface area. Потребитель "берет immutable", но фактически получает наружу и `table:core`, и `datetime`.

Архитектурный эффект:
- downstream-модули знают о большем числе типов, чем им реально нужно
- сложнее сузить границы и заменить реализацию
- таблицы становятся обязательным центром почти для любой data-heavy фичи

### 3. `:features:manageitem:update` и `:features:files` стали инфраструктурными монолитами

По отчету:
- `:features:manageitem:update` fan-in `9`, fan-out `7`
- `:features:files` fan-in `8`, fan-out `7`

`features/files/build.gradle.kts`:
- зависит от `database`
- зависит от `manageitem:loadinitdata`
- зависит от `manageitem:update`

`features/manageitem/update/build.gradle.kts`:
- зависит от `database`
- зависит от `table:immutable`
- зависит от `manageitem:loadinitdata`

Из графа видно, что эти модули тянут:
- формы
- doctor
- root
- табличный слой

То есть "files" и "manage item update" уже не выглядят как локальные бизнес-фичи. Они работают как shared service layer, но оформлены как feature-модули. Это делает архитектуру неочевидной: имя обещает прикладную фичу, а фактическая роль ближе к reusable platform/service.

Архитектурный эффект:
- неправильный уровень абстракции в названиях и зависимостях
- тяжело понять, что является доменом, а что инфраструктурой
- слишком многие модули зависят от одинакового тяжелого набора возможностей

### 4. Формы повторяют одинаковый тяжелый dependency bundle

Примеры по Gradle:
- `features/form/document`
- `features/form/product`
- `features/form/vendor`
- `features/form/declaration`
- `features/form/transaction`
- `features/form/expense`

Типовой набор зависимостей у форм:
- `database`
- `features:files`
- `features:manageitem:update`
- `features:manageitem:loadinitdata`
- `features:table:immutable`
- `features:table:mutable`
- `features:table:core`

Это говорит, что у форм смешаны несколько ролей:
- экран редактирования сущности
- работа с таблицами выбора/связанными списками
- работа с файлами
- инициализация справочников/lookup-данных
- прямой доступ к данным

Архитектурный эффект:
- формы трудно менять изолированно
- любое изменение в shared-инфраструктуре бьет сразу по пакету форм
- форма становится "workflow shell", но при этом не выделена как отдельная архитектурная роль

### 5. `:database` слишком близко к UI-фичам

По отчету:
- fan-in `17`

На `database` напрямую завязаны:
- формы
- doctor
- files
- notification
- storage
- таблицы
- profitability
- root

Если UI- или screen-модули напрямую зависят от `database`, значит граница между presentation/application и data слоем широкая. Даже если внутри есть репозитории, на уровне модульной схемы это не зафиксировано.

Архитектурный эффект:
- трудно заменить хранилище
- data concerns протекают наверх
- feature-модули компилируются против слишком богатого набора data-типов

### 6. Shared base-модули тянутся почти везде

Самые "центральные" узлы графа:
- `:core` fan-in `27`
- `:coreui` fan-in `25`
- `:theme` fan-in `25`
- `:corekoin` fan-in `23`

Часть этого нормальна. Но в сумме это говорит, что большинство feature-модулей строятся на одинаковом толстом baseline.

Это не обязательно плохо, но здесь есть риск:
- `core` становится складом общих сущностей "на всякий случай"
- `coreui` начинает содержать не только generic UI primitives, но и полудоменные helper'ы
- `corekoin` делает DI частью compile-time контракта почти каждой фичи

## Корневые причины

### 1. Root composition и feature registry не разведены

Сейчас `rootnocombro` одновременно:
- знает маршруты
- знает drawer destinations
- знает concrete component constructors
- знает feature dependencies classes
- знает правила переходов между feature-экранами

Нужна граница между:
- registry доступных экранов и контрактов
- фактической сборкой конкретных feature-компонентов

### 2. Shared behavior вынесен не в api/service-модули, а в "фичи"

`files`, `manageitem:update`, частично table-модули используются как foundation для других экранов. Но архитектурно они оформлены как обычные фичи, а не как:
- `api`
- `domain`
- `application`
- `infra`

Из-за этого граф выглядит как feature-to-feature coupling, хотя фактически это использование общих сервисов.

### 3. Текущие модульные границы ориентированы на экран, а не на capability

Например, форма может нуждаться в:
- работе с вложениями
- выборе значений из справочника
- обновлении связанных items
- таблице для просмотра/выбора

Сейчас эти capability приходят тяжелыми пакетами через крупные модули. Нужны более узкие контракты.

### 4. `api` используется на слишком низком уровне без строгой необходимости

Для `table:immutable` и `table:mutable` это особенно заметно. Если downstream-модулям реально не нужны типы из `table:core` и `datetime`, то `api` здесь расширяет поверхность зависимости без пользы.

## Что улучшать в архитектуре

### Приоритет 1. Сузить роль `rootnocombro`

Цель:
- оставить root как orchestrator
- убрать из него знание о конкретных реализациях, где это возможно

Практические шаги:
- ввести `FeatureEntry` или `ScreenFactory` контракты для экранов, которые root только регистрирует и вызывает
- перевести `MainTabNavigationComponent` от большого `when` к реестру маршрутов
- вынести маппинг `DrawerDestination -> ScreenDescriptor` в отдельный registry-слой
- отделить навигационные контракты (`openProduct`, `openDocument`, `openStorage`) от прямого создания конкретных компонентов

Ожидаемый эффект:
- меньше fan-out у root
- проще добавлять новые экраны
- меньше мест, которые нужно править при изменении feature API

### Приоритет 2. Разрезать `files` и `manageitem:update` на более узкие capability-модули

Вместо тяжёлых feature-зависимостей многим экраном нужны только части поведения.

Кандидаты на выделение:
- `features/files/api`
- `features/files/impl`
- `features/manageitem/update/api`
- `features/manageitem/update/impl`
- возможно `features/manageitem/lookup` или `features/manageitem/initdata`

Если форма использует только контракт загрузки/привязки файлов, ей не нужно знать внутреннюю реализацию feature-модуля.

Ожидаемый эффект:
- уменьшение fan-in hotspot-модулей
- более честные архитектурные границы
- упрощение тестирования и замены реализаций

### Приоритет 3. Превратить table-стек в платформу с четкими слоями

Текущая структура намекает на это, но границы еще широкие.

Рекомендуемое направление:
- `table:model` или `table:api` для общих контрактов и DTO/Ui-model
- `table:core` только для truly shared engine
- `table:immutable` и `table:mutable` как отдельные реализации
- проверить, где можно заменить `api(...)` на `implementation(...)`

Особенно важно:
- если потребители не используют публичные типы `table:core` и `datetime` напрямую, убрать их из re-export через `api`

Ожидаемый эффект:
- уменьшение transitive coupling
- более понятные роли модулей
- возможность использовать один table-capability без подтягивания всего набора

### Приоритет 4. Отодвинуть `database` от экранов

Цель:
- feature-модули зависят от более узких repository/service interfaces
- `database` реализует эти контракты, но не просачивается в UI-слой

Минимальный шаг:
- не переписывать всё сразу
- начать с самых горячих направлений: forms, files, storage, doctor

Рабочий вариант:
- выделить contracts в `api` или `domain`-модули
- DI связывает интерфейсы с `database`-реализациями
- screen/features получают только нужные интерфейсы

Ожидаемый эффект:
- меньше fan-in у `database`
- слабее compile-time сцепление UI и data
- проще двигаться к тестируемым use case/repository слоям

### Приоритет 5. Схлопнуть дублирование в forms

Сейчас шесть form-модулей тянут почти один и тот же infrastructure bundle.

Есть два возможных пути:

Путь A:
- выделить общий `form-shell`/`form-workflow` модуль
- оставить в конкретных формах только специфику сущности

Путь B:
- выделить capability-контракты отдельно
- формы собирают только нужные части: attachments, lookup, table-select, save/update

Практически я бы начал с B, потому что он лучше уменьшает связанность, а не просто переносит ее в новый общий модуль.

## Предлагаемая последовательность рефакторинга

### Этап 1. Быстрые архитектурные выигрыши без большого переписывания

1. Проверить все `api(...)` в `table:immutable` и `table:mutable`, заменить на `implementation(...)`, где re-export не нужен.
2. Выделить узкие api-контракты для `files` и `manageitem:update`.
3. Убрать зависимости `rootnocombro` на concrete implementation там, где можно опираться на API/Factory.
4. Зафиксировать в отдельном документе список допустимых направлений зависимостей:
   - `app -> root`
   - `root -> feature api`
   - `feature impl -> shared/data`
   - `ui feature -/-> database` как целевое правило

### Этап 2. Упростить навигационную композицию

1. Ввести `ScreenDescriptor`/`FeatureEntry`.
2. Вынести регистрацию экранов из большого `when`.
3. Сократить знание root о конкретных form/table component constructors.

### Этап 3. Развести capability-слои

1. Отделить attachments/files capability.
2. Отделить item update/loadinitdata capability.
3. Отделить table contracts от table implementations.

### Этап 4. Ослабить связку с `database`

1. Начать с doctor/files/forms.
2. Вынести repository interfaces.
3. Оставить `database` инфраструктурной реализацией, а не общим compile-time фундаментом для UI.

## На что не стоит тратить силы в первую очередь

- На борьбу с циклами: их сейчас нет.
- На isolated parent-модули вроде `:features` и `:test`: это почти наверняка harmless Gradle aggregation.
- На косметическое дробление `core` без предварительного снижения зависимости root/forms/table/files от тяжелых capability-модулей.

## Практический целевой образ

Архитектурно проект станет заметно чище, если прийти к такой форме:

- `app` знает только root
- `root` знает только feature entry/api-контракты и навигационные descriptor'ы
- feature-экраны знают только нужные capability interfaces
- capability implementations знают data/infrastructure
- `database` не импортируется напрямую большинством экранных модулей
- table/files/manageitem перестают быть "скрытыми бог-модулями" и становятся набором более узких сервисов

## Итог

Отчет показывает, что архитектура пока здорова по формальным признакам, но уже накопила выраженные coupling hotspot'ы. Самые важные из них:

1. `rootnocombro` как чрезмерно конкретный orchestration-узел
2. `table:*` как слишком широкий shared-platform слой
3. `files` и `manageitem:update` как инфраструктурные монолиты под видом feature-модулей
4. формы как потребители одного и того же тяжелого dependency bundle
5. `database` как слишком близкая зависимость для UI/features

Если брать это в работу, я бы начал не с полного рефакторинга, а с серии маленьких шагов:
- сузить `api`-поверхности
- выделить capability contracts
- ослабить compile-time знание root о concrete features
- постепенно выдавить `database` из screen-модулей за интерфейсы

Это даст заметное уменьшение связанности без необходимости сразу ломать всю текущую сборку приложения.
