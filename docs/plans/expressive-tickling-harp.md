# Fix ProfitabilityDao KSP Errors

## Changes to make in `database/src/desktopMain/kotlin/ru/pavlig43/database/data/money/profitability/ProfitabilityDao.kt`

### 1. Fix @Relation parentColumn (line 68)
Change from `parentColumn = "transaction_id"` to `parentColumn = "transactionId"`

### 2. Add @Transaction annotation (line 22)
Add `@Transaction` to `observeOnSale` function

### 3. Add @SuppressWarnings annotation (line 22)
Add `@SuppressWarnings(RoomWarnings.QUERY_MISMATCH)` to suppress unused columns warning
