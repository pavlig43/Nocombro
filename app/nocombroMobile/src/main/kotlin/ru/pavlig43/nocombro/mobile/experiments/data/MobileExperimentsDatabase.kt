package ru.pavlig43.nocombro.mobile.experiments.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Room database для offline-данных mobile-экспериментов.
 */
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
    /**
     * DAO экспериментов.
     */
    abstract val experimentDao: MobileExperimentDao

    /**
     * DAO записей журнала.
     */
    abstract val experimentEntryDao: MobileExperimentEntryDao

    /**
     * DAO напоминаний.
     */
    abstract val experimentReminderDao: MobileExperimentReminderDao

    companion object {
        /**
         * Создаёт database instance для Android app context.
         */
        fun create(context: Context): MobileExperimentsDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MobileExperimentsDatabase::class.java,
                "nocombro-mobile.db",
            ).build()
        }
    }
}

/**
 * Room converters для kotlinx datetime типов.
 */
class MobileExperimentsConverters {
    /**
     * Читает [LocalDate] из строки БД.
     */
    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    /**
     * Пишет [LocalDate] в строку БД.
     */
    @TypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    /**
     * Читает [LocalDateTime] из строки БД.
     */
    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime = LocalDateTime.parse(value)

    /**
     * Пишет [LocalDateTime] в строку БД.
     */
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String = value.toString()
}
