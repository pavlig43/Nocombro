# Test Runbook

Короткая памятка для быстрых прогонов перед ручной приемкой и после загрузки реальных данных.

## Что уже есть

Автотесты сейчас покрывают в первую очередь `database`:

- пустая тестовая БД поднимается без ошибок;
- встроенный seeded-набор читается с ожидаемыми количествами;
- ключевые связи seeded-набора не развалены;
- seeded storage считается с ожидаемыми остатками и накопительными балансами;
- seeded profitability считается с ожидаемыми итогами;
- root bootstrap и открытие ключевых экранов проходят без падений;
- transaction form для seeded `BUY` и `SALE` собирает ожидаемые вкладки;
- product form для seeded `FOOD_PF` продукта собирает expected tabs, включая composition;
- declaration и expense формы поднимают базовые tabs и корректный title;
- vendor форма поднимает базовые tabs и корректный title;
- document форма готова к real-data smoke и автоматически пропускается без дампа;
- реальный дамп можно открыть в изолированной временной копии;
- для реального дампа есть быстрые sanity-checks по сущностям, связям, storage и profitability.

Файлы тестов:

- `database/src/desktopTest/kotlin/ru/pavlig43/database/DatabaseSeedSmokeTest.kt`
- `database/src/desktopTest/kotlin/ru/pavlig43/database/DatabaseSeedRelationsSmokeTest.kt`
- `database/src/desktopTest/kotlin/ru/pavlig43/database/RealDataDatabaseSmokeTest.kt`
- `database/src/desktopTest/kotlin/ru/pavlig43/database/StorageSmokeTest.kt`
- `database/src/desktopTest/kotlin/ru/pavlig43/database/RealDataStorageSmokeTest.kt`
- `features/analytic/profitability/src/desktopTest/kotlin/ru/pavlig43/profitability/internal/di/ProfitabilitySmokeTest.kt`
- `features/analytic/profitability/src/desktopTest/kotlin/ru/pavlig43/profitability/internal/di/RealDataProfitabilitySmokeTest.kt`
- `features/form/declaration/src/desktopTest/kotlin/ru/pavlig43/declaration/api/component/DeclarationFormComponentSmokeTest.kt`
- `features/form/declaration/src/desktopTest/kotlin/ru/pavlig43/declaration/api/component/RealDataDeclarationFormComponentSmokeTest.kt`
- `features/form/document/src/desktopTest/kotlin/ru/pavlig43/document/api/component/RealDataDocumentFormComponentSmokeTest.kt`
- `features/form/expense/src/desktopTest/kotlin/ru/pavlig43/expense/api/component/ExpenseFormComponentSmokeTest.kt`
- `features/form/expense/src/desktopTest/kotlin/ru/pavlig43/expense/api/component/RealDataExpenseFormComponentSmokeTest.kt`
- `features/form/product/src/desktopTest/kotlin/ru/pavlig43/product/api/component/ProductFormComponentSmokeTest.kt`
- `features/form/product/src/desktopTest/kotlin/ru/pavlig43/product/api/component/RealDataProductFormComponentSmokeTest.kt`
- `features/form/transaction/src/desktopTest/kotlin/ru/pavlig43/transaction/api/component/TransactionFormComponentSmokeTest.kt`
- `features/form/transaction/src/desktopTest/kotlin/ru/pavlig43/transaction/api/component/RealDataTransactionFormComponentSmokeTest.kt`
- `features/form/vendor/src/desktopTest/kotlin/ru/pavlig43/vendor/api/component/VendorFormComponentSmokeTest.kt`
- `features/form/vendor/src/desktopTest/kotlin/ru/pavlig43/vendor/api/component/RealDataVendorFormComponentSmokeTest.kt`
- `rootnocombro/src/desktopTest/kotlin/ru/pavlig43/rootnocombro/api/component/RootNocombroFlowSmokeTest.kt`

## Основные команды

Прогнать текущие desktop-тесты базы:

```powershell
.\gradlew :database:desktopTest
```

Прогнать весь desktop smoke-контур одной командой:

```powershell
.\gradlew smokeDesktop
```

Прогнать только core desktop smoke:

```powershell
.\gradlew smokeCoreDesktop
```

Прогнать только form desktop smoke:

```powershell
.\gradlew smokeFormsDesktop
```

Прогнать только seeded smoke:

```powershell
.\gradlew :database:desktopTest --tests "*DatabaseSeedSmokeTest*"
```

Прогнать только seeded relations smoke:

```powershell
.\gradlew :database:desktopTest --tests "*DatabaseSeedRelationsSmokeTest*"
```

Прогнать real-data smoke на реальном дампе:

```powershell
.\gradlew :database:desktopTest -Dnocombro.realData.dbPath="C:\path\to\nocombro.db" --tests "*RealDataDatabaseSmokeTest*"
```

Прогнать real-data storage smoke на реальном дампе:

```powershell
.\gradlew :database:desktopTest -Dnocombro.realData.dbPath="C:\path\to\nocombro.db" --tests "*RealDataStorageSmokeTest*"
```

Прогнать seeded profitability smoke:

```powershell
.\gradlew :features:analytic:profitability:desktopTest --tests "*ProfitabilitySmokeTest*"
```

Прогнать real-data profitability smoke на реальном дампе:

```powershell
.\gradlew :features:analytic:profitability:desktopTest -Dnocombro.realData.dbPath="C:\path\to\nocombro.db" --tests "*RealDataProfitabilitySmokeTest*"
```

Прогнать root flow smoke:

```powershell
.\gradlew :rootnocombro:desktopTest --tests "*RootNocombroFlowSmokeTest*"
```

Прогнать transaction form smoke:

```powershell
.\gradlew :features:form:transaction:desktopTest --tests "*TransactionFormComponentSmokeTest*"
```

Прогнать real-data transaction form smoke:

```powershell
.\gradlew :features:form:transaction:desktopTest -Dnocombro.realData.dbPath="C:\path\to\nocombro.db" --tests "*RealDataTransactionFormComponentSmokeTest*"
```

Прогнать product form smoke:

```powershell
.\gradlew :features:form:product:desktopTest --tests "*ProductFormComponentSmokeTest*"
```

Прогнать real-data product form smoke:

```powershell
.\gradlew :features:form:product:desktopTest -Dnocombro.realData.dbPath="C:\path\to\nocombro.db" --tests "*RealDataProductFormComponentSmokeTest*"
```

Прогнать declaration form smoke:

```powershell
.\gradlew :features:form:declaration:desktopTest --tests "*DeclarationFormComponentSmokeTest*"
```

Прогнать expense form smoke:

```powershell
.\gradlew :features:form:expense:desktopTest --tests "*ExpenseFormComponentSmokeTest*"
```

Прогнать vendor form smoke:

```powershell
.\gradlew :features:form:vendor:desktopTest --tests "*VendorFormComponentSmokeTest*"
```

Прогнать real-data declaration form smoke:

```powershell
.\gradlew :features:form:declaration:desktopTest -Dnocombro.realData.dbPath="C:\path\to\nocombro.db" --tests "*RealDataDeclarationFormComponentSmokeTest*"
```

Прогнать real-data document form smoke:

```powershell
.\gradlew :features:form:document:desktopTest -Dnocombro.realData.dbPath="C:\path\to\nocombro.db" --tests "*RealDataDocumentFormComponentSmokeTest*"
```

Прогнать real-data expense form smoke:

```powershell
.\gradlew :features:form:expense:desktopTest -Dnocombro.realData.dbPath="C:\path\to\nocombro.db" --tests "*RealDataExpenseFormComponentSmokeTest*"
```

Прогнать real-data vendor form smoke:

```powershell
.\gradlew :features:form:vendor:desktopTest -Dnocombro.realData.dbPath="C:\path\to\nocombro.db" --tests "*RealDataVendorFormComponentSmokeTest*"
```

Если рядом с файлом есть `nocombro.db-wal` и `nocombro.db-shm`, тестовый хелпер автоматически скопирует и их тоже.

## Что проверяют тесты

`DatabaseSeedSmokeTest`

- пустая БД реально пустая;
- seeded БД имеет ожидаемые базовые counts;
- seed не сломан на уровне схемы и записи данных.

`DatabaseSeedRelationsSmokeTest`

- декларация согласована с поставщиком;
- продукт и спецификация продукта связаны корректно;
- продажи, расходы и напоминания связаны с транзакцией.

`RealDataDatabaseSmokeTest`

- в дампе есть базовые сущности;
- декларации, расходы и напоминания не ссылаются в пустоту;
- выборочные записи открываются по `id`;
- у `buy`/`sale` не пустые ключевые поля и положительные количества.

`StorageSmokeTest`

- seeded storage по периоду даёт ожидаемые totals;
- баланс партии накапливается последовательно по движениям.

`RealDataStorageSmokeTest`

- product totals сходятся с суммой по партиям;
- `balanceOnEnd = before + incoming - outgoing` на уровне продукта и партии;
- выборочная партия имеет непротиворечивую историю движений.

`ProfitabilitySmokeTest`

- seeded profitability по марту 2026 даёт ожидаемые summary totals;
- продуктовые строки и детали согласованы с seeded sale-данными.

`RealDataProfitabilitySmokeTest`

- `summary.totalRevenue` сходится с суммой product revenue;
- `summary.batchExpenses` сходится с суммой product expenses;
- `summary.profit` считается консистентно;
- у выборочных продуктов детали сходятся с агрегатами.

`RootNocombroFlowSmokeTest`

- root стартует в `Tabs`;
- profitability открывается как дефолтный таб;
- drawer умеет открыть `storage` и `analytic`;
- таб-навигация умеет собрать product list и базовые формы по `id`;
- seeded CRUD-маршруты для vendor, declaration и expense открываются без падений.

`TransactionFormComponentSmokeTest`

- seeded `SALE` транзакция поднимает вкладки `sale`, `reminders` и `expenses`;
- seeded `BUY` транзакция поднимает вкладки `buy`, `reminders` и `expenses`;
- для обоих сценариев выбирается правильная рабочая вкладка и обновляется title формы.

`RealDataTransactionFormComponentSmokeTest`

- берёт первую real-data транзакцию поддерживаемого типа `BUY`, `SALE` или `OPZS`;
- поднимает `essentials`, `reminders` и правильные динамические вкладки для типа транзакции;
- сверяет выбранную рабочую вкладку и title формы с живыми данными.

`ProductFormComponentSmokeTest`

- seeded `FOOD_PF` продукт поднимает `essentials`, `specification`, `files`, `safety`, `declaration`;
- для `FOOD_PF` дополнительно появляется вкладка `composition`;
- после инициализации выбирается `essentials`, а title формы заполняется именем продукта.

`RealDataProductFormComponentSmokeTest`

- берёт первый real-data продукт из переданного дампа;
- поднимает `essentials`, `specification`, `files`, `safety`, `declaration`;
- для `FOOD_PF` дополнительно ожидает `composition`;
- сверяет выбранную вкладку и title формы с живыми данными.

`DeclarationFormComponentSmokeTest`

- seeded declaration поднимает `essential` и `files`;
- title формы заполняется именем декларации.

`RealDataDeclarationFormComponentSmokeTest`

- берёт первую real-data declaration из переданного дампа;
- поднимает `essential` и `files`;
- сверяет title формы с реальным `declaration.displayName`.

`ExpenseFormComponentSmokeTest`

- seeded expense поднимает `essentials` и `files`;
- title формы заполняется типом расхода.

`RealDataExpenseFormComponentSmokeTest`

- берёт первый real-data expense из переданного дампа;
- поднимает `essentials` и `files`;
- сверяет title формы с реальным `expenseType.displayName`.

`VendorFormComponentSmokeTest`

- seeded vendor поднимает `essential` и `files`;
- title формы заполняется именем поставщика.

`RealDataVendorFormComponentSmokeTest`

- берёт первого real-data vendor из переданного дампа;
- поднимает `essential` и `files`;
- сверяет title формы с реальным `vendor.displayName`.

`RealDataDocumentFormComponentSmokeTest`

- берёт первый реальный документ из переданного дампа;
- поднимает `essential` и `files`;
- сверяет title формы с реальным `document.displayName`.

## Как читать падения

Если падает `DatabaseSeedSmokeTest`:

- проблема в тестовой инфраструктуре;
- либо изменился `seedDatabase`,
- либо изменилась схема/DAO и тестовые ожидания устарели.

Если падает `DatabaseSeedRelationsSmokeTest`:

- likely сломалась связность между сущностями;
- либо changed business wiring в базе,
- либо изменились seeded-данные и надо обновить ожидания.

Если падает `RealDataDatabaseSmokeTest`:

- это уже сильный сигнал по реальным данным;
- сначала смотреть, какая именно сущность или связь не прошла;
- потом сверять, это дефект загрузки, реальный мусор в дампе или ошибочное предположение теста.

Если падает `RealDataStorageSmokeTest`:

- сначала смотреть, на каком уровне ломается инвариант: продукт, партия или отдельное движение;
- потом сверять, это ошибка расчета storage, дубль или пропуск движения, либо реально неожиданные данные в дампе.

Если падает `ProfitabilitySmokeTest` или `RealDataProfitabilitySmokeTest`:

- сначала смотреть, ломается ли summary, product row или batch detail;
- потом сверять, это ошибка распределения расходов, batch cost, sale-данных или сама бизнес-формула profitability.

Если падает `RootNocombroFlowSmokeTest`:

- сначала смотреть, это bootstrap, DI или main-tab navigation;
- потом проверять root wiring, drawer opening и сборку form/table screen по `MainTabConfig`.

Если падает `TransactionFormComponentSmokeTest`:

- сначала смотреть, собрались ли динамические вкладки для нужного `transactionType`;
- потом проверять `TransactionFormTabsComponent`, загрузку initial data и tab-navigation на UI thread.

Если падает `RealDataTransactionFormComponentSmokeTest`:

- сначала смотреть, есть ли в дампе поддерживаемые типы `BUY`, `SALE` или `OPZS`;
- потом проверять open-by-id, динамические вкладки и выбор стартовой вкладки по типу транзакции.

Если падает `ProductFormComponentSmokeTest`:

- сначала смотреть, собралась ли базовая сетка вкладок продукта;
- потом проверять `ProductFormTabsComponent`, условие добавления `composition` и загрузку seeded `FOOD_PF` данных.

Если падает `RealDataProductFormComponentSmokeTest`:

- сначала смотреть, какой `productType` пришёл из дампа и должен ли появляться `composition`;
- потом проверять open-by-id, базовую сетку вкладок и обновление title после init.

Если падает `DeclarationFormComponentSmokeTest` или `ExpenseFormComponentSmokeTest`:

- сначала смотреть, собрались ли `essential(s)` и `files`;
- потом проверять dependencies на files, single-line load и обновление title после init.

Если падает `VendorFormComponentSmokeTest`:

- сначала смотреть, собрались ли `essential` и `files`;
- потом проверять files dependencies и single-line init поставщика.

Если падает `RealDataDeclarationFormComponentSmokeTest`, `RealDataExpenseFormComponentSmokeTest` или `RealDataVendorFormComponentSmokeTest`:

- сначала смотреть, есть ли соответствующие записи в самом дампе;
- потом проверять open-by-id для формы, file-tab и обновление title на живых данных.

Если падает `RealDataDocumentFormComponentSmokeTest`:

- сначала смотреть, есть ли документы в самом дампе;
- потом проверять open-by-id для document формы и загрузку file-tab на живых данных.

## Рекомендуемый порядок на завтра

1. Получить путь к реальному `.db` файлу.
2. Сразу прогнать `RealDataDatabaseSmokeTest`.
3. Сразу после него прогнать `RealDataStorageSmokeTest`.
4. Сразу после него прогнать `RealDataProfitabilitySmokeTest`.
5. Быстро прогнать `RootNocombroFlowSmokeTest`, чтобы убедиться, что приложение поднимает ключевые экраны.
6. Быстро прогнать `TransactionFormComponentSmokeTest`, чтобы проверить ветки `BUY` и `SALE`.
7. Сразу после этого прогнать `RealDataTransactionFormComponentSmokeTest`.
8. Быстро прогнать `ProductFormComponentSmokeTest`, чтобы проверить rich product tabs.
9. Сразу после этого прогнать `RealDataProductFormComponentSmokeTest`.
10. Быстро прогнать `DeclarationFormComponentSmokeTest`, `ExpenseFormComponentSmokeTest` и `VendorFormComponentSmokeTest`.
11. Сразу после этого прогнать `RealDataDeclarationFormComponentSmokeTest`, `RealDataExpenseFormComponentSmokeTest` и `RealDataVendorFormComponentSmokeTest`.
12. Если в дампе есть документы, прогнать `RealDataDocumentFormComponentSmokeTest`.
13. Если smoke зелёный, открыть приложение и пройти [first-pass-checklist.md](C:/Users/user/AndroidStudioProjects/Nocombro/test/docs/first-pass-checklist.md).
14. Все наблюдения фиксировать в [real-data-observations-template.md](C:/Users/user/AndroidStudioProjects/Nocombro/test/docs/real-data-observations-template.md).
15. Если всплывут системные проблемы, сверяться с [real-data-risk-matrix.md](C:/Users/user/AndroidStudioProjects/Nocombro/test/docs/real-data-risk-matrix.md).

## Что можно добавить следующим

- отдельный тест на known-good реальный дамп, если появится стабильная эталонная копия.
