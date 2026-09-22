package com.example.checklist.viewmodel

import com.example.checklist.data.ChecklistEntity
import com.example.checklist.data.ChecklistFull
import com.example.checklist.data.EntryEntity
import com.example.checklist.data.SectionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistDetailMappingTest {

    private val checklist = ChecklistEntity(id = 1, name = "Trip", createdAt = 0)

    private fun entry(id: Long, sectionId: Long?, text: String, order: Int) =
        EntryEntity(id = id, checklistId = 1, sectionId = sectionId, text = text, orderIndex = order)

    @Test
    fun `groups entries by section and drops none`() {
        val full = ChecklistFull(
            checklist = checklist,
            sections = listOf(
                SectionEntity(id = 10, checklistId = 1, title = "Bag", orderIndex = 0),
                SectionEntity(id = 20, checklistId = 1, title = "Docs", orderIndex = 1)
            ),
            entries = listOf(
                entry(1, null, "loose", 0),
                entry(2, 10, "socks", 0),
                entry(3, 20, "passport", 0),
                entry(4, 10, "shirt", 1)
            )
        )

        val detail = full.toDetail()

        assertEquals(listOf("loose"), detail.ungrouped.map { it.text })
        assertEquals(listOf("Bag", "Docs"), detail.sections.map { it.section.title })
        assertEquals(listOf("socks", "shirt"), detail.sections[0].entries.map { it.text })
        assertEquals(listOf("passport"), detail.sections[1].entries.map { it.text })
    }

    @Test
    fun `sorts sections and entries by orderIndex regardless of query order`() {
        val full = ChecklistFull(
            checklist = checklist,
            sections = listOf(
                SectionEntity(id = 20, checklistId = 1, title = "second", orderIndex = 1),
                SectionEntity(id = 10, checklistId = 1, title = "first", orderIndex = 0)
            ),
            entries = listOf(
                entry(1, null, "u2", 1),
                entry(2, 10, "b", 1),
                entry(3, null, "u1", 0),
                entry(4, 10, "a", 0)
            )
        )

        val detail = full.toDetail()

        assertEquals(listOf("first", "second"), detail.sections.map { it.section.title })
        assertEquals(listOf("u1", "u2"), detail.ungrouped.map { it.text })
        assertEquals(listOf("a", "b"), detail.sections[0].entries.map { it.text })
    }

    @Test
    fun `section with no entries maps to an empty list`() {
        val full = ChecklistFull(
            checklist = checklist,
            sections = listOf(SectionEntity(id = 10, checklistId = 1, title = "Empty", orderIndex = 0)),
            entries = emptyList()
        )

        val detail = full.toDetail()

        assertEquals(1, detail.sections.size)
        assertEquals(emptyList<EntryEntity>(), detail.sections[0].entries)
        assertEquals(emptyList<EntryEntity>(), detail.ungrouped)
    }
}
