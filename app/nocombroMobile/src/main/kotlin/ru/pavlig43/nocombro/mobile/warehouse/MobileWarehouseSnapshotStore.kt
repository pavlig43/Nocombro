package ru.pavlig43.nocombro.mobile.warehouse

import kotlinx.coroutines.flow.Flow

interface MobileWarehouseSnapshotStore {
    fun observeSnapshot(): Flow<MobileWarehouseSnapshot?>

    suspend fun replaceSnapshot(snapshot: MobileWarehouseSnapshot)
}
