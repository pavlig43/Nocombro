package ru.pavlig43.nocombro.mobile.experiments.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@Database(
    entities = [
        MobileExperimentEntity::class,
        MobileExperimentEntryEntity::class,
        MobileExperimentReminderEntity::class,
    ],
    version = 1,
)
@TypeConverters(MobileExperimentsConverters::class)
abstract class MobileExperimentsDatabase : RoomDatabase() {
    abstract val experimentDao: MobileExperimentDao
    abstract val experimentEntryDao: MobileExperimentEntryDao
    abstract val experimentReminderDao: MobileExperimentReminderDao

    companion object {
        fun create(context: Context): MobileExperimentsDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MobileExperimentsDatabase::class.java,
                "nocombro-mobile.db",
            ).build()
        }
    }
}

class MobileExperimentsConverters {
    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime = LocalDateTime.parse(value)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String = value.toString()
}
