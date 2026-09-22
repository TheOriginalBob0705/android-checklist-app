package com.example.checklist.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import com.example.checklist.App
import com.example.checklist.ui.theme.ChecklistTheme
import com.example.checklist.viewmodel.ChecklistViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChecklistViewModel by viewModels {
        ChecklistViewModel.factory((application as App).db)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 36 is edge-to-edge regardless; this keeps the system bar icons legible in both themes.
        enableEdgeToEdge()

        setContent {
            ChecklistTheme {
                Surface {
                    ChecklistNav(vm = viewModel)
                }
            }
        }
    }
}
