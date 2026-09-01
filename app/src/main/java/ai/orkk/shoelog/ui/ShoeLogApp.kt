package ai.orkk.shoelog.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ai.orkk.shoelog.AppContainer
import ai.orkk.shoelog.ui.home.HomeScreen
import ai.orkk.shoelog.ui.home.HomeViewModel
import ai.orkk.shoelog.ui.exercises.ExerciseListScreen
import ai.orkk.shoelog.ui.exercises.ExerciseListViewModel
import ai.orkk.shoelog.ui.settings.SettingsScreen
import ai.orkk.shoelog.ui.settings.SettingsViewModel
import ai.orkk.shoelog.ui.shoes.ShoeDetailScreen
import ai.orkk.shoelog.ui.shoes.ShoeEditorScreen
import ai.orkk.shoelog.ui.shoes.ShoeEditorViewModel
import ai.orkk.shoelog.ui.shoes.ShoeListScreen
import kotlinx.coroutines.launch

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

private data class MainDestination(
    val route: String,
    val navigationRoute: String,
    val label: String,
    val symbol: String,
)

private val mainDestinations = listOf(
    MainDestination(Routes.HOME, Routes.HOME, "홈", "●"),
    MainDestination(Routes.EXERCISES, Routes.exercises(), "달리기", "↗"),
    MainDestination(Routes.SETTINGS, Routes.SETTINGS, "설정", "⚙"),
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
                                navController.navigate(destination.navigationRoute) {
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
            composable(Routes.SHOES) {
                var includeRetired by remember { mutableStateOf(false) }
                val shoesFlow = remember(includeRetired) { container.shoeRepository.observeShoes(includeRetired) }
                val shoes by shoesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                ShoeListScreen(
                    shoes = shoes,
                    includeRetired = includeRetired,
                    onIncludeRetiredChange = { includeRetired = it },
                    onAdd = { navController.navigate(Routes.shoeEditor()) },
                    onOpen = { navController.navigate(Routes.shoeDetail(it)) },
                )
            }
            composable(Routes.SHOE_DETAIL) { entry ->
                val shoeId = entry.arguments?.getString("shoeId")?.toLongOrNull() ?: return@composable
                val shoeFlow = remember(shoeId) { container.shoeRepository.observeShoe(shoeId) }
                val exercisesFlow = remember { container.exerciseRepository.observeExercises() }
                val shoe by shoeFlow.collectAsStateWithLifecycle(initialValue = null)
                val exercises by exercisesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                val scope = rememberCoroutineScope()
                ShoeDetailScreen(
                    shoe = shoe,
                    exercises = exercises.filter { it.assignedShoeId == shoeId },
                    onBack = navController::popBackStack,
                    onEdit = { navController.navigate(Routes.shoeEditor(shoeId)) },
                    onRetireToggle = {
                        val current = shoe ?: return@ShoeDetailScreen
                        scope.launch { container.shoeRepository.setRetired(shoeId, !current.retired) }
                    },
                )
            }
            composable(Routes.SHOE_EDITOR) { entry ->
                val requestedId = entry.arguments?.getString("shoeId")?.toLongOrNull()
                val shoeId = requestedId?.takeIf { it != -1L }
                val editor: ShoeEditorViewModel = viewModel(
                    key = "shoe-editor-${shoeId ?: "new"}",
                    factory = ShoeEditorViewModel.factory(container.shoeRepository, shoeId),
                )
                val state by editor.state.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    if (uri != null) {
                        runCatching {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        }
                        editor.setPhotoUri(uri.toString())
                    }
                }
                LaunchedEffect(state.savedShoeId) {
                    if (state.savedShoeId != null) navController.popBackStack()
                }
                ShoeEditorScreen(
                    state = state,
                    onFormChange = editor::updateForm,
                    onPickPhoto = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onSave = editor::save,
                    onBack = navController::popBackStack,
                )
            }
            composable(Routes.EXERCISES) { entry ->
                val selectedId = entry.arguments?.getString("exerciseId")?.takeIf(String::isNotBlank)
                val exerciseViewModel: ExerciseListViewModel = viewModel(
                    key = "exercises-${selectedId.orEmpty()}",
                    factory = ExerciseListViewModel.factory(
                        container.exerciseRepository,
                        container.shoeRepository,
                        selectedId,
                    ),
                )
                val state by exerciseViewModel.state.collectAsStateWithLifecycle()
                ExerciseListScreen(
                    state = state,
                    onUnassignedOnlyChange = exerciseViewModel::setUnassignedOnly,
                    onSelect = exerciseViewModel::select,
                    onAssign = exerciseViewModel::assign,
                )
            }
            composable(Routes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
                val state by settingsViewModel.state.collectAsStateWithLifecycle()
                val healthPermissionLauncher = rememberLauncherForActivityResult(
                    PermissionController.createRequestPermissionResultContract(),
                ) { granted -> settingsViewModel.refreshPermissions(granted) }
                SettingsScreen(
                    state = state,
                    onRequestPermissions = {
                        healthPermissionLauncher.launch(container.healthConnectDataSource.requiredPermissions)
                    },
                    onRequestHistory = {
                        healthPermissionLauncher.launch(setOf(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY))
                    },
                    onSync = settingsViewModel::sync,
                    onAutoAssignChange = settingsViewModel::setAutoAssign,
                    onSampleModeChange = settingsViewModel::setSampleMode,
                )
            }
        }
    }
}
