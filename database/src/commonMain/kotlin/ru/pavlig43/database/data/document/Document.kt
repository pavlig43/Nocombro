package ru.pavlig43.database.data.document

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.pavlig43.core.UTC
import ru.pavlig43.core.data.Item


const val DOCUMENT_TABLE_NAME = "document"
@Entity(DOCUMENT_TABLE_NAME)
data class Document(

    @ColumnInfo("display_name")
    override val displayName: String = "",

    override val type: DocumentType,

    @ColumnInfo("created_at")
    override val createdAt: Long,

    override val comment:String ="",

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    ) : Item








