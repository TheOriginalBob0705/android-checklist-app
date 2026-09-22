package com.example.checklist.data

import androidx.room.*

@Entity(tableName = "checklists")
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sections",
    foreignKeys = [ForeignKey(
        entity = ChecklistEntity::class,
        parentColumns = ["id"],
        childColumns = ["checklistId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("checklistId")]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val checklistId: Long,
    val title: String,
    val orderIndex: Int
)

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("checklistId"), Index("sectionId")]
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val checklistId: Long,
    val sectionId: Long? = null, // null => ungrouped
    val text: String,
    val checked: Boolean = false,
    val orderIndex: Int
)

// Aggregates

// Entries are read once for the whole checklist and grouped by section in the view model.
data class ChecklistFull(
    @Embedded val checklist: ChecklistEntity,
    @Relation(parentColumn = "id", entityColumn = "checklistId")
    val sections: List<SectionEntity>,
    @Relation(parentColumn = "id", entityColumn = "checklistId")
    val entries: List<EntryEntity>
)
