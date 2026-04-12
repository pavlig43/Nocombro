package ru.pavlig43.doctor.api

import ru.pavlig43.files.api.LocalFilesMaintenanceRepository
import ru.pavlig43.files.api.RemoteFilesMaintenanceRepository

class DoctorDependencies(
    val localFilesMaintenanceRepository: LocalFilesMaintenanceRepository,
    val remoteFilesMaintenanceRepository: RemoteFilesMaintenanceRepository,
)
