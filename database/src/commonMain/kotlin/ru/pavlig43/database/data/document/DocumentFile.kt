package ru.pavlig43.database.data.document

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.data.FileData

@Entity(
    tableName = "document_file",
    foreignKeys = [ForeignKey(
        entity = Document::class,
        parentColumns = ["id"],
        childColumns = ["document_id"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class DocumentFile(

    @ColumnInfo("document_id")
    val documentId: Int,

    @ColumnInfo("path")
    override val path: String,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
):FileData