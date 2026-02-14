# Бизнес-логика базы данных Nocombro

## Обзор проекта

Nocombro — это Kotlin Multiplatform приложение для управления:
- Товарами и партиями
- Транзакциями (покупка, продажа, производство)
- Декларациями и документами
- Расходами и напоминаниями

---

## Основные сущности

### 1. Vendor (Поставщик)

```kotlin
data class Vendor(
    val displayName: String,
    val comment: String,
    val id: Int
)
```

**Назначение:** Поставщики товаров/сырья.

---

### 2. Declaration (Декларация качества)

```kotlin
data class Declaration(
    val displayName: String,        // название декларации
    val vendorId: Int,              // поставщик
    val vendorName: String,
    val bornDate: LocalDate,        // дата создания декларации
    val bestBefore: LocalDate,      // срок годности
    val observeFromNotification: Boolean,
    val createdAt: LocalDate,
    val id: Int
)
```

**Назначение:** Документ качества (сертификат соответствия) от производителя.

**Важно:**
- Создаётся производителем и загружается на официальный сайт
- **Без декларации продукта не может быть**
- Действует ограниченное время (например, 3 года)
- В системе хранятся и просроченные декларации (для истории)

**Связи:**
- Один продукт может иметь **несколько деклараций** одновременно (старая + свежая)
- Одна декларация может покрывать **несколько продуктов** (группа товаров)
- Связь реализована через таблицу `ProductDeclarationIn` (M:N)

---

### 3. Product (Товар/Продукт)

```kotlin
data class Product(
    val type: ProductType,          // FOOD_BASE, FOOD_PF, FOOD_SALE, PACK
    val displayName: String,
    val createdAt: LocalDate,
    val comment: String,
    val id: Int
)
```

**Назначение:** Товары, сырьё, полуфабрикаты, готовая продукция, упаковка.

**Типы продуктов:**
- `FOOD_BASE` — Пищевой базовый (сырьё: мука, сахар, соль)
- `FOOD_PF` — Пищевой полуфабрикат (тесто, фарш)
- `FOOD_SALE` — Пищевой продукт для продажи (готовая продукция)
- `PACK` — Упаковка

---

### 4. Document (Техническая документация)

```kotlin
data class Document(
    val displayName: String,
    val type: DocumentType,         // GOST или SPECIFICATION
    val createdAt: LocalDate,
    val comment: String,
    val id: Int
)
```

**Назначение:** Техническая документация (ГОСТ, спецификации).

**Важное отличие от Declaration:**
- **Declaration** — критически важный документ качества (отслеживается срок, уведомления)
- **Document** — справочная техдокументация (не требует пристального внимания)

---

### 5. Batch (Партия товара)

```kotlin
data class Batch(
    val id: Int,
    val productId: Int,            // ссылка на Product
    val declarationId: Int,        // ссылка на Declaration
    val dateBorn: LocalDate        // дата производства (на упаковке)
)
```

**Назначение:** Физическая партия товара на складе.

**Важно:**
- `dateBorn` = дата производства с упаковки, **НЕ** дата поступления
- Связь с `Declaration` нужна для **истории** (даже если Declaration просрочится или Product поменяет её)
- Одна партия создаётся один раз (immutable)

**Связи:**
- `ForeignKey` на `Product` (RESTRICT)
- `ForeignKey` на `Declaration` (RESTRICT)

---

### 6. BatchMovement (Движение по партии)

```kotlin
data class BatchMovement(
    val batchId: Int,              // ссылка на Batch
    val movementType: MovementType, // INCOMING или OUTGOING
    val count: Int,                // количество в граммах/штуках
    val transactionId: Int,        // ссылка на Transaction
    val id: Int
)
```

**Назначение:** Учёт движения товара по партиям.

**Типы движения:**
- `INCOMING` — Приход (создание партии, покупка, производство, инвентаризация доначисление)
- `OUTGOING` — Расход (продажа, списание, производство, инвентаризация списание)

**Остаток партии:**
```
Остаток = SUM(INCOMING.count) - SUM(OUTGOING.count) по batchId
```

**Важно:**
- Дублирующие поля (`productId`, `declarationId`, `dateBorn`) **НЕ хранятся** — берутся из `Batch` через @Relation

---

### 7. Transaction (Транзакция)

```kotlin
data class Transaction(
    val transactionType: TransactionType,
    val createdAt: LocalDateTime,
    val comment: String,
    val isCompleted: Boolean,
    val id: Int
)
```

**Назначение:** Операция с товарами/партиями.

**Типы транзакций:**

| Тип | Описание | Что создаётся |
|-----|----------|---------------|
| **BUY** | Покупка | `Batch` + `BatchMovement(INCOMING)` + `BuyBDIn` |
| **SALE** | Продажа | `BatchMovement(OUTGOING)` |
| **OPZS** | Отчёт производства за смену | `BatchMovement` для компонентов (OUTGOING) + полуфабриката (INCOMING) |
| **WRITE_OFF** | Списание | `BatchMovement(OUTGOING)` (дегустация, порча, хищение) |
| **INVENTORY** | Инвентаризация | `BatchMovement` для каждой партии (INCOMING/OUTGOING) |

**Важно:**
- `WRITE_OFF` — любой расход, не связанный с продажей (дегустация, порча)
- При `INVENTORY` для каждой партии создаётся `BatchMovement` с нужным типом для корректировки остатков

---

### 8. BuyBDIn / BuyBD (Покупка)

**BuyBDIn (Entity в БД):**
```kotlin
data class BuyBDIn(
    val price: Int,                // цена закупки в копейках
    val count: Int,                // количество
    val transactionId: Int,
    // другие поля...
)
```

**BuyBD/Out (DTO для UI):**
```kotlin
data class BuyBD(
    val productName: String,       // через @Relation с Product
    val count: Int,
    val declarationName: String,   // через @Relation с Declaration
    val vendorName: String,        // через @Relation с Vendor
    val dateBorn: LocalDate,       // через @Relation с Batch
    val price: Int,                // из BuyBDIn
    val comment: String,
    val id: Int
)
```

**Логика:**
```
Покупка (BUY)
  ↓
Создаётся BuyBDIn (с ценой)
  ↓
Создаётся Batch (партия)
  ↓
Создаётся BatchMovement (INCOMING)
  ↓
BuyBD/Out собирается через @Relation для UI
```

**Маржа:** Высчитывается runtime для готового продукта.

---

### 9. CompositionIn (Состав продукта)

```kotlin
data class CompositionIn(
    val parentId: Int,             // родительский продукт
    val productId: Int,            // дочерний продукт (компонент)
    val count: Int,                // количество
    val id: Int
)
```

**Назначение:** Рецептура / состав полуфабрикатов.

**Примеры:**
- "Тесто" состоит из: Мука 500г + Яйца 2шт + Вода 200мл
- "Булочка с мясом" состоит из: Тесто 100г + Начинка 50г

**Использование:**
- Для ознакомления (просмотр состава продукта)
- Для **первичного заполнения формы OPZS** (шаблон)

**Важно:** Реальный расход компонентов фиксируется через `BatchMovement`, а не через `CompositionIn`.

---

### 10. ReminderBD (Напоминания)

```kotlin
data class ReminderBD(
    val transactionId: Int,        // привязка к транзакции
    val text: String,              // свободный текст
    val reminderDateTime: LocalDateTime,
    val id: Int
)
```

**Назначение:** Напоминания о необходимости оплаты/поставки по транзакции.

**Важно:** Пользователь сам вводит текст напоминания (свободный формат).

---

### 11. ExpenseBD (Расходы)

```kotlin
data class ExpenseBD(
    val transactionId: Int?,       // опциональная связь с транзакцией
    val expenseType: ExpenseType,
    val amount: Int,               // сумма в копейках
    val expenseDateTime: LocalDateTime,
    val comment: String,
    val id: Int
)
```

**Типы расходов:**
- `TRANSPORT_GASOLINE` — Бензин
- `TRANSPORT_DELIVERY` — Доставка
- `TRANSPORT_DEPRECIATION` — Амортизация авто
- `STATIONERY` — Канцелярия
- `COMMISSION` — Откаты (условное название расхода)

---

### 12. FileBD (Файлы)

```kotlin
data class FileBD(
    val ownerId: Int,
    val ownerFileType: OwnerType,  // DECLARATION, PRODUCT, VENDOR, DOCUMENT, TRANSACTION
    val path: String,
    val id: Int
)
```

**Назначение:** Универсальная система прикрепления файлов к любой сущности.

**Примеры:**
- Скан сертификата → `DECLARATION`
- Фото товара → `PRODUCT`
- Любые другие документы

---

## Диаграмма связей

```
Vendor (Поставщик)
  └── Declaration (Декларация качества)
        ├── bornDate (создания документа)
        ├── bestBefore (срок годности)
        └── (M:N) Product ←─ через ProductDeclarationIn
              ├── type, displayName, createdAt
              ├── (1:N) Batch
              │     └── dateBorn (дата производства)
              ├── (M:N) Product (состав) ←─ через CompositionIn
              └── (1:N) BatchMovement ←─ через Batch
                    ├── movementType (INCOMING/OUTGOING)
                    ├── count
                    └── (N:1) Transaction

Document (Техническая документация)
  ├── type: GOST / SPECIFICATION
  └── (1:N) FileBD

Transaction (Транзакция)
  ├── type: BUY/SALE/OPZS/WRITE_OFF/INVENTORY
  ├── createdAt, comment, isCompleted
  └── (1:N) BatchMovement

BuyBDIn (Покупка в БД)
  ├── batchId, price, count
  └── → BuyBD/Out (через @Relation для UI)

ReminderBD (Напоминания)
  └── (N:1) Transaction

ExpenseBD (Расходы)
  └── (N:1?) Transaction (опционально)

FileBD (Файлы)
  └── ownerId, ownerType
```

---

## Сценарии использования

### Сценарий 1: Покупка товара

1. Пользователь создаёт транзакцию типа `BUY`
2. Создаётся `BuyBDIn` с ценой закупки
3. Создаётся `Batch` (партия товара) с датой производства
4. Создаётся `BatchMovement` с `movementType = INCOMING`
5. UI получает `BuyBD` через @Relation

### Сценарий 2: Продажа товара

1. Пользователь **сам выбирает партию** из списка
2. Создаётся транзакция типа `SALE`
3. Создаётся `BatchMovement` с `movementType = OUTGOING`
4. Остаток партии пересчитывается: `SUM(INCOMING) - SUM(OUTGOING)`

### Сценарий 3: Производство полуфабриката (OPZS)

1. Пользователь создаёт отчёт `OPZS`
2. `CompositionIn` заполняет форму (шаблон рецептуры)
3. Для каждого компонента создаётся `BatchMovement` с `movementType = OUTGOING`
4. Для готового полуфабриката создаётся новый `Batch` + `BatchMovement` с `movementType = INCOMING`

### Сценарий 4: Инвентаризация

1. Пользователь создаёт транзакцию типа `INVENTORY`
2. Для каждой партии:
   - Если факт > остаток → `BatchMovement(INCOMING)` — доначисление
   - Если факт < остаток → `BatchMovement(OUTGOING)` — списание

### Сценарий 5: Списание (не продажа)

1. Пользователь создаёт транзакцию типа `WRITE_OFF`
2. Выбирает партию и количество
3. Создаётся `BatchMovement` с `movementType = OUTGOING`
4. Причина: дегустация, порча, хищение и т.д.

---

## Правила дизайна (Database Rules)

### 1. Используй @Relation вместо raw SQL JOINs

❌ **Плохо:**
```kotlin
@Query("""
    SELECT p.name, b.count, d.displayName
    FROM buy_bd b
    INNER JOIN products p ON b.product_id = p.id
    INNER JOIN declarations d ON b.declaration_id = d.id
""")
suspend fun getAllBuys(): List<BuyBD>
```

✅ **Хорошо:**
```kotlin
data class BuyBD(
    @Embedded val buy: BuyEntity,
    @Relation(parentColumn = "product_id", entityColumn = "id")
    val product: Product,
    @Relation(parentColumn = "declaration_id", entityColumn = "id")
    val declaration: Declaration
)
```

### 2. Храни десятичные числа как Int

- **Деньги** — в копейках (×100)
- **Вес** — в граммах (×1000)

Используй `decimalColumn()` из `TableCellTextFieldNumber.kt` для отображения.

### 3. Дата и время

- Используй готовые компоненты из `coreui`:
  - `DateTimeRow` — дата + время
  - `DateRow` — только дата
  - `DateTimePickerDialog` / `DatePickerDialog`

- В Decompose используй `SlotNavigation` для управления диалогами.

### 4. Без дублирования данных

Если данные есть в родительской сущности — не дублируй в дочерней.
- ❌ `BatchMovement` с `productId`, `declarationId`, `dateBorn`
- ✅ `BatchMovement` с `batchId` (остальное через @Relation)

---

## Планы на будущее

- [ ] Расчёт маржи runtime для готовой продукции
- [ ] Создание `BuyBDIn` entity

---

## История изменений

- **2024** — Рефакторинг: `TransactionProductBDIn` → `BatchMovement`
- Добавлена сущность `Batch` для партий товара
- Убраны дублирующие поля из `BatchMovement`
- Добавлена документация бизнес-логики
