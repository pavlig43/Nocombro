package ru.pavlig43.database.data.document

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import ru.pavlig43.core.UTC
import ru.pavlig43.core.data.Item

@Entity("document")
data class Document(

    @ColumnInfo("display_name")
    override val displayName: String = "",

    override val type: DocumentType,

    @ColumnInfo("created_at")
    override val createdAt: UTC,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    ) : Item


@Entity(
    tableName = "document_file_path",
    foreignKeys = [ForeignKey(
        entity = Document::class,
        parentColumns = ["id"],
        childColumns = ["document_id"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class DocumentFilePath(

    @ColumnInfo("document_id")
    val documentId: Int,

    @ColumnInfo("file_path")
    val filePath: String,

    @ColumnInfo("file_extension")
    val fileExtension: String,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

data class DocumentWithFiles(
    @Embedded
    val document: Document,
    @Relation(
        parentColumn = "id",
        entityColumn = "document_id"
    )
    val files: List<DocumentFilePath>
) : Item by document



