# План: Code Review и создание PR для feature_storage

## Обзор изменений

**Ветка:** `87-feature_storage` (от master)

**Количество коммитов:** 27

**Основные изменения:**
- Новая функциональность Storage (таблица остатков на складе)
- Возможность раскрытия/скрытия партий в таблице
- Предупреждение об отрицательных остатках
- Оптимизация StorageDao для больших наборов данных
- Добавление агрегированных полей в StorageProduct

## Изменённые файлы (55 файлов, +3259/-289)

### Database
- `database/data/batch/dao/BatchMovementDao.kt`
- `database/data/storage/dao/StorageDao.kt` (новый)
- `database/data/storage/StorageProduct.kt`
- `database/DatabaseSeeder.kt` (рефакторинг)
- `database/NocombroDatabase.kt`

### Storage feature
- `features/storage/...` (новые файлы)
  - `StorageComponent.kt`
  - `StorageScreen.kt`
  - `Columns.kt`
  - `StorageDependencies.kt`

### Immutable таблицы
- `features/table/immutable/...` (обновления для поддержки expand/collapse)

### Navigation
- `rootnocombro/internal/navigation/` (добавлен Storage tab)

## Результаты Code Review

### ✅ Что прошло проверку
- ✅ **Imports** — нет wildcard импортов
- ✅ **Decimal fields** — все балансы хранятся в Int (копейки/граммы)
- ✅ **Dialogs** — правильно используется SlotNavigation
- ✅ **Security** — нет SQL injection (параметризованные запросы)
- ✅ **Архитектура** — хорошее разделение слоёв, использование Flow

### ⚠️ Критические проблемы (рекомендуется исправить)

1. **Потенциальная NPE** — `StorageDao.kt:44`
   - `movementOuts.first()` бросит NoSuchElementException если список пуст
   - **Решение:** использовать `firstOrNull()` с обработкой null

2. **Валидация DateTimePeriod** — `StorageComponent.kt:222-225`
   - Нет проверки что `start <= end`
   - **Решение:** добавить `init { require(start <= end) }`

3. **JOIN вместо @Relation** — `StorageDao.kt:25-31`
   - Нарушает `.claude/rules/database.md`
   - **Решение:** задокументировать исключение или переписать

### 🔧 Рекомендации (необязательно)
- Упростить ExpandedCell через `when`
- Извлечь magic numbers в константы
- Добавить KDoc для StorageDao

### Общая оценка: 7.5/10

---

## План выполнения

### Шаг 1: Исправление проблем (опционально)
- [ ] Добавить защиту от NPE в StorageDao.kt:44
- [ ] Добавить валидацию DateTimePeriod
- [ ] Задокументировать исключение для JOIN

### Шаг 2: Создание PR
1. Собрать актуальные изменения с master (`git fetch && git rebase master`)
2. Написать описание PR
3. Создать PR через GitHub CLI (`gh pr create`)

## Команда для запуска билда
```bash
./gradlew build --continue
```
