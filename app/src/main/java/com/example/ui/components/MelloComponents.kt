package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Habit
import com.example.data.model.HabitLog
import com.example.data.model.MelloDateFormatters
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.ui.theme.MelloThemeColors
import com.example.ui.theme.cardPastelPair
import com.example.ui.theme.priorityColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

enum class MelloNavTab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Outlined.Home),
    TASKS("tasks", "Tasks", Icons.Outlined.TaskAlt),
    HABITS("habits", "Habits", Icons.Outlined.Repeat),
    PROFILE("profile", "Profile", Icons.Outlined.PersonOutline)
}

@Composable
fun MelloBottomNavigation(
    currentTab: MelloNavTab,
    onSelectTab: (MelloNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    Surface(
        color = palette.surface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MelloNavTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val bg by animateColorAsState(
                    targetValue = if (isSelected) palette.primarySoft else Color.Transparent,
                    label = "navBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) palette.primary else palette.textSecondary,
                    label = "navContent"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(bg)
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("nav_tab_${tab.route}")
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyDateSelector(
    selectedDate: LocalDate,
    datesWithActivity: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    val today = remember { LocalDate.now() }
    val days = remember(selectedDate) {
        val startOfStrip = selectedDate.minusDays(3)
        (0..6).map { offset ->
            val d = startOfStrip.plusDays(offset.toLong())
            val shortDay = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)
            d to shortDay
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(
            items = days,
            key = { (date, _) -> date.toEpochDay() },
            contentType = { "week_day_pill" }
        ) { (date, shortDayName) ->
            val isSelected = date == selectedDate
            val isToday = date == today
            val hasActivity = datesWithActivity.contains(date)

            val containerColor by animateColorAsState(
                targetValue = when {
                    isSelected -> palette.primary
                    isToday -> palette.primarySoft
                    else -> palette.surface
                },
                label = "dateBg"
            )
            val textColor = when {
                isSelected -> palette.onPrimary
                isToday -> palette.primary
                else -> palette.textPrimary
            }
            val subTextColor = when {
                isSelected -> palette.onPrimary.copy(alpha = 0.85f)
                else -> palette.textSecondary
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(50.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(containerColor)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) palette.primary else palette.border,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelectDate(date) }
                    .padding(vertical = 10.dp)
                    .testTag("week_date_${date.dayOfMonth}")
            ) {
                Text(
                    text = shortDayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = subTextColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !hasActivity -> Color.Transparent
                                isSelected -> palette.onPrimary
                                else -> palette.primary
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun ProgressCard(
    completedCount: Int,
    totalCount: Int,
    dateLabel: String,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    val reducedMotion = MelloThemeColors.reducedMotion
    val remaining = (totalCount - completedCount).coerceAtLeast(0)
    val fraction = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = if (reducedMotion) tween(0) else tween(650),
        label = "dailyProgress"
    )
    val percentage = (fraction * 100).toInt()

    val statusHeadline = when {
        totalCount == 0 -> "Let's make today count. Make every day feel a little more organized."
        percentage == 100 -> "All tasks completed! You made today count ✨"
        percentage >= 60 -> "Almost there — let's make today count!"
        percentage > 0 -> "Make every day feel a little more organized."
        else -> "Let's make today count. Pick one task to begin."
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = palette.primarySoft),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_progress_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = palette.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(0.5.dp, palette.border)
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Daily Progress",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusHeadline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(label = "Total", value = totalCount.toString(), accent = palette.warning)
                    StatPill(label = "Done", value = completedCount.toString(), accent = palette.success)
                    StatPill(label = "Left", value = remaining.toString(), accent = palette.primary)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(88.dp)
            ) {
                Canvas(modifier = Modifier.size(88.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    drawArc(
                        color = palette.surface.copy(alpha = 0.7f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = palette.primary,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "$completedCount/$totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    accent: Color
) {
    val palette = MelloThemeColors.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface.copy(alpha = 0.85f))
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$value $label",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary
        )
    }
}

@Composable
fun CompletionCheckbox(
    checked: Boolean,
    accentColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    val reducedMotion = MelloThemeColors.reducedMotion
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.05f else 1f,
        animationSpec = if (reducedMotion) tween(0) else spring(dampingRatio = 0.55f),
        label = "checkScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (checked) accentColor else palette.surface,
        label = "checkBg"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(28.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = 2.dp,
                color = if (checked) accentColor else palette.border,
                shape = CircleShape
            )
            .clickable { onToggle() }
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PriorityBadge(
    priority: String,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    val (bg, fg) = palette.priorityColors(priority)
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = priority,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TaskCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    usePastelBackground: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    val colorKey = remember(task.category) { TaskCategory.colorKeyForCategory(task.category) }
    val (pastelBg, pastelAccent) = palette.cardPastelPair(colorKey)
    val cardContainer = when {
        task.isCompleted -> palette.surfaceMuted.copy(alpha = 0.7f)
        usePastelBackground -> pastelBg.copy(alpha = 0.65f)
        else -> palette.surface
    }

    val timeLabel = remember(task.dueDateEpochDay, task.dueTime) {
        if (task.dueDateEpochDay <= 0L && task.dueTime.isBlank()) {
            ""
        } else {
            val todayEpoch = LocalDate.now().toEpochDay()
            val datePart = when {
                task.dueDateEpochDay <= 0L -> ""
                task.dueDateEpochDay == todayEpoch -> "Today"
                task.dueDateEpochDay == todayEpoch + 1L -> "Tomorrow"
                else -> LocalDate.ofEpochDay(task.dueDateEpochDay).format(MelloDateFormatters.SHORT_MONTH_DAY)
            }
            if (datePart.isNotEmpty() && task.dueTime.isNotBlank()) {
                "$datePart • ${task.dueTime}"
            } else {
                datePart.ifEmpty { task.dueTime }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        border = BorderStroke(1.dp, if (task.isCompleted) palette.border.copy(alpha = 0.5f) else palette.border),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            CompletionCheckbox(
                checked = task.isCompleted,
                accentColor = palette.primary,
                onToggle = onToggleComplete,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .testTag("task_checkbox_${task.id}")
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (task.isCompleted) palette.textSecondary else palette.textPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PriorityBadge(priority = task.priority)
                }

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = palette.surface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(0.5.dp, palette.border)
                        ) {
                            Text(
                                text = task.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = pastelAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }

                        if (timeLabel.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = "Due time",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = timeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        if (task.hasReminder) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = "Reminder active",
                                tint = palette.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit task",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("delete_task_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete task",
                                tint = palette.danger.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreakIndicator(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    Surface(
        color = palette.pastelButter,
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.5.dp, palette.border),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Streak",
                tint = palette.pastelButterText,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${streakDays}d",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = palette.pastelButterText
            )
        }
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    selectedDate: LocalDate,
    logsForHabit: List<HabitLog>,
    currentStreak: Int,
    longestStreak: Int,
    onToggleDate: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    completedDateIsoSet: Set<String>? = null
) {
    val palette = MelloThemeColors.palette
    val (pastelBg, pastelFg) = palette.cardPastelPair(habit.color)
    val completedDates = completedDateIsoSet ?: remember(logsForHabit) {
        logsForHabit.mapTo(HashSet(logsForHabit.size)) { it.completedOn }
    }
    val selectedDateIso = remember(selectedDate) {
        selectedDate.format(MelloDateFormatters.ISO_DATE)
    }
    val isDoneOnSelected = completedDates.contains(selectedDateIso)

    val weekDaysWithMeta = remember(selectedDate) {
        (6 downTo 0).map { offset ->
            val d = selectedDate.minusDays(offset.toLong())
            val iso = d.format(MelloDateFormatters.ISO_DATE)
            val narrowLabel = d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            Triple(d, iso, narrowLabel)
        }
    }
    val completedInWeek = remember(weekDaysWithMeta, completedDates) {
        weekDaysWithMeta.count { (_, iso, _) -> completedDates.contains(iso) }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = pastelBg.copy(alpha = 0.72f)),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("habit_card_${habit.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.surface.copy(alpha = 0.9f))
                    ) {
                        Icon(
                            imageVector = habitIconForName(habit.iconName),
                            contentDescription = habit.title,
                            tint = pastelFg,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (habit.description.isNotBlank()) {
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                StreakIndicator(streakDays = currentStreak)
                Spacer(modifier = Modifier.width(10.dp))
                CompletionCheckbox(
                    checked = isDoneOnSelected,
                    accentColor = pastelFg,
                    onToggle = { onToggleDate(selectedDate) },
                    modifier = Modifier.testTag("habit_toggle_${habit.id}")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7-day history strip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.surface.copy(alpha = 0.78f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    weekDaysWithMeta.forEach { (d, iso, narrowLabel) ->
                        val done = completedDates.contains(iso)
                        val isTarget = d == selectedDate
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onToggleDate(d) }
                        ) {
                            Text(
                                text = narrowLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isTarget) pastelFg else palette.textSecondary,
                                fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (done) pastelFg else palette.surfaceMuted)
                                    .border(
                                        width = if (isTarget) 1.5.dp else 0.dp,
                                        color = if (isTarget) pastelFg else Color.Transparent,
                                        shape = CircleShape
                                    )
                            ) {
                                if (done) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Logged",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$completedInWeek/7 wk",
                        style = MaterialTheme.typography.labelLarge,
                        color = pastelFg
                    )
                    Text(
                        text = "Best: ${longestStreak}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

fun habitIconForName(iconName: String): ImageVector = when (iconName.lowercase()) {
    "water_drop" -> Icons.Outlined.WaterDrop
    "fitness_center" -> Icons.Outlined.FitnessCenter
    "menu_book" -> Icons.AutoMirrored.Outlined.MenuBook
    "self_improvement" -> Icons.Outlined.SelfImprovement
    "school" -> Icons.Outlined.School
    "check_circle" -> Icons.Outlined.CheckCircle
    else -> Icons.Outlined.Spa
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(palette.primarySoft)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = palette.primary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onAction() }
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onPrimary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    Surface(
        color = palette.priorityHighBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = palette.priorityHighText,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss error",
                    tint = palette.priorityHighText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
