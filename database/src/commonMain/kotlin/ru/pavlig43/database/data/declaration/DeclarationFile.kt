package ru.pavlig43.database.data.declaration

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.data.FileData

@Entity(
    tableName = "declaration_file",
    foreignKeys = [ForeignKey(
        entity = DeclarationIn::class,
        parentColumns = ["id"],
        childColumns = ["declaration_id"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class DeclarationFile(

    @ColumnInfo("declaration_id")
    val declarationId: Int,

    @ColumnInfo("path")
    override val path: String,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
): FileData