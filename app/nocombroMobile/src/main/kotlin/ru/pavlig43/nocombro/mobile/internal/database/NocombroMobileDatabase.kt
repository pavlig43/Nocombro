package ru.pavlig43.nocombro.mobile.internal.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.nocombro.mobile.internal.database.dao.MobileExperimentDao
import ru.pavlig43.nocombro.mobile.internal.database.dao.MobileExperimentEntryDao
import ru.pavlig43.nocombro.mobile.internal.database.dao.MobileExperimentReminderDao
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntryEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentReminderEntity

@Database(
    entities = [
        MobileExperimentEntity::class,
        MobileExperimentEntryEntity::class,
        MobileExperimentReminderEntity::class,
    ],
    version = 1,
)
@TypeConverters(NocombroMobileConverters::class)
abstract class NocombroMobileDatabase : RoomDatabase() {
    abstract val experimentDao: MobileExperimentDao

    abstract val experimentEntryDao: MobileExperimentEntryDao

    abstract val experimentReminderDao: MobileExperimentReminderDao

    companion object {
        fun create(context: Context): NocombroMobileDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                NocombroMobileDatabase::class.java,
                "nocombro-mobile.db",
            ).build()
        }
    }
}

class NocombroMobileConverters {
    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime = LocalDateTime.parse(value)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String = value.toString()
}
