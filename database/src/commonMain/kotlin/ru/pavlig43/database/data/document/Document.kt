package ru.pavlig43.database.data.document

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.pavlig43.core.UTC
import ru.pavlig43.database.data.common.data.Item

@Entity("document")
data class Document(

    @ColumnInfo("display_name")
    override val displayName: String,

    override val type: DocumentType,

    val path: String,

    override val createdAt: UTC,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    ):Item


