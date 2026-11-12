package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Habit
import com.example.data.model.HabitInsights
import com.example.data.model.HabitLog
import com.example.data.model.MelloDateFormatters
import com.example.data.model.Task
import com.example.data.model.TaskFilterTab
import com.example.data.model.TaskPriority
import com.example.data.model.TaskSortOption
import com.example.data.model.ThemePreset
import com.example.data.preferences.MelloPreferencesRepository
import com.example.data.preferences.MelloUserPreferences
import com.example.data.repository.MelloRepository
import com.example.util.ReminderNotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class MelloUiState(
    val isDemoMode: Boolean = false,
    val userId: String = "",
    val userDisplayName: String = "Mello Planner",
    val userEmail: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val selectedDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val currentCalendarMonth: YearMonth = YearMonth.now(),
    val tasks: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList(),
    // Task filters & search
    val taskFilterTab: TaskFilterTab = TaskFilterTab.TODAY,
    val searchQuery: String = "",
    val selectedCategoryFilter: String? = null,
    val selectedPriorityFilter: String? = null,
    val sortOption: TaskSortOption = TaskSortOption.DUE_DATE,
    // Precomputed derived state for zero-cost UI rendering
    val filteredTasks: List<Task> = emptyList(),
    val habitInsights: HabitInsights = HabitInsights(),
    val activeHabits: List<Habit> = emptyList(),
    val logsByHabitId: Map<String, List<HabitLog>> = emptyMap(),
    val completedDatesByHabitId: Map<String, Set<String>> = emptyMap(),
    val habitLogDates: Set<LocalDate> = emptySet(),
    val completedTaskDates: Set<LocalDate> = emptySet(),
    val scheduledTaskDates: Set<LocalDate> = emptySet(),
    val activityDates: Set<LocalDate> = emptySet(),
    val completedHabitIdsForSelectedDate: Set<String> = emptySet(),
    val selectedDayTasks: List<Task> = emptyList(),
    val completedDayTasksCount: Int = 0,
    val upcomingTasks: List<Task> = emptyList(),
    val completedTasksCount: Int = 0,
    val remainingTasksCount: Int = 0
)

class MelloViewModel(
    private val repository: MelloRepository?,
    private val preferencesRepository: MelloPreferencesRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val isDemoMode: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MelloUiState(
            isDemoMode = isDemoMode,
            userId = currentUserId,
            userDisplayName = userDisplayName.ifBlank { "Mello Friend" },
            userEmail = userEmail,
            isLoading = !isDemoMode
        )
    )
    val uiState: StateFlow<MelloUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<MelloUserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = MelloUserPreferences()
        )

    private var dataJob: Job? = null
    private var deriveJob: Job? = null

    init {
        if (isDemoMode || repository == null) {
            seedDemoModeData()
        } else {
            startCloudObservation(repository, currentUserId)
        }
    }

    private data class CloudSnapshotBundle(
        val tasks: List<Task>,
        val habits: List<Habit>,
        val logs: List<HabitLog>,
        val insights: HabitInsights,
        val activeHabits: List<Habit>,
        val logsByHabitId: Map<String, List<HabitLog>>,
        val completedDatesByHabitId: Map<String, Set<String>>,
        val habitLogDates: Set<LocalDate>
    )

    private fun startCloudObservation(repo: MelloRepository, uid: String) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            launch(Dispatchers.IO) {
                try {
                    repo.seedStarterHabitsIfEmpty()
                } catch (e: Exception) {
                    Log.w(TAG, "Seed starter habits skipped: ${e.message}")
                }
            }

            combine(
                repo.observeTasks(uid),
                repo.observeHabits(uid),
                repo.observeHabitLogs(uid)
            ) { tasks, habits, logs ->
                val activeHabits = habits.filter { it.isActive }
                val logsByHabitId = logs.groupBy { it.habitId }
                val completedDatesByHabitId = logsByHabitId.mapValues { (_, list) ->
                    list.mapTo(HashSet(list.size)) { it.completedOn }
                }
                val habitLogDates = HashSet<LocalDate>(logs.size)
                val parsedLogsByHabit = HashMap<String, MutableSet<LocalDate>>(activeHabits.size)
                for (log in logs) {
                    val parsed = parseIsoDateFast(log.completedOn)
                    if (parsed != null) {
                        habitLogDates.add(parsed)
                        parsedLogsByHabit.getOrPut(log.habitId) { HashSet() }.add(parsed)
                    }
                }
                val insights = calculateHabitInsightsFromParsed(activeHabits, parsedLogsByHabit)
                CloudSnapshotBundle(
                    tasks = tasks,
                    habits = habits,
                    logs = logs,
                    insights = insights,
                    activeHabits = activeHabits,
                    logsByHabitId = logsByHabitId,
                    completedDatesByHabitId = completedDatesByHabitId,
                    habitLogDates = habitLogDates
                )
            }
                .flowOn(Dispatchers.Default)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing Mello cloud streams", rootError)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = rootError.localizedMessage ?: "Unable to sync cloud data"
                        )
                    }
                }
                .collect { bundle ->
                    _uiState.update { current ->
                        recomputeFullDerivedState(
                            base = current.copy(
                                isLoading = false,
                                errorMessage = null,
                                tasks = bundle.tasks,
                                habits = bundle.habits,
                                habitLogs = bundle.logs,
                                habitInsights = bundle.insights,
                                activeHabits = bundle.activeHabits,
                                logsByHabitId = bundle.logsByHabitId,
                                completedDatesByHabitId = bundle.completedDatesByHabitId,
                                habitLogDates = bundle.habitLogDates
                            ),
                            recalculateHabits = false
                        )
                    }
                }
        }
    }

    private fun seedDemoModeData() {
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val tomorrowEpoch = today.plusDays(1).toEpochDay()
        val yesterdayEpoch = today.minusDays(1).toEpochDay()
        val nowTs = Timestamp.now()

        val demoTasks = listOf(
            Task(
                id = "demo_t1",
                userId = "demo_user",
                title = "Review UI layout & typography tokens",
                description = "Ensure soft lavender spacing and rounded 24dp cards match the design system",
                category = "Work",
                priority = "High",
                dueDateEpochDay = todayEpoch,
                dueTime = "10:30",
                hasReminder = true,
                isCompleted = true,
                completedAtEpochMillis = System.currentTimeMillis() - 3600_000L,
                createdAt = nowTs,
                updatedAt = nowTs
            ),
            Task(
                id = "demo_t2",
                userId = "demo_user",
                title = "Hydration & matcha break",
                description = "Step away from screens for 15 minutes",
                category = "Health",
                priority = "Low",
                dueDateEpochDay = todayEpoch,
                dueTime = "14:00",
                hasReminder = false,
                isCompleted = false,
                completedAtEpochMillis = 0L,
                createdAt = nowTs,
                updatedAt = nowTs
            ),
            Task(
                id = "demo_t3",
                userId = "demo_user",
                title = "Complete Chapter 5 flashcards",
                description = "Focus on key definitions and spaced repetition deck",
                category = "Study",
                priority = "High",
                dueDateEpochDay = todayEpoch,
                dueTime = "16:30",
                hasReminder = true,
                isCompleted = false,
                completedAtEpochMillis = 0L,
                createdAt = nowTs,
                updatedAt = nowTs
            ),
            Task(
                id = "demo_t4",
                userId = "demo_user",
                title = "Pick up oat milk & fresh berries",
                description = "Local market run before dinner",
                category = "Other",
                priority = "Medium",
                dueDateEpochDay = tomorrowEpoch,
                dueTime = "18:00",
                hasReminder = false,
                isCompleted = false,
                completedAtEpochMillis = 0L,
                createdAt = nowTs,
                updatedAt = nowTs
            ),
            Task(
                id = "demo_t5",
                userId = "demo_user",
                title = "Organize digital photo album",
                description = "Sort weekend snapshots into folders",
                category = "Personal",
                priority = "Low",
                dueDateEpochDay = yesterdayEpoch,
                dueTime = "19:00",
                hasReminder = false,
                isCompleted = false,
                completedAtEpochMillis = 0L,
                createdAt = nowTs,
                updatedAt = nowTs
            )
        )

        val demoHabits = MelloRepository.defaultStarterHabits("demo_user")
        val fmt = MelloDateFormatters.ISO_DATE
        val demoLogs = mutableListOf<HabitLog>()
        for (offset in 0..5) {
            val dateStr = today.minusDays(offset.toLong()).format(fmt)
            if (offset <= 3) {
                demoLogs.add(
                    HabitLog(
                        id = "starter_water_$dateStr",
                        userId = "demo_user",
                        habitId = "starter_water",
                        completedOn = dateStr,
                        createdAt = nowTs
                    )
                )
            }
            if (offset in listOf(0, 1, 2, 4)) {
                demoLogs.add(
                    HabitLog(
                        id = "starter_reading_$dateStr",
                        userId = "demo_user",
                        habitId = "starter_reading",
                        completedOn = dateStr,
                        createdAt = nowTs
                    )
                )
            }
            if (offset in listOf(0, 1)) {
                demoLogs.add(
                    HabitLog(
                        id = "starter_meditation_$dateStr",
                        userId = "demo_user",
                        habitId = "starter_meditation",
                        completedOn = dateStr,
                        createdAt = nowTs
                    )
                )
            }
        }

        _uiState.update { current ->
            recomputeFullDerivedState(
                base = current.copy(
                    isLoading = false,
                    tasks = demoTasks,
                    habits = demoHabits,
                    habitLogs = demoLogs
                ),
                recalculateHabits = true
            )
        }
    }

    private fun recomputeAsync(
        recalculateHabits: Boolean = false,
        transform: (MelloUiState) -> MelloUiState
    ) {
        val intermediate = transform(_uiState.value)
        // Update primary state immediately so inputs/toggles feel instantaneous
        _uiState.value = recomputeLightweightDateAndFilterState(intermediate)
        deriveJob?.cancel()
        deriveJob = viewModelScope.launch(Dispatchers.Default) {
            val fullComputed = recomputeFullDerivedState(
                base = _uiState.value,
                recalculateHabits = recalculateHabits
            )
            _uiState.value = fullComputed
        }
    }

    // --- DATE & CALENDAR ACTIONS ---

    fun selectDate(date: LocalDate) {
        val targetEpoch = date.toEpochDay()
        if (_uiState.value.selectedDateEpochDay == targetEpoch) return
        _uiState.update { current ->
            recomputeLightweightDateAndFilterState(
                current.copy(
                    selectedDateEpochDay = targetEpoch,
                    currentCalendarMonth = YearMonth.from(date)
                )
            )
        }
    }

    fun navigateCalendarMonth(deltaMonths: Long) {
        _uiState.update {
            it.copy(currentCalendarMonth = it.currentCalendarMonth.plusMonths(deltaMonths))
        }
    }

    // --- TASK FILTER & SORT ACTIONS ---

    fun setTaskFilterTab(tab: TaskFilterTab) {
        if (_uiState.value.taskFilterTab == tab) return
        _uiState.update { current ->
            val updated = current.copy(taskFilterTab = tab)
            updated.copy(filteredTasks = filterAndSortTasks(updated))
        }
    }

    fun setSearchQuery(query: String) {
        if (_uiState.value.searchQuery == query) return
        _uiState.update { current ->
            val updated = current.copy(searchQuery = query)
            updated.copy(filteredTasks = filterAndSortTasks(updated))
        }
    }

    fun setCategoryFilter(category: String?) {
        _uiState.update { current ->
            val nextCat = if (current.selectedCategoryFilter == category) null else category
            val updated = current.copy(selectedCategoryFilter = nextCat)
            updated.copy(filteredTasks = filterAndSortTasks(updated))
        }
    }

    fun setPriorityFilter(priority: String?) {
        _uiState.update { current ->
            val nextPrio = if (current.selectedPriorityFilter == priority) null else priority
            val updated = current.copy(selectedPriorityFilter = nextPrio)
            updated.copy(filteredTasks = filterAndSortTasks(updated))
        }
    }

    fun setSortOption(option: TaskSortOption) {
        if (_uiState.value.sortOption == option) return
        _uiState.update { current ->
            val updated = current.copy(sortOption = option)
            updated.copy(filteredTasks = filterAndSortTasks(updated))
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // --- TASK CRUD (WITH INSTANT OPTIMISTIC UPDATES) ---

    fun saveTask(
        context: Context,
        existingId: String?,
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDateEpochDay: Long,
        dueTime: String,
        hasReminder: Boolean
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Please enter a task title") }
            return
        }

        val previousTasks = _uiState.value.tasks
        val nowTs = Timestamp.now()
        val isCreating = existingId.isNullOrBlank()
        val optimisticId = if (isCreating) "temp_${UUID.randomUUID().toString().take(8)}" else existingId!!

        val optimisticTask = if (isCreating) {
            Task(
                id = if (isDemoMode) "demo_${UUID.randomUUID().toString().take(8)}" else optimisticId,
                userId = if (isDemoMode) "demo_user" else currentUserId,
                title = cleanTitle,
                description = description.trim(),
                category = category,
                priority = priority,
                dueDateEpochDay = dueDateEpochDay,
                dueTime = dueTime,
                hasReminder = hasReminder,
                isCompleted = false,
                completedAtEpochMillis = 0L,
                createdAt = nowTs,
                updatedAt = nowTs
            )
        } else {
            val existing = previousTasks.firstOrNull { it.id == existingId }
            (existing ?: Task(id = existingId, userId = currentUserId)).copy(
                title = cleanTitle,
                description = description.trim(),
                category = category,
                priority = priority,
                dueDateEpochDay = dueDateEpochDay,
                dueTime = dueTime,
                hasReminder = hasReminder,
                updatedAt = nowTs
            )
        }

        recomputeAsync(recalculateHabits = false) { state ->
            val updatedList = if (isCreating) {
                listOf(optimisticTask) + state.tasks
            } else {
                state.tasks.map { if (it.id == existingId) optimisticTask else it }
            }
            state.copy(
                tasks = updatedList,
                snackbarMessage = if (isCreating) {
                    if (isDemoMode) "Task added" else "Task saved to cloud"
                } else {
                    "Task updated"
                }
            )
        }

        if (isDemoMode || repository == null) {
            if (isCreating && hasReminder && preferences.value.notificationsEnabled) {
                ReminderNotificationHelper.sendTaskReminderNotification(
                    context,
                    optimisticTask.id,
                    optimisticTask.title,
                    optimisticTask.dueTime,
                    optimisticTask.category
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                if (isCreating) {
                    val createdId = repository.createTask(optimisticTask.copy(id = ""))
                    if (hasReminder && preferences.value.notificationsEnabled) {
                        ReminderNotificationHelper.sendTaskReminderNotification(
                            context,
                            createdId,
                            cleanTitle,
                            dueTime,
                            category
                        )
                    }
                } else {
                    repository.updateTask(optimisticTask)
                }
            } catch (e: Exception) {
                recomputeAsync(recalculateHabits = false) { state ->
                    state.copy(
                        tasks = previousTasks,
                        errorMessage = e.localizedMessage ?: "Failed to save task"
                    )
                }
            }
        }
    }

    fun toggleTaskCompleted(task: Task) {
        val targetCompleted = !task.isCompleted
        val nowMillis = if (targetCompleted) System.currentTimeMillis() else 0L
        val previousTasks = _uiState.value.tasks

        // Instant optimistic update on the UI state
        recomputeAsync(recalculateHabits = false) { state ->
            state.copy(
                tasks = state.tasks.map {
                    if (it.id == task.id) {
                        it.copy(
                            isCompleted = targetCompleted,
                            completedAtEpochMillis = nowMillis,
                            updatedAt = Timestamp.now()
                        )
                    } else it
                }
            )
        }

        if (isDemoMode || repository == null) return

        viewModelScope.launch {
            try {
                repository.toggleTaskCompletion(task.id, targetCompleted, nowMillis)
            } catch (e: Exception) {
                recomputeAsync(recalculateHabits = false) { state ->
                    state.copy(
                        tasks = previousTasks,
                        errorMessage = e.localizedMessage ?: "Failed to update task status"
                    )
                }
            }
        }
    }

    fun deleteTask(taskId: String) {
        val previousTasks = _uiState.value.tasks
        recomputeAsync(recalculateHabits = false) { state ->
            state.copy(
                tasks = state.tasks.filterNot { it.id == taskId },
                snackbarMessage = "Task deleted"
            )
        }

        if (isDemoMode || repository == null) return

        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
            } catch (e: Exception) {
                recomputeAsync(recalculateHabits = false) { state ->
                    state.copy(
                        tasks = previousTasks,
                        errorMessage = e.localizedMessage ?: "Failed to delete task"
                    )
                }
            }
        }
    }

    // --- HABIT CRUD & LOGGING (WITH INSTANT OPTIMISTIC UPDATES) ---

    fun saveHabit(
        existingId: String?,
        title: String,
        description: String,
        color: String,
        iconName: String,
        isActive: Boolean
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Please enter a habit title") }
            return
        }

        val previousHabits = _uiState.value.habits
        val nowTs = Timestamp.now()
        val isCreating = existingId.isNullOrBlank()
        val optimisticHabit = Habit(
            id = if (isCreating) "habit_${UUID.randomUUID().toString().take(8)}" else existingId!!,
            userId = if (isDemoMode) "demo_user" else currentUserId,
            title = cleanTitle,
            description = description.trim(),
            color = color,
            iconName = iconName,
            isActive = isActive,
            createdAt = nowTs,
            updatedAt = nowTs
        )

        recomputeAsync(recalculateHabits = true) { state ->
            val updatedHabits = if (isCreating) {
                state.habits + optimisticHabit
            } else {
                state.habits.map { h -> if (h.id == existingId) optimisticHabit else h }
            }
            state.copy(
                habits = updatedHabits,
                snackbarMessage = if (isCreating) "Habit created" else "Habit updated"
            )
        }

        if (isDemoMode || repository == null) return

        viewModelScope.launch {
            try {
                if (isCreating) {
                    repository.createHabit(optimisticHabit.copy(id = ""))
                } else {
                    repository.updateHabit(optimisticHabit)
                }
            } catch (e: Exception) {
                recomputeAsync(recalculateHabits = true) { state ->
                    state.copy(
                        habits = previousHabits,
                        errorMessage = e.localizedMessage ?: "Failed to save habit"
                    )
                }
            }
        }
    }

    fun deleteHabit(habitId: String) {
        val previousHabits = _uiState.value.habits
        val previousLogs = _uiState.value.habitLogs

        recomputeAsync(recalculateHabits = true) { state ->
            state.copy(
                habits = state.habits.filterNot { it.id == habitId },
                habitLogs = state.habitLogs.filterNot { it.habitId == habitId },
                snackbarMessage = "Habit deleted"
            )
        }

        if (isDemoMode || repository == null) return

        viewModelScope.launch {
            try {
                repository.deleteHabit(habitId)
            } catch (e: Exception) {
                recomputeAsync(recalculateHabits = true) { state ->
                    state.copy(
                        habits = previousHabits,
                        habitLogs = previousLogs,
                        errorMessage = e.localizedMessage ?: "Failed to delete habit"
                    )
                }
            }
        }
    }

    fun toggleHabitOnDate(habitId: String, date: LocalDate) {
        val dateIso = date.format(MelloDateFormatters.ISO_DATE)
        val logId = "${habitId}_${dateIso}"
        val previousLogs = _uiState.value.habitLogs
        val currentlyCompleted = _uiState.value.completedDatesByHabitId[habitId]?.contains(dateIso) == true ||
            previousLogs.any { it.habitId == habitId && it.completedOn == dateIso }
        val targetCompleted = !currentlyCompleted

        // Instant optimistic update on the UI state
        recomputeAsync(recalculateHabits = true) { state ->
            val updatedLogs = if (targetCompleted) {
                state.habitLogs + HabitLog(
                    id = logId,
                    userId = if (isDemoMode) "demo_user" else currentUserId,
                    habitId = habitId,
                    completedOn = dateIso,
                    createdAt = Timestamp.now()
                )
            } else {
                state.habitLogs.filterNot {
                    it.id == logId || (it.habitId == habitId && it.completedOn == dateIso)
                }
            }
            state.copy(habitLogs = updatedLogs)
        }

        if (isDemoMode || repository == null) return

        viewModelScope.launch {
            try {
                repository.setHabitCompletedOnDate(habitId, dateIso, targetCompleted)
            } catch (e: Exception) {
                recomputeAsync(recalculateHabits = true) { state ->
                    state.copy(
                        habitLogs = previousLogs,
                        errorMessage = e.localizedMessage ?: "Failed to update habit log"
                    )
                }
            }
        }
    }

    // --- PREFERENCES & ACCOUNT ---

    fun selectThemePreset(preset: ThemePreset) {
        viewModelScope.launch {
            preferencesRepository.setThemePreset(preset)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReducedMotion(enabled)
        }
    }

    fun updateCustomDisplayName(name: String) {
        viewModelScope.launch {
            preferencesRepository.setCustomDisplayName(name)
            _uiState.update { it.copy(snackbarMessage = "Display name updated") }
        }
    }

    fun addCustomCategory(category: String) {
        viewModelScope.launch {
            preferencesRepository.addCustomCategory(category)
        }
    }

    fun deleteAccountDataAndReset(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                if (!isDemoMode && repository != null) {
                    withContext(Dispatchers.IO) {
                        repository.deleteAllUserData()
                    }
                }
                onComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to delete cloud data")
                }
            }
        }
    }

    // --- COMPUTED HELPERS ---

    companion object {
        private const val TAG = "MelloViewModel"

        private fun parseIsoDateFast(iso: String): LocalDate? {
            if (iso.length != 10 || iso[4] != '-' || iso[7] != '-') {
                return runCatching { LocalDate.parse(iso, MelloDateFormatters.ISO_DATE) }.getOrNull()
            }
            return try {
                val year = iso.substring(0, 4).toInt()
                val month = iso.substring(5, 7).toInt()
                val day = iso.substring(8, 10).toInt()
                LocalDate.of(year, month, day)
            } catch (_: Exception) {
                null
            }
        }

        internal fun recomputeLightweightDateAndFilterState(state: MelloUiState): MelloUiState {
            val todayEpoch = LocalDate.now().toEpochDay()
            val selectedDate = LocalDate.ofEpochDay(state.selectedDateEpochDay)
            val selectedDateIso = selectedDate.format(MelloDateFormatters.ISO_DATE)

            val selectedDayTasks = state.tasks.filter { task ->
                task.dueDateEpochDay == state.selectedDateEpochDay ||
                    (state.selectedDateEpochDay == todayEpoch && task.dueDateEpochDay == 0L)
            }
            val completedDayCount = selectedDayTasks.count { it.isCompleted }
            val upcomingTasks = state.tasks
                .asSequence()
                .filter { it.dueDateEpochDay > state.selectedDateEpochDay && !it.isCompleted }
                .sortedBy { it.dueDateEpochDay }
                .take(3)
                .toList()

            val completedHabitIds = state.activeHabits
                .asSequence()
                .filter { h -> state.completedDatesByHabitId[h.id]?.contains(selectedDateIso) == true }
                .mapTo(HashSet()) { it.id }

            return state.copy(
                filteredTasks = filterAndSortTasks(state),
                selectedDayTasks = selectedDayTasks,
                completedDayTasksCount = completedDayCount,
                upcomingTasks = upcomingTasks,
                completedHabitIdsForSelectedDate = completedHabitIds
            )
        }

        internal fun recomputeFullDerivedState(
            base: MelloUiState,
            recalculateHabits: Boolean
        ): MelloUiState {
            val todayEpoch = LocalDate.now().toEpochDay()
            val selectedDate = LocalDate.ofEpochDay(base.selectedDateEpochDay)
            val selectedDateIso = selectedDate.format(MelloDateFormatters.ISO_DATE)

            val activeHabits = if (recalculateHabits) {
                base.habits.filter { it.isActive }
            } else {
                base.activeHabits
            }

            val logsByHabitId = if (recalculateHabits) {
                base.habitLogs.groupBy { it.habitId }
            } else {
                base.logsByHabitId
            }

            val completedDatesByHabitId = if (recalculateHabits) {
                logsByHabitId.mapValues { (_, list) ->
                    list.mapTo(HashSet(list.size)) { it.completedOn }
                }
            } else {
                base.completedDatesByHabitId
            }

            val habitLogDates: Set<LocalDate>
            val insights: HabitInsights
            if (recalculateHabits) {
                val allLogDates = HashSet<LocalDate>(base.habitLogs.size)
                val parsedByHabit = HashMap<String, MutableSet<LocalDate>>(activeHabits.size)
                for (log in base.habitLogs) {
                    val parsed = parseIsoDateFast(log.completedOn)
                    if (parsed != null) {
                        allLogDates.add(parsed)
                        parsedByHabit.getOrPut(log.habitId) { HashSet() }.add(parsed)
                    }
                }
                habitLogDates = allLogDates
                insights = calculateHabitInsightsFromParsed(activeHabits, parsedByHabit)
            } else {
                habitLogDates = base.habitLogDates
                insights = base.habitInsights
            }

            val completedTaskDates = HashSet<LocalDate>()
            val scheduledTaskDates = HashSet<LocalDate>()
            var completedTotal = 0
            for (t in base.tasks) {
                if (t.isCompleted) {
                    completedTotal++
                    if (t.dueDateEpochDay > 0L) {
                        completedTaskDates.add(LocalDate.ofEpochDay(t.dueDateEpochDay))
                    }
                } else if (t.dueDateEpochDay > 0L) {
                    scheduledTaskDates.add(LocalDate.ofEpochDay(t.dueDateEpochDay))
                }
            }

            val activityDates = HashSet<LocalDate>(completedTaskDates.size + habitLogDates.size).apply {
                addAll(completedTaskDates)
                addAll(habitLogDates)
            }

            val selectedDayTasks = base.tasks.filter { task ->
                task.dueDateEpochDay == base.selectedDateEpochDay ||
                    (base.selectedDateEpochDay == todayEpoch && task.dueDateEpochDay == 0L)
            }
            val completedDayCount = selectedDayTasks.count { it.isCompleted }

            val upcomingTasks = base.tasks
                .asSequence()
                .filter { it.dueDateEpochDay > base.selectedDateEpochDay && !it.isCompleted }
                .sortedBy { it.dueDateEpochDay }
                .take(3)
                .toList()

            val completedHabitIds = activeHabits
                .asSequence()
                .filter { h -> completedDatesByHabitId[h.id]?.contains(selectedDateIso) == true }
                .mapTo(HashSet()) { it.id }

            return base.copy(
                filteredTasks = filterAndSortTasks(base),
                habitInsights = insights,
                activeHabits = activeHabits,
                logsByHabitId = logsByHabitId,
                completedDatesByHabitId = completedDatesByHabitId,
                habitLogDates = habitLogDates,
                completedTaskDates = completedTaskDates,
                scheduledTaskDates = scheduledTaskDates,
                activityDates = activityDates,
                completedHabitIdsForSelectedDate = completedHabitIds,
                selectedDayTasks = selectedDayTasks,
                completedDayTasksCount = completedDayCount,
                upcomingTasks = upcomingTasks,
                completedTasksCount = completedTotal,
                remainingTasksCount = (base.tasks.size - completedTotal).coerceAtLeast(0)
            )
        }

        fun filterAndSortTasks(state: MelloUiState): List<Task> {
            val todayEpoch = LocalDate.now().toEpochDay()
            val query = state.searchQuery.trim().lowercase()

            return state.tasks
                .filter { task ->
                    val matchesTab = when (state.taskFilterTab) {
                        TaskFilterTab.TODAY -> task.dueDateEpochDay == todayEpoch || (task.dueDateEpochDay == 0L && !task.isCompleted)
                        TaskFilterTab.UPCOMING -> task.dueDateEpochDay > todayEpoch && !task.isCompleted
                        TaskFilterTab.ALL -> true
                        TaskFilterTab.COMPLETED -> task.isCompleted
                        TaskFilterTab.OVERDUE -> task.dueDateEpochDay in 1 until todayEpoch && !task.isCompleted
                    }
                    val matchesQuery = query.isEmpty() ||
                        task.title.lowercase().contains(query) ||
                        task.description.lowercase().contains(query) ||
                        task.category.lowercase().contains(query)
                    val matchesCategory = state.selectedCategoryFilter == null ||
                        task.category.equals(state.selectedCategoryFilter, ignoreCase = true)
                    val matchesPriority = state.selectedPriorityFilter == null ||
                        task.priority.equals(state.selectedPriorityFilter, ignoreCase = true)

                    matchesTab && matchesQuery && matchesCategory && matchesPriority
                }
                .sortedWith { a, b ->
                    // Incomplete first, then by selected sort option
                    if (a.isCompleted != b.isCompleted) {
                        if (a.isCompleted) 1 else -1
                    } else {
                        when (state.sortOption) {
                            TaskSortOption.DUE_DATE -> {
                                val aDue = if (a.dueDateEpochDay == 0L) Long.MAX_VALUE else a.dueDateEpochDay
                                val bDue = if (b.dueDateEpochDay == 0L) Long.MAX_VALUE else b.dueDateEpochDay
                                val cmp = aDue.compareTo(bDue)
                                if (cmp != 0) cmp else priorityRank(b.priority).compareTo(priorityRank(a.priority))
                            }
                            TaskSortOption.CREATED_DATE -> {
                                val aCreated = a.createdAt?.seconds ?: 0L
                                val bCreated = b.createdAt?.seconds ?: 0L
                                bCreated.compareTo(aCreated)
                            }
                        }
                    }
                }
        }

        private fun priorityRank(priority: String): Int = when (TaskPriority.fromLabel(priority)) {
            TaskPriority.HIGH -> 3
            TaskPriority.MEDIUM -> 2
            TaskPriority.LOW -> 1
        }

        fun calculateHabitInsights(habits: List<Habit>, logs: List<HabitLog>): HabitInsights {
            val activeHabits = habits.filter { it.isActive }
            if (activeHabits.isEmpty()) return HabitInsights()

            val parsedByHabit = HashMap<String, MutableSet<LocalDate>>(activeHabits.size)
            for (log in logs) {
                val parsed = parseIsoDateFast(log.completedOn)
                if (parsed != null) {
                    parsedByHabit.getOrPut(log.habitId) { HashSet() }.add(parsed)
                }
            }
            return calculateHabitInsightsFromParsed(activeHabits, parsedByHabit)
        }

        private fun calculateHabitInsightsFromParsed(
            activeHabits: List<Habit>,
            logsByHabit: Map<String, Set<LocalDate>>
        ): HabitInsights {
            if (activeHabits.isEmpty()) return HabitInsights()

            val today = LocalDate.now()
            val habitStreaks = HashMap<String, Pair<Int, Int>>(activeHabits.size)
            var maxCurrentStreak = 0
            var maxLongestStreak = 0

            for (habit in activeHabits) {
                val dates = logsByHabit[habit.id] ?: emptySet()
                val current = computeCurrentStreak(dates, today)
                val longest = computeLongestStreak(dates)
                habitStreaks[habit.id] = current to longest
                if (current > maxCurrentStreak) maxCurrentStreak = current
                if (longest > maxLongestStreak) maxLongestStreak = longest
            }

            // Weekly completion over last 7 days
            val last7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val weeklyBars = last7Days.map { date ->
                val completedCount = activeHabits.count { h ->
                    logsByHabit[h.id]?.contains(date) == true
                }
                val ratio = (completedCount.toFloat() / activeHabits.size.toFloat()).coerceIn(0f, 1f)
                val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)
                label to ratio
            }

            val avgCompletion = ((weeklyBars.map { it.second }.average() * 100.0).toInt()).coerceIn(0, 100)

            return HabitInsights(
                averageCompletionPercent = avgCompletion,
                currentOverallStreak = maxCurrentStreak,
                longestOverallStreak = maxLongestStreak,
                weeklyCompletionByDay = weeklyBars,
                habitStreaks = habitStreaks
            )
        }

        private fun computeCurrentStreak(dates: Set<LocalDate>, today: LocalDate): Int {
            if (dates.isEmpty()) return 0
            var cursor = if (dates.contains(today)) today else today.minusDays(1)
            if (!dates.contains(cursor)) return 0
            var streak = 0
            while (dates.contains(cursor)) {
                streak++
                cursor = cursor.minusDays(1)
            }
            return streak
        }

        private fun computeLongestStreak(dates: Set<LocalDate>): Int {
            if (dates.isEmpty()) return 0
            val sorted = dates.sorted()
            var longest = 1
            var current = 1
            for (i in 1 until sorted.size) {
                val diff = sorted[i].toEpochDay() - sorted[i - 1].toEpochDay()
                if (diff == 1L) {
                    current++
                    if (current > longest) longest = current
                } else if (diff != 0L) {
                    current = 1
                }
            }
            return longest
        }
    }
}
