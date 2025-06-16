package ru.pavlig43.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDataBaseBuilder(context: Context): RoomDatabase.Builder<NocombroDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("rm.db")
    return Room.databaseBuilder<NocombroDatabase>(
        context = context,
        name = dbFile.name,
    )
}
