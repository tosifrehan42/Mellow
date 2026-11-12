package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Habit
import com.example.data.model.HabitInsights
import com.example.data.model.MelloDateFormatters
import com.example.ui.components.EmptyState
import com.example.ui.components.HabitCard
import com.example.ui.components.WeeklyDateSelector
import com.example.ui.theme.MelloThemeColors
import com.example.ui.viewmodel.MelloUiState
import com.example.ui.viewmodel.MelloViewModel
import java.time.LocalDate

@Composable
fun HabitsScreen(
    uiState: MelloUiState,
    onSelectDate: (LocalDate) -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onToggleHabitDate: (String, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    val selectedDate = remember(uiState.selectedDateEpochDay) {
        LocalDate.ofEpochDay(uiState.selectedDateEpochDay)
    }
    val shortDateLabel = remember(selectedDate) {
        selectedDate.format(MelloDateFormatters.SHORT_MONTH_DAY)
    }

    val activeHabits = if (uiState.activeHabits.isNotEmpty() || uiState.habits.isEmpty()) {
        uiState.activeHabits
    } else {
        remember(uiState.habits) { uiState.habits.filter { it.isActive } }
    }

    val insights = if (uiState.habitInsights.habitStreaks.isNotEmpty() || uiState.habits.isEmpty()) {
        uiState.habitInsights
    } else {
        remember(uiState.habits, uiState.habitLogs) {
            MelloViewModel.calculateHabitInsights(uiState.habits, uiState.habitLogs)
        }
    }

    val logsByHabit = uiState.logsByHabitId
    val completedDatesByHabit = uiState.completedDatesByHabitId
    val activityDates = uiState.habitLogDates

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item(key = "habits_header", contentType = "header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Habits",
                        style = MaterialTheme.typography.headlineLarge,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Small daily rituals that build lasting streaks",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                Surface(
                    color = palette.primarySoft,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onAddHabit() }
                        .testTag("add_habit_header_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New habit",
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New Habit",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primary
                        )
                    }
                }
            }
        }

        // Date Selector
        item(key = "habits_week_selector", contentType = "week_selector") {
            WeeklyDateSelector(
                selectedDate = selectedDate,
                datesWithActivity = activityDates,
                onSelectDate = onSelectDate
            )
        }

        // Insights Section (Average Completion, Current Streak, Longest Streak, Weekly Chart)
        item(key = "habits_insights_card", contentType = "insights_card") {
            HabitInsightsSection(insights = insights)
        }

        // Daily Habits List Header
        item(key = "habits_list_header", contentType = "section_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Daily Habits (${activeHabits.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                Text(
                    text = shortDateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primary
                )
            }
        }

        if (activeHabits.isEmpty()) {
            item(key = "habits_empty_state", contentType = "empty_state") {
                EmptyState(
                    title = "Start your first daily habit",
                    subtitle = "Track simple habits like Drinking water, Exercise, Reading, or Meditation.",
                    actionLabel = "+ Create Habit",
                    onAction = onAddHabit
                )
            }
        } else {
            items(
                items = activeHabits,
                key = { it.id },
                contentType = { "habit_card" }
            ) { habit ->
                val (currStreak, bestStreak) = insights.habitStreaks[habit.id] ?: (0 to 0)
                HabitCard(
                    habit = habit,
                    selectedDate = selectedDate,
                    logsForHabit = logsByHabit[habit.id] ?: emptyList(),
                    currentStreak = currStreak,
                    longestStreak = bestStreak,
                    onToggleDate = { date -> onToggleHabitDate(habit.id, date) },
                    onEdit = { onEditHabit(habit) },
                    onDelete = { onDeleteHabit(habit.id) },
                    completedDateIsoSet = completedDatesByHabit[habit.id]
                )
            }
        }
    }
}

@Composable
private fun HabitInsightsSection(insights: HabitInsights) {
    val palette = MelloThemeColors.palette
    val reducedMotion = MelloThemeColors.reducedMotion

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_insights_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Habit Insights",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 3 Metric Cards
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InsightStatTile(
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    label = "Avg. Rate",
                    value = "${insights.averageCompletionPercent}%",
                    bgColor = palette.pastelMint,
                    fgColor = palette.pastelMintText,
                    modifier = Modifier.weight(1f)
                )
                InsightStatTile(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Current",
                    value = "${insights.currentOverallStreak}d",
                    bgColor = palette.pastelButter,
                    fgColor = palette.pastelButterText,
                    modifier = Modifier.weight(1f)
                )
                InsightStatTile(
                    icon = Icons.Outlined.EmojiEvents,
                    label = "Longest",
                    value = "${insights.longestOverallStreak}d",
                    bgColor = palette.pastelPink,
                    fgColor = palette.pastelPinkText,
                    modifier = Modifier.weight(1f)
                )
            }

            if (insights.weeklyCompletionByDay.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "7-Day Completion Consistency",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    insights.weeklyCompletionByDay.forEach { (dayLabel, ratio) ->
                        val animRatio by animateFloatAsState(
                            targetValue = ratio.coerceIn(0.08f, 1f),
                            animationSpec = if (reducedMotion) tween(0) else tween(500),
                            label = "barRatio"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Box(
                                contentAlignment = Alignment.BottomCenter,
                                modifier = Modifier
                                    .width(22.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(palette.surfaceMuted)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animRatio)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (ratio >= 0.8f) palette.success else palette.primary
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightStatTile(
    icon: ImageVector,
    label: String,
    value: String,
    bgColor: androidx.compose.ui.graphics.Color,
    fgColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fgColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = fgColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fgColor.copy(alpha = 0.85f)
        )
    }
}
