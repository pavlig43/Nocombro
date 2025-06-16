package ru.pavlig43.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDataBaseBuilder(): RoomDatabase.Builder<NocombroDatabase> {
    val dbFile = File("data/databases/nocombro.db")

    val builder =  Room.databaseBuilder<NocombroDatabase>(
        name = dbFile.absolutePath,
    )
    return builder
}
