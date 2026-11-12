package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.Habit
import com.example.data.model.Task
import com.example.data.model.TaskFilterTab
import com.example.data.model.TaskSortOption
import com.example.data.model.ThemePreset
import com.example.data.preferences.MelloPreferencesRepository
import com.example.data.preferences.MelloUserPreferences
import com.example.data.repository.MelloRepository
import com.example.ui.components.AddHabitSheet
import com.example.ui.components.AddTaskSheet
import com.example.ui.components.MelloBottomNavigation
import com.example.ui.components.MelloNavTab
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingAuthScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.signOutFromMello
import com.example.ui.theme.MelloTheme
import com.example.ui.theme.MelloThemeColors
import com.example.ui.viewmodel.MelloViewModel
import com.example.util.ReminderNotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderNotificationHelper.ensureChannelCreated(this)
        setContent {
            MelloRootApp()
        }
    }
}

@Composable
fun MelloRootApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    // Single shared preferences repository instance across root and ViewModel
    val preferencesRepo = remember(appContext) { MelloPreferencesRepository(appContext) }
    val preferences by preferencesRepo.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = MelloUserPreferences()
    )

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isDemoMode by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isDemoMode = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    MelloTheme(
        preset = preferences.themePreset,
        reducedMotion = preferences.reducedMotion
    ) {
        val activeUser = currentUser
        if (activeUser == null && !isDemoMode) {
            val onCompleteOnboarding = remember(preferencesRepo, scope) {
                {
                    scope.launch { preferencesRepo.setOnboardingCompleted(true) }
                    Unit
                }
            }
            val onAuthSuccess = remember {
                { currentUser = Firebase.auth.currentUser }
            }
            val onEnterDemoMode = remember {
                { isDemoMode = true }
            }

            OnboardingAuthScreen(
                hasCompletedOnboarding = preferences.hasCompletedOnboarding,
                onCompleteOnboarding = onCompleteOnboarding,
                onAuthSuccess = onAuthSuccess,
                onEnterDemoMode = onEnterDemoMode
            )
        } else {
            val sessionKey = activeUser?.uid ?: "demo_mode_session"
            val displayName = activeUser?.displayName ?: "Mello Explorer"
            val email = activeUser?.email ?: ""

            key(sessionKey) {
                val viewModel: MelloViewModel = viewModel(
                    key = sessionKey,
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            val repo = if (activeUser != null) {
                                val dbId = app.getString(R.string.firestore_database_id)
                                val db = FirebaseFirestore.getInstance(dbId)
                                MelloRepository(db)
                            } else {
                                null
                            }
                            MelloViewModel(
                                repository = repo,
                                preferencesRepository = preferencesRepo,
                                currentUserId = sessionKey,
                                userDisplayName = displayName,
                                userEmail = email,
                                isDemoMode = activeUser == null
                            )
                        }
                    }
                )

                val onExitToAuth = remember(activeUser, context, scope) {
                    {
                        if (activeUser != null) {
                            val credManager = CredentialManager.create(context)
                            signOutFromMello(credManager, scope) {
                                isDemoMode = false
                                currentUser = null
                            }
                        } else {
                            isDemoMode = false
                        }
                    }
                }

                MelloAuthenticatedShell(
                    viewModel = viewModel,
                    onExitToAuth = onExitToAuth
                )
            }
        }
    }
}

@Composable
fun MelloAuthenticatedShell(
    viewModel: MelloViewModel,
    onExitToAuth: () -> Unit
) {
    val palette = MelloThemeColors.palette
    val reducedMotion = MelloThemeColors.reducedMotion
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by rememberSaveable { mutableStateOf(MelloNavTab.HOME) }
    var showCalendarScreen by rememberSaveable { mutableStateOf(false) }

    var showTaskSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    var showHabitSheet by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    // Stable callback references to prevent unnecessary child screen recompositions
    val onSelectDate = remember(viewModel) { { date: LocalDate -> viewModel.selectDate(date) } }
    val onNavigateMonth = remember(viewModel) { { delta: Long -> viewModel.navigateCalendarMonth(delta) } }
    val onToggleTask = remember(viewModel) { { task: Task -> viewModel.toggleTaskCompleted(task) } }
    val onDeleteTask = remember(viewModel) { { id: String -> viewModel.deleteTask(id) } }
    val onToggleHabitDate = remember(viewModel) {
        { habitId: String, date: LocalDate -> viewModel.toggleHabitOnDate(habitId, date) }
    }
    val onDismissError = remember(viewModel) { { viewModel.clearError() } }
    val onSelectFilterTab = remember(viewModel) { { tab: TaskFilterTab -> viewModel.setTaskFilterTab(tab) } }
    val onSearchQueryChange = remember(viewModel) { { q: String -> viewModel.setSearchQuery(q) } }
    val onSelectCategoryFilter = remember(viewModel) { { cat: String? -> viewModel.setCategoryFilter(cat) } }
    val onSelectPriorityFilter = remember(viewModel) { { prio: String? -> viewModel.setPriorityFilter(prio) } }
    val onDeleteHabit = remember(viewModel) { { id: String -> viewModel.deleteHabit(id) } }
    val onSelectTheme = remember(viewModel) { { preset: ThemePreset -> viewModel.selectThemePreset(preset) } }
    val onUpdateDisplayName = remember(viewModel) { { name: String -> viewModel.updateCustomDisplayName(name) } }
    val onSetNotificationsEnabled = remember(viewModel) { { enabled: Boolean -> viewModel.setNotificationsEnabled(enabled) } }
    val onSetReducedMotion = remember(viewModel) { { enabled: Boolean -> viewModel.setReducedMotion(enabled) } }
    val onAddCustomCategory = remember(viewModel) { { cat: String -> viewModel.addCustomCategory(cat) } }

    val onOpenCalendar = remember { { showCalendarScreen = true } }
    val onCloseCalendar = remember { { showCalendarScreen = false } }
    val onOpenTasksTab = remember { { currentTab = MelloNavTab.TASKS } }
    val onOpenHabitsTab = remember { { currentTab = MelloNavTab.HABITS } }
    val onOpenProfileTab = remember { { currentTab = MelloNavTab.PROFILE } }
    val onQuickAddTask = remember {
        {
            editingTask = null
            showTaskSheet = true
        }
    }
    val onEditTask = remember {
        { task: Task ->
            editingTask = task
            showTaskSheet = true
        }
    }
    val onAddHabit = remember {
        {
            editingHabit = null
            showHabitSheet = true
        }
    }
    val onEditHabit = remember {
        { habit: Habit ->
            editingHabit = habit
            showHabitSheet = true
        }
    }
    val onAddTaskForDate = remember(viewModel) {
        { date: LocalDate ->
            viewModel.selectDate(date)
            editingTask = null
            showTaskSheet = true
        }
    }
    val onSelectTab = remember {
        { tab: MelloNavTab ->
            showCalendarScreen = false
            currentTab = tab
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Back button returns to Home tab from secondary tabs
    BackHandler(enabled = showCalendarScreen || currentTab != MelloNavTab.HOME) {
        if (showCalendarScreen) {
            showCalendarScreen = false
        } else {
            currentTab = MelloNavTab.HOME
        }
    }

    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (!showCalendarScreen) {
                MelloBottomNavigation(
                    currentTab = currentTab,
                    onSelectTab = onSelectTab
                )
            }
        },
        floatingActionButton = {
            if (showCalendarScreen || currentTab == MelloNavTab.HOME || currentTab == MelloNavTab.TASKS || currentTab == MelloNavTab.HABITS) {
                FloatingActionButton(
                    onClick = {
                        if (currentTab == MelloNavTab.HABITS && !showCalendarScreen) {
                            editingHabit = null
                            showHabitSheet = true
                        } else {
                            editingTask = null
                            showTaskSheet = true
                        }
                    },
                    shape = CircleShape,
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary,
                    modifier = Modifier.testTag("main_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (currentTab == MelloNavTab.HABITS) "Add Habit" else "Add Task"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showCalendarScreen) {
                CalendarScreen(
                    uiState = uiState,
                    onSelectDate = onSelectDate,
                    onNavigateMonth = onNavigateMonth,
                    onBack = onCloseCalendar,
                    onAddTaskForDate = onAddTaskForDate,
                    onToggleTask = onToggleTask,
                    onEditTask = onEditTask,
                    onDeleteTask = onDeleteTask
                )
            } else {
                Crossfade(
                    targetState = currentTab,
                    animationSpec = if (reducedMotion) tween(0) else tween(220),
                    label = "tabTransition"
                ) { tab ->
                    when (tab) {
                        MelloNavTab.HOME -> {
                            HomeScreen(
                                uiState = uiState,
                                customDisplayName = preferences.customDisplayName,
                                onSelectDate = onSelectDate,
                                onOpenCalendar = onOpenCalendar,
                                onOpenTasksTab = onOpenTasksTab,
                                onOpenHabitsTab = onOpenHabitsTab,
                                onOpenProfileTab = onOpenProfileTab,
                                onQuickAddTask = onQuickAddTask,
                                onToggleTask = onToggleTask,
                                onEditTask = onEditTask,
                                onDeleteTask = onDeleteTask,
                                onToggleHabitToday = onToggleHabitDate,
                                onDismissError = onDismissError
                            )
                        }

                        MelloNavTab.TASKS -> {
                            TasksScreen(
                                uiState = uiState,
                                customCategories = preferences.customCategories,
                                onSelectFilterTab = onSelectFilterTab,
                                onSearchQueryChange = onSearchQueryChange,
                                onSelectCategoryFilter = onSelectCategoryFilter,
                                onSelectPriorityFilter = onSelectPriorityFilter,
                                onToggleSortOption = {
                                    val next = if (uiState.sortOption == TaskSortOption.DUE_DATE) {
                                        TaskSortOption.CREATED_DATE
                                    } else {
                                        TaskSortOption.DUE_DATE
                                    }
                                    viewModel.setSortOption(next)
                                },
                                onOpenCalendar = onOpenCalendar,
                                onAddTask = onQuickAddTask,
                                onToggleTask = onToggleTask,
                                onEditTask = onEditTask,
                                onDeleteTask = onDeleteTask
                            )
                        }

                        MelloNavTab.HABITS -> {
                            HabitsScreen(
                                uiState = uiState,
                                onSelectDate = onSelectDate,
                                onAddHabit = onAddHabit,
                                onEditHabit = onEditHabit,
                                onDeleteHabit = onDeleteHabit,
                                onToggleHabitDate = onToggleHabitDate
                            )
                        }

                        MelloNavTab.PROFILE -> {
                            ProfileScreen(
                                uiState = uiState,
                                preferences = preferences,
                                onSelectTheme = onSelectTheme,
                                onUpdateDisplayName = onUpdateDisplayName,
                                onSetNotificationsEnabled = onSetNotificationsEnabled,
                                onSetReducedMotion = onSetReducedMotion,
                                onSignInFromDemo = onExitToAuth,
                                onSignOut = onExitToAuth,
                                onDeleteAccount = {
                                    viewModel.deleteAccountDataAndReset {
                                        onExitToAuth()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTaskSheet) {
        AddTaskSheet(
            initialTask = editingTask,
            defaultDate = LocalDate.ofEpochDay(uiState.selectedDateEpochDay),
            customCategories = preferences.customCategories,
            onAddCustomCategory = onAddCustomCategory,
            onDismiss = {
                showTaskSheet = false
                editingTask = null
            },
            onSave = { id, title, desc, cat, prio, dueEpoch, dueTime, reminder ->
                viewModel.saveTask(
                    context = context,
                    existingId = id,
                    title = title,
                    description = desc,
                    category = cat,
                    priority = prio,
                    dueDateEpochDay = dueEpoch,
                    dueTime = dueTime,
                    hasReminder = reminder
                )
            },
            onDelete = onDeleteTask
        )
    }

    if (showHabitSheet) {
        AddHabitSheet(
            initialHabit = editingHabit,
            onDismiss = {
                showHabitSheet = false
                editingHabit = null
            },
            onSave = { id, title, desc, color, iconName, isActive ->
                viewModel.saveHabit(
                    existingId = id,
                    title = title,
                    description = desc,
                    color = color,
                    iconName = iconName,
                    isActive = isActive
                )
            },
            onDelete = onDeleteHabit
        )
    }
}
