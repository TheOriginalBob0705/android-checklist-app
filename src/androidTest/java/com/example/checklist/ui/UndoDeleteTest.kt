package com.example.checklist.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.checklist.data.AppDatabase
import com.example.checklist.data.Repository
import com.example.checklist.ui.screens.ChecklistDetailScreen
import com.example.checklist.viewmodel.ChecklistViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UndoDeleteTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repo: Repository
    private lateinit var vm: ChecklistViewModel
    private var checklistId = 0L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repo = Repository(db)
        vm = ChecklistViewModel(repo)
        runBlocking {
            checklistId = repo.addChecklist("Trip")
            repo.addEntry(checklistId, null, "socks", 0)
            repo.addEntry(checklistId, null, "shirt", 1)
        }
    }

    @After
    fun tearDown() = db.close()

    private fun showScreen() {
        composeRule.setContent {
            MaterialTheme {
                ChecklistDetailScreen(id = checklistId, onBack = {}, vm = vm)
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("socks")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun swipingAnEntryDeletesItAndOffersUndo() {
        showScreen()

        composeRule.onNodeWithText("socks").performTouchInput { swipeLeft() }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Undo")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Undo").assertIsDisplayed()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("socks")).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun tappingUndoBringsTheEntryBack() {
        showScreen()

        composeRule.onNodeWithText("socks").performTouchInput { swipeLeft() }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Undo")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Undo").performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("socks")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("socks").assertIsDisplayed()
    }
}
