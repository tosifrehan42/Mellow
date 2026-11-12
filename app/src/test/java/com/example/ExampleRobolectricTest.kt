package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Habit
import com.example.data.model.HabitLog
import com.example.data.model.MelloDateFormatters
import com.example.data.model.Task
import com.example.data.model.TaskFilterTab
import com.example.data.preferences.MelloPreferencesRepository
import com.example.ui.viewmodel.MelloUiState
import com.example.ui.viewmodel.MelloViewModel
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Mello", appName)
    }

    @Test
    fun `viewModel demo mode initializes precomputed tasks habits and streaks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = MelloPreferencesRepository(context)
        val vm = MelloViewModel(
            repository = null,
            preferencesRepository = prefs,
            currentUserId = "demo_user",
            userDisplayName = "Mello Tester",
            userEmail = "tester@mello.app",
            isDemoMode = true
        )

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.tasks.isNotEmpty())
        assertTrue(state.activeHabits.isNotEmpty())
        assertTrue(state.filteredTasks.isNotEmpty())
        assertTrue(state.habitInsights.currentOverallStreak >= 1)
    }

    @Test
    fun `task creation completion filtering and calendar navigation update derived state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = MelloPreferencesRepository(context)
        val vm = MelloViewModel(
            repository = null,
            preferencesRepository = prefs,
            currentUserId = "demo_user",
            userDisplayName = "Mello Tester",
            userEmail = "tester@mello.app",
            isDemoMode = true
        )

        val initialCount = vm.uiState.value.tasks.size
        vm.saveTask(
            context = context,
            existingId = null,
            title = "Optimize Compose stability",
            description = "Verify @Immutable models",
            category = "Work",
            priority = "High",
            dueDateEpochDay = LocalDate.now().toEpochDay(),
            dueTime = "11:00",
            hasReminder = false
        )

        val afterAdd = vm.uiState.value
        assertEquals(initialCount + 1, afterAdd.tasks.size)
        val addedTask = afterAdd.tasks.first { it.title == "Optimize Compose stability" }
        assertFalse(addedTask.isCompleted)

        // Toggle task completion optimistically
        vm.toggleTaskCompleted(addedTask)
        val toggled = vm.uiState.value.tasks.first { it.id == addedTask.id }
        assertTrue(toggled.isCompleted)

        // Filter by Completed tab
        vm.setTaskFilterTab(TaskFilterTab.COMPLETED)
        assertTrue(vm.uiState.value.filteredTasks.all { it.isCompleted })

        // Calendar navigation
        val currentMonth = vm.uiState.value.currentCalendarMonth
        vm.navigateCalendarMonth(1)
        assertEquals(currentMonth.plusMonths(1), vm.uiState.value.currentCalendarMonth)

        val nextWeek = LocalDate.now().plusDays(7)
        vm.selectDate(nextWeek)
        assertEquals(nextWeek.toEpochDay(), vm.uiState.value.selectedDateEpochDay)
        assertEquals(YearMonth.from(nextWeek), vm.uiState.value.currentCalendarMonth)
    }

    @Test
    fun `habit streak calculation computes current and longest streaks accurately`() {
        val today = LocalDate.now()
        val fmt = MelloDateFormatters.ISO_DATE
        val habit = Habit(id = "h1", userId = "u1", title = "Read 20 minutes", isActive = true)
        val logs = listOf(
            HabitLog(id = "h1_0", userId = "u1", habitId = "h1", completedOn = today.format(fmt)),
            HabitLog(id = "h1_1", userId = "u1", habitId = "h1", completedOn = today.minusDays(1).format(fmt)),
            HabitLog(id = "h1_2", userId = "u1", habitId = "h1", completedOn = today.minusDays(2).format(fmt)),
            HabitLog(id = "h1_5", userId = "u1", habitId = "h1", completedOn = today.minusDays(5).format(fmt))
        )

        val insights = MelloViewModel.calculateHabitInsights(listOf(habit), logs)
        assertEquals(3, insights.currentOverallStreak)
        assertEquals(3, insights.longestOverallStreak)
        assertEquals(3 to 3, insights.habitStreaks["h1"])
    }
}
