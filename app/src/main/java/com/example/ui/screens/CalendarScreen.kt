package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.MelloDateFormatters
import com.example.data.model.Task
import com.example.ui.components.EmptyState
import com.example.ui.components.TaskCard
import com.example.ui.theme.MelloThemeColors
import com.example.ui.viewmodel.MelloUiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val WEEKDAY_HEADERS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun CalendarScreen(
    uiState: MelloUiState,
    onSelectDate: (LocalDate) -> Unit,
    onNavigateMonth: (Long) -> Unit,
    onBack: () -> Unit,
    onAddTaskForDate: (LocalDate) -> Unit,
    onToggleTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val palette = MelloThemeColors.palette
    val today = remember { LocalDate.now() }
    val selectedDate = remember(uiState.selectedDateEpochDay) {
        LocalDate.ofEpochDay(uiState.selectedDateEpochDay)
    }
    val selectedFullLabel = remember(selectedDate) {
        selectedDate.format(MelloDateFormatters.FULL_WEEKDAY_MONTH_DAY)
    }
    val selectedShortLabel = remember(selectedDate) {
        selectedDate.format(MelloDateFormatters.SHORT_MONTH_DAY)
    }

    // Consume precomputed date sets from MelloUiState
    val completedDates = uiState.activityDates
    val scheduledDates = uiState.scheduledTaskDates

    val selectedDayTasks = remember(uiState.tasks, uiState.selectedDateEpochDay) {
        uiState.tasks.filter { it.dueDateEpochDay == uiState.selectedDateEpochDay }
    }
    val (todayTasks, tomorrowTasks, weekTasks) = remember(uiState.tasks, today) {
        val todayEpoch = today.toEpochDay()
        val tomorrowEpoch = todayEpoch + 1L
        val weekEndEpoch = todayEpoch + 7L
        val tToday = mutableListOf<Task>()
        val tTomorrow = mutableListOf<Task>()
        val tWeek = mutableListOf<Task>()
        for (task in uiState.tasks) {
            val due = task.dueDateEpochDay
            when {
                due == todayEpoch -> tToday.add(task)
                due == tomorrowEpoch -> tTomorrow.add(task)
                due in (tomorrowEpoch + 1L)..weekEndEpoch -> tWeek.add(task)
            }
        }
        Triple(tToday, tTomorrow, tWeek)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Back button
        item(key = "calendar_header", contentType = "header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = palette.surface,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("calendar_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Calendar",
                            style = MaterialTheme.typography.headlineLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Monthly schedule & activity",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }

                Surface(
                    color = palette.primarySoft,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectDate(today) }
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Monthly Calendar Card
        item(key = "calendar_month_card", contentType = "month_card") {
            MonthlyCalendarCard(
                yearMonth = uiState.currentCalendarMonth,
                selectedDate = selectedDate,
                today = today,
                completedDates = completedDates,
                scheduledDates = scheduledDates,
                onSelectDate = onSelectDate,
                onPrevMonth = { onNavigateMonth(-1) },
                onNextMonth = { onNavigateMonth(1) }
            )
        }

        // Selected Day Section
        item(key = "calendar_selected_header", contentType = "section_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Selected: $selectedFullLabel",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                Text(
                    text = "${selectedDayTasks.size} tasks",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primary
                )
            }
        }

        if (selectedDayTasks.isEmpty()) {
            item(key = "calendar_empty_selected", contentType = "empty_state") {
                EmptyState(
                    title = "Free day on $selectedShortLabel",
                    subtitle = "No tasks scheduled for this date.",
                    actionLabel = "+ Schedule Task",
                    onAction = { onAddTaskForDate(selectedDate) }
                )
            }
        } else {
            items(
                items = selectedDayTasks,
                key = { "cal_sel_${it.id}" },
                contentType = { "task_card" }
            ) { task ->
                TaskCard(
                    task = task,
                    onToggleComplete = { onToggleTask(task) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task.id) }
                )
            }
        }

        // Today / Tomorrow / This Week Summary Sections
        if (selectedDate != today && todayTasks.isNotEmpty()) {
            item(key = "calendar_today_header", contentType = "section_header") {
                AgendaSectionTitle("Today", todayTasks.size)
            }
            items(
                items = todayTasks,
                key = { "cal_today_${it.id}" },
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

        if (tomorrowTasks.isNotEmpty()) {
            item(key = "calendar_tomorrow_header", contentType = "section_header") {
                AgendaSectionTitle("Tomorrow", tomorrowTasks.size)
            }
            items(
                items = tomorrowTasks,
                key = { "cal_tmw_${it.id}" },
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

        if (weekTasks.isNotEmpty()) {
            item(key = "calendar_week_header", contentType = "section_header") {
                AgendaSectionTitle("This Week", weekTasks.size)
            }
            items(
                items = weekTasks,
                key = { "cal_wk_${it.id}" },
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
private fun AgendaSectionTitle(title: String, count: Int) {
    val palette = MelloThemeColors.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = palette.surfaceMuted,
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun MonthlyCalendarCard(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    completedDates: Set<LocalDate>,
    scheduledDates: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val palette = MelloThemeColors.palette
    val (monthTitle, leadingEmptyDays, daysInMonth, totalWeeks) = remember(yearMonth) {
        val firstOfMonth = yearMonth.atDay(1)
        val leading = (firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val days = yearMonth.lengthOfMonth()
        val weeks = (leading + days + 6) / 7
        val title = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}"
        MonthGridGeometry(title, leading, days, weeks)
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Month navigation header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onPrevMonth,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("calendar_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = palette.textPrimary
                    )
                }

                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("calendar_next_month")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Next month",
                        tint = palette.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday header row
            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAY_HEADERS.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid rows
            for (weekIndex in 0 until totalWeeks) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayOfWeekIndex in 0..6) {
                        val cellIndex = weekIndex * 7 + dayOfWeekIndex
                        val dayNumber = cellIndex - leadingEmptyDays + 1
                        if (dayNumber in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayNumber)
                            val isSelected = date == selectedDate
                            val isToday = date == today
                            val hasCompleted = completedDates.contains(date)
                            val hasScheduled = scheduledDates.contains(date)

                            val cellBg = when {
                                isSelected -> palette.primary
                                isToday -> palette.primarySoft
                                else -> Color.Transparent
                            }
                            val textColor = when {
                                isSelected -> palette.onPrimary
                                isToday -> palette.primary
                                else -> palette.textPrimary
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cellBg)
                                    .then(
                                        if (isToday && !isSelected) {
                                            Modifier.border(1.dp, palette.primary, RoundedCornerShape(14.dp))
                                        } else Modifier
                                    )
                                    .clickable { onSelectDate(date) }
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    if (hasCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) palette.onPrimary else palette.success)
                                        )
                                    }
                                    if (hasScheduled) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) palette.onPrimary.copy(alpha = 0.7f) else palette.primary)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class MonthGridGeometry(
    val monthTitle: String,
    val leadingEmptyDays: Int,
    val daysInMonth: Int,
    val totalWeeks: Int
)
