package com.example.checklist.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.checklist.ui.screens.ChecklistDetailScreen
import com.example.checklist.ui.screens.ChecklistListScreen
import com.example.checklist.viewmodel.ChecklistViewModel

object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{id}"
}

@Composable
fun ChecklistNav(
    vm: ChecklistViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController, startDestination = Routes.LIST, modifier = modifier) {
        composable(Routes.LIST) {
            ChecklistListScreen(onOpen = { id -> navController.navigate("detail/$id") }, vm = vm)
        }
        composable(Routes.DETAIL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: -1L
            ChecklistDetailScreen(id = id, onBack = { navController.popBackStack() }, vm = vm)
        }
    }
}
