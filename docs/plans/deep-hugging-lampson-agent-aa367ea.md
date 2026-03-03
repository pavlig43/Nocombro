# Code Review Plan: Branch 87-feature_storage

## Обзор
Выполняется подробный code review ветки `87-feature_storage` для Kotlin Multiplatform приложения Nocombro.

## Файлы для проверки
- Database: StorageDao.kt, StorageProduct.kt, MovementOut.kt
- Storage feature: StorageComponent.kt, StorageScreen.kt, Columns.kt
- Immutable tables: DecimalColumn.kt
- DI: StorageModule.kt

## Критерии проверки
1. Imports (без wildcards)
2. Database (@Relation вместо JOIN)
3. Decimal fields (Int для хранения)
4. Dialogs (SlotNavigation)
5. Security (SQL injection)
6. Code quality и архитектура
