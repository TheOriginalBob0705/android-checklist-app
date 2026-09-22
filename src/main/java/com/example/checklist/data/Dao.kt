package com.example.checklist.data

import androidx.room.*
import androidx.room.Dao
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ChecklistEntity>>

    @Query("""
        SELECT c.*,
               COUNT(e.id) AS total,
               COALESCE(SUM(CASE WHEN e.checked THEN 1 ELSE 0 END), 0) AS done
        FROM checklists c
        LEFT JOIN entries e ON e.checklistId = c.id
        GROUP BY c.id
        ORDER BY c.createdAt DESC
    """)
    fun observeSummaries(): Flow<List<ChecklistSummary>>

    @Insert
    suspend fun insert(checklist: ChecklistEntity): Long

    @Update
    suspend fun update(checklist: ChecklistEntity)

    @Delete
    suspend fun delete(checklist: ChecklistEntity)
}

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE checklistId = :checklistId ORDER BY orderIndex ASC")
    fun observe(checklistId: Long): Flow<List<SectionEntity>>

    @Insert
    suspend fun insert(section: SectionEntity): Long

    @Update
    suspend fun update(section: SectionEntity)

    @Query("UPDATE sections SET orderIndex = :order WHERE id = :id")
    suspend fun setOrder(id: Long, order: Int)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(section: SectionEntity)
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE checklistId = :checklistId ORDER BY orderIndex ASC")
    fun observe(checklistId: Long): Flow<List<EntryEntity>>

    @Insert
    suspend fun insert(entry: EntryEntity): Long

    // Targeted write so a toggle can't clobber a concurrent text edit.
    @Query("UPDATE entries SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("UPDATE entries SET sectionId = :sectionId, orderIndex = :order WHERE id = :id")
    suspend fun setPlacement(id: Long, sectionId: Long?, order: Int)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun restore(entry: EntryEntity)

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Delete
    suspend fun delete(entry: EntryEntity)
}

@Dao
interface AggregateDao {
    @Transaction
    @Query("SELECT * FROM checklists WHERE id = :checklistId LIMIT 1")
    fun observeChecklist(checklistId: Long): Flow<ChecklistFull?>
}