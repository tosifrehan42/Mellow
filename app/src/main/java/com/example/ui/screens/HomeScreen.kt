package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Habit
import com.example.data.model.MelloDateFormatters
import com.example.data.model.Task
import com.example.ui.components.CompletionCheckbox
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.ProgressCard
import com.example.ui.components.StreakIndicator
import com.example.ui.components.TaskCard
import com.example.ui.components.WeeklyDateSelector
import com.example.ui.components.habitIconForName
import com.example.ui.theme.MelloThemeColors
import com.example.ui.theme.cardPastelPair
import com.example.ui.viewmodel.MelloUiState
import com.example.ui.viewmodel.MelloViewModel
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    uiState: MelloUiState,
    customDisplayName: String = "",
    onSelectDate: (LocalDate) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenTasksTab: () -> Unit,
    onOpenHabitsTab: () -> Unit,
    onOpenProfileTab: () -> Unit,
    onQuickAddTask: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    onToggleHabitToday: (String, LocalDate) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    val today = remember { LocalDate.now() }
    val selectedDate = remember(uiState.selectedDateEpochDay) {
        LocalDate.ofEpochDay(uiState.selectedDateEpochDay)
    }
    val headerDateLabel = remember(selectedDate) {
        selectedDate.format(MelloDateFormatters.FULL_WEEKDAY_MONTH_DAY)
    }
    val shortDateLabel = remember(selectedDate) {
        selectedDate.format(MelloDateFormatters.SHORT_MONTH_DAY)
    }
    val dateBadge = remember(selectedDate, today) {
        when (selectedDate) {
            today -> "Today's Flow"
            today.plusDays(1) -> "Tomorrow"
            else -> shortDateLabel
        }
    }

    val greetingPrefix = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val firstName = remember(customDisplayName, uiState.userDisplayName) {
        customDisplayName.ifBlank { uiState.userDisplayName }
            .substringBefore(" ")
            .ifBlank { "Friend" }
    }

    // Use precomputed slices from MelloViewModel (with memoized fallback if state was constructed manually in tests)
    val selectedDayTasks = if (uiState.selectedDayTasks.isNotEmpty() || uiState.tasks.isEmpty()) {
        uiState.selectedDayTasks
    } else {
        remember(uiState.tasks, uiState.selectedDateEpochDay, today) {
            uiState.tasks.filter { task ->
                task.dueDateEpochDay == uiState.selectedDateEpochDay ||
                    (selectedDate == today && task.dueDateEpochDay == 0L)
            }
        }
    }
    val completedDayTasks = if (uiState.selectedDayTasks.isNotEmpty() || uiState.tasks.isEmpty()) {
        uiState.completedDayTasksCount
    } else {
        remember(selectedDayTasks) { selectedDayTasks.count { it.isCompleted } }
    }

    val upcomingTasks = if (uiState.upcomingTasks.isNotEmpty() || uiState.tasks.isEmpty()) {
        uiState.upcomingTasks
    } else {
        remember(uiState.tasks, uiState.selectedDateEpochDay) {
            uiState.tasks
                .asSequence()
                .filter { it.dueDateEpochDay > uiState.selectedDateEpochDay && !it.isCompleted }
                .sortedBy { it.dueDateEpochDay }
                .take(3)
                .toList()
        }
    }

    val activityDates = uiState.activityDates
    val insights = if (uiState.habitInsights.habitStreaks.isNotEmpty() || uiState.habits.isEmpty()) {
        uiState.habitInsights
    } else {
        remember(uiState.habits, uiState.habitLogs) {
            MelloViewModel.calculateHabitInsights(uiState.habits, uiState.habitLogs)
        }
    }
    val activeHabits = if (uiState.activeHabits.isNotEmpty() || uiState.habits.isEmpty()) {
        uiState.activeHabits
    } else {
        remember(uiState.habits) { uiState.habits.filter { it.isActive } }
    }
    val completedHabitIds = uiState.completedHabitIdsForSelectedDate

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Header: Greeting, Date, Calendar Action & Profile Avatar
        item(key = "home_header", contentType = "header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = headerDateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primary
                        )
                        if (uiState.isDemoMode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = palette.pastelButter,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Demo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.pastelButterText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$greetingPrefix, $firstName ✨",
                        style = MaterialTheme.typography.headlineLarge,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Make every day feel a little more organized.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = palette.surface,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(
                            onClick = onOpenCalendar,
                            modifier = Modifier.testTag("open_calendar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = "Open Calendar",
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(palette.primarySoft)
                            .clickable { onOpenProfileTab() }
                            .testTag("home_avatar_button")
                    ) {
                        Text(
                            text = firstName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                    }
                }
            }
        }

        if (uiState.errorMessage != null) {
            item(key = "home_error", contentType = "error") {
                ErrorBanner(
                    message = uiState.errorMessage,
                    onDismiss = onDismissError
                )
            }
        }

        // 2. Weekly Horizontal Date Selector
        item(key = "home_week_selector", contentType = "week_selector") {
            WeeklyDateSelector(
                selectedDate = selectedDate,
                datesWithActivity = activityDates,
                onSelectDate = onSelectDate
            )
        }

        // 3. Bento Daily Productivity Progress Card
        item(key = "home_progress_card", contentType = "progress_card") {
            ProgressCard(
                completedCount = completedDayTasks,
                totalCount = selectedDayTasks.size,
                dateLabel = dateBadge
            )
        }

        // 4. Habit Progress Preview Strip
        if (activeHabits.isNotEmpty()) {
            item(key = "home_habits_strip", contentType = "habits_strip") {
                Column {
                    SectionHeaderRow(
                        title = "Daily Habits",
                        badgeText = "${completedHabitIds.size}/${activeHabits.size} done",
                        actionText = "See all",
                        onAction = onOpenHabitsTab
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = activeHabits,
                            key = { it.id },
                            contentType = { "habit_preview_card" }
                        ) { habit ->
                            val isDone = completedHabitIds.contains(habit.id)
                            val streak = insights.habitStreaks[habit.id]?.first ?: 0
                            HabitPreviewMiniCard(
                                habit = habit,
                                isCompleted = isDone,
                                streakDays = streak,
                                onToggle = { onToggleHabitToday(habit.id, selectedDate) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Today's / Selected Day's Task List Header + Quick Add
        item(key = "home_tasks_header", contentType = "section_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (selectedDate == today) "Today's Tasks" else "Tasks for $shortDateLabel",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = palette.primarySoft,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = selectedDayTasks.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onQuickAddTask() }
                        .testTag("home_quick_add_task")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Quick add task",
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Quick Add",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primary
                        )
                    }
                }
            }
        }

        if (selectedDayTasks.isEmpty()) {
            item(key = "home_empty_tasks", contentType = "empty_state") {
                EmptyState(
                    title = "No tasks scheduled",
                    subtitle = "Enjoy your calm space or add a small task for ${if (selectedDate == today) "today" else "this day"}.",
                    actionLabel = "+ Add Task",
                    onAction = onQuickAddTask
                )
            }
        } else {
            items(
                items = selectedDayTasks,
                key = { it.id },
                contentType = { "task_card" }
            ) { task ->
                TaskCard(
                    task = task,
                    onToggleComplete = { onToggleTask(task) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task.id) },
                    usePastelBackground = true
                )
            }
        }

        // 6. Upcoming Task Preview
        if (upcomingTasks.isNotEmpty()) {
            item(key = "home_upcoming_header", contentType = "section_header") {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeaderRow(
                    title = "Upcoming Next",
                    badgeText = "${upcomingTasks.size}",
                    actionText = "All tasks",
                    onAction = onOpenTasksTab
                )
            }

            items(
                items = upcomingTasks,
                key = { "upcoming_${it.id}" },
                contentType = { "task_card" }
            ) { task ->
                TaskCard(
                    task = task,
                    onToggleComplete = { onToggleTask(task) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task.id) },
                    usePastelBackground = false
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    badgeText: String,
    actionText: String,
    onAction: () -> Unit
) {
    val palette = MelloThemeColors.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = palette.surfaceMuted,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onAction() }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = palette.primary
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun HabitPreviewMiniCard(
    habit: Habit,
    isCompleted: Boolean,
    streakDays: Int,
    onToggle: () -> Unit
) {
    val palette = MelloThemeColors.palette
    val (bg, fg) = palette.cardPastelPair(habit.color)
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = bg.copy(alpha = 0.75f)),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .width(164.dp)
            .clickable { onToggle() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(palette.surface.copy(alpha = 0.9f))
                ) {
                    Icon(
                        imageVector = habitIconForName(habit.iconName),
                        contentDescription = habit.title,
                        tint = fg,
                        modifier = Modifier.size(18.dp)
                    )
                }

                CompletionCheckbox(
                    checked = isCompleted,
                    accentColor = fg,
                    onToggle = onToggle
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = habit.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            StreakIndicator(streakDays = streakDays)
        }
    }
}
