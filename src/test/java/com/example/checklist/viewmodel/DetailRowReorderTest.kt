package com.example.checklist.viewmodel

import com.example.checklist.data.ChecklistEntity
import com.example.checklist.data.EntryEntity
import com.example.checklist.data.SectionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DetailRowReorderTest {

    private fun entry(id: Long, sectionId: Long?, text: String, order: Int, checked: Boolean = false) =
        EntryEntity(id = id, checklistId = 1, sectionId = sectionId, text = text, orderIndex = order, checked = checked)

    private fun section(id: Long, title: String, order: Int) =
        SectionEntity(id = id, checklistId = 1, title = title, orderIndex = order)

    /** Ungrouped "loose", then section Bag with "socks" and "shirt". */
    private val detail = ChecklistDetail(
        checklist = ChecklistEntity(id = 1, name = "Trip", createdAt = 0),
        ungrouped = listOf(entry(1, null, "loose", 0)),
        sections = listOf(
            SectionGroup(section(10, "Bag", 0), listOf(entry(2, 10, "socks", 0), entry(3, 10, "shirt", 1)))
        )
    )

    @Test
    fun `rows interleave headers, entries and add buttons`() {
        assertEquals(
            listOf(
                "header-0", "entry-1", "add-0",
                "header-10", "entry-2", "entry-3", "add-10",
                "bottom-space"
            ),
            detail.toRows().map { it.key }
        )
    }

    @Test
    fun `a header cannot be dragged`() {
        val rows = detail.toRows()
        assertSame(rows, moveRow(rows, from = 0, to = 2))
    }

    @Test
    fun `an entry cannot be dropped on the trailing spacer`() {
        val rows = detail.toRows()
        assertSame(rows, moveRow(rows, from = 1, to = rows.lastIndex))
    }

    @Test
    fun `an entry cannot be dropped above the first header`() {
        val rows = detail.toRows()
        assertSame(rows, moveRow(rows, from = 4, to = 0))
    }

    @Test
    fun `dragging an entry under another header reassigns its section`() {
        val rows = detail.toRows()
        // "loose" (index 1, ungrouped) dropped between "socks" and "shirt". The target index is
        // 4 rather than 5 because removing it first shifts everything below up by one.
        val moved = moveRow(rows, from = 1, to = 4)

        assertEquals(
            listOf(
                EntryPlacementLike(2, 10, 0),
                EntryPlacementLike(1, 10, 1),
                EntryPlacementLike(3, 10, 2)
            ),
            moved.toPlacements().map { EntryPlacementLike(it.id, it.sectionId, it.orderIndex) }
        )
    }

    @Test
    fun `placements renumber each section from zero`() {
        val rows = detail.toRows()
        val moved = moveRow(rows, from = 5, to = 4)

        val placements = moved.toPlacements().map { EntryPlacementLike(it.id, it.sectionId, it.orderIndex) }
        assertEquals(
            listOf(
                EntryPlacementLike(1, null, 0),
                EntryPlacementLike(3, 10, 0),
                EntryPlacementLike(2, 10, 1)
            ),
            placements
        )
    }

    @Test
    fun `a checklist with no sections has no headers to anchor against`() {
        val flat = ChecklistDetail(
            checklist = ChecklistEntity(id = 1, name = "Flat", createdAt = 0),
            ungrouped = listOf(entry(1, null, "a", 0), entry(2, null, "b", 1)),
            sections = emptyList()
        )

        val rows = flat.toRows()
        assertEquals(listOf("entry-1", "entry-2", "bottom-space"), rows.map { it.key })

        val moved = moveRow(rows, from = 1, to = 0)
        assertEquals(
            listOf(EntryPlacementLike(2, null, 0), EntryPlacementLike(1, null, 1)),
            moved.toPlacements().map { EntryPlacementLike(it.id, it.sectionId, it.orderIndex) }
        )
    }

    @Test
    fun `progress counts checked entries across every section`() {
        val counted = ChecklistDetail(
            checklist = ChecklistEntity(id = 1, name = "Trip", createdAt = 0),
            ungrouped = listOf(entry(1, null, "loose", 0, checked = true)),
            sections = listOf(
                SectionGroup(
                    section(10, "Bag", 0),
                    listOf(entry(2, 10, "socks", 0, checked = true), entry(3, 10, "shirt", 1))
                )
            )
        )

        assertEquals(3, counted.total)
        assertEquals(2, counted.done)
    }

    private data class EntryPlacementLike(val id: Long, val sectionId: Long?, val orderIndex: Int)
}
