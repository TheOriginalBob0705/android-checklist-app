package com.example.checklist.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checklist.ui.screens.ChecklistDetailScreen
import com.example.checklist.ui.screens.ChecklistListScreen
import com.example.checklist.viewmodel.ChecklistViewModel


object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{id}"
}

@Composable
fun ChecklistNav(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    factory: ViewModelProvider.Factory
): NavHostController {
    NavHost(navController, startDestination = Routes.LIST, modifier = modifier) {
        composable(Routes.LIST) {
            val vm: ChecklistViewModel = viewModel(factory = factory)
            ChecklistListScreen(onOpen = { id -> navController.navigate("detail/$id") }, vm = vm)
        }
        composable(Routes.DETAIL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: -1L
            val vm: ChecklistViewModel = viewModel(factory = factory)
            ChecklistDetailScreen(id = id, onBack = { navController.popBackStack() }, vm = vm)
        }
    }
    return navController
}