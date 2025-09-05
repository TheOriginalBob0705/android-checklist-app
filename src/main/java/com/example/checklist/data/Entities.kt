package com.example.checklist.data

import androidx.room.*
import kotlinx.coroutines.flow.*

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
data class SectionWithEntries(
    @Embedded val section: SectionEntity,
    @Relation(parentColumn = "id", entityColumn = "sectionId")
    val entries: List<EntryEntity>
)

data class ChecklistFull(
    @Embedded val checklist: ChecklistEntity,
    @Relation(parentColumn = "id", entityColumn = "checklistId", entity = SectionEntity::class)
    val sections: List<SectionWithEntries>,
    @Relation(parentColumn = "id", entityColumn = "checklistId", entity = EntryEntity::class)
    val allEntries: List<EntryEntity>
) {
    val ungrouped: List<EntryEntity>
        get() = allEntries.filter { it.sectionId == null }.sortedBy { it.orderIndex }
}