package ru.pavlig43.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDataBaseBuilder(): RoomDatabase.Builder<NocombroDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "nocombro.db")

    val builder =  Room.databaseBuilder<NocombroDatabase>(
        name = dbFile.absolutePath,
    )
        .fallbackToDestructiveMigration(dropAllTables = true)
    return builder
}
