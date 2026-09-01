package ai.orkk.shoelog.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ai.orkk.shoelog.AppContainer
import ai.orkk.shoelog.ui.home.HomeScreen
import ai.orkk.shoelog.ui.home.HomeViewModel

object Routes {
    const val HOME = "home"
    const val SHOES = "shoes"
    const val SHOE_DETAIL = "shoe/{shoeId}"
    const val SHOE_EDITOR = "shoe/edit?shoeId={shoeId}"
    const val EXERCISES = "exercises?exerciseId={exerciseId}"
    const val SETTINGS = "settings"

    fun shoeDetail(id: Long) = "shoe/$id"
    fun shoeEditor(id: Long? = null) = "shoe/edit?shoeId=${id ?: -1}"
    fun exercises(id: String = "") = "exercises?exerciseId=$id"
}

private data class MainDestination(val route: String, val label: String, val symbol: String)

private val mainDestinations = listOf(
    MainDestination(Routes.HOME, "홈", "●"),
    MainDestination(Routes.EXERCISES, "달리기", "↗"),
    MainDestination(Routes.SETTINGS, "설정", "⚙"),
)

@Composable
fun ShoeLogApp(container: AppContainer) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in mainDestinations.map { it.route }) {
                NavigationBar {
                    mainDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(destination.symbol) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
                val state by homeViewModel.state.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    onRefresh = homeViewModel::refresh,
                    onAddShoe = { navController.navigate(Routes.shoeEditor()) },
                    onOpenShoe = { navController.navigate(Routes.shoeDetail(it)) },
                    onOpenExercises = { navController.navigate(Routes.exercises()) },
                )
            }
            composable(Routes.SHOES) { FeaturePlaceholder("러닝화") }
            composable(Routes.SHOE_DETAIL) { FeaturePlaceholder("러닝화 상세") }
            composable(Routes.SHOE_EDITOR) { FeaturePlaceholder("러닝화 추가·수정") }
            composable(Routes.EXERCISES) { FeaturePlaceholder("달리기 기록") }
            composable(Routes.SETTINGS) { FeaturePlaceholder("설정") }
        }
    }
}

@Composable
private fun FeaturePlaceholder(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}
