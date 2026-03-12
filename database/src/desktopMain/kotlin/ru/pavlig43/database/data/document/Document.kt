package ru.pavlig43.database.data.document

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.SingleItem


const val DOCUMENT_TABLE_NAME = "document"
@Entity(DOCUMENT_TABLE_NAME)
data class Document(

    @ColumnInfo("display_name")
    val displayName: String = "",

    val type: DocumentType,

    @ColumnInfo("created_at")
     val createdAt: LocalDate,

    val comment:String ="",

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    ) : SingleItem








