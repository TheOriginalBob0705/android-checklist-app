package com.example.checklist.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checklist.App
import com.example.checklist.data.Repository
import com.example.checklist.viewmodel.ChecklistViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val app = application as App
                val factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ChecklistViewModel(Repository(app.db)) as T
                    }
                }
                Surface {
                    ChecklistNav(factory = factory)
                }
            }
        }
    }
}