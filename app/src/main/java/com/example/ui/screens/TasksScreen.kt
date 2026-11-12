package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.data.model.TaskFilterTab
import com.example.data.model.TaskPriority
import com.example.ui.components.EmptyState
import com.example.ui.components.TaskCard
import com.example.ui.theme.MelloThemeColors
import com.example.ui.viewmodel.MelloUiState
import com.example.ui.viewmodel.MelloViewModel

@Composable
fun TasksScreen(
    uiState: MelloUiState,
    customCategories: List<String> = emptyList(),
    onSelectFilterTab: (TaskFilterTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategoryFilter: (String?) -> Unit,
    onSelectPriorityFilter: (String?) -> Unit,
    onToggleSortOption: () -> Unit,
    onOpenCalendar: () -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    val filteredTasks = if (uiState.filteredTasks.isNotEmpty() || uiState.tasks.isEmpty()) {
        uiState.filteredTasks
    } else {
        remember(
            uiState.tasks,
            uiState.taskFilterTab,
            uiState.searchQuery,
            uiState.selectedCategoryFilter,
            uiState.selectedPriorityFilter,
            uiState.sortOption
        ) {
            MelloViewModel.filterAndSortTasks(uiState)
        }
    }

    val completedCount = if (uiState.completedTasksCount > 0 || uiState.remainingTasksCount > 0 || uiState.tasks.isEmpty()) {
        uiState.completedTasksCount
    } else {
        remember(uiState.tasks) { uiState.tasks.count { it.isCompleted } }
    }
    val remainingCount = if (uiState.completedTasksCount > 0 || uiState.remainingTasksCount > 0 || uiState.tasks.isEmpty()) {
        uiState.remainingTasksCount
    } else {
        remember(uiState.tasks, completedCount) { (uiState.tasks.size - completedCount).coerceAtLeast(0) }
    }

    val categoryLabels = remember(customCategories) {
        (TaskCategory.entries.map { it.label } + customCategories)
            .distinctBy { it.lowercase() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(top = 16.dp)
    ) {
        // Screen Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.textPrimary
                )
                Text(
                    text = "$completedCount completed • $remainingCount remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sort toggle pill
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onToggleSortOption() }
                        .testTag("tasks_sort_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapVert,
                            contentDescription = "Sort tasks",
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = uiState.sortOption.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary
                        )
                    }
                }

                Surface(
                    color = palette.surface,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(onClick = onOpenCalendar) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Calendar view",
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search input
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search tasks, notes, or categories...") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear search",
                            tint = palette.textSecondary
                        )
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.surface,
                unfocusedContainerColor = palette.surface,
                focusedBorderColor = palette.primary,
                unfocusedBorderColor = palette.border
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("tasks_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Filter Tabs (Today, Upcoming, All, Completed, Overdue)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = TaskFilterTab.entries,
                key = { it.name },
                contentType = { "filter_tab" }
            ) { tab ->
                val isSelected = uiState.taskFilterTab == tab
                Surface(
                    color = if (isSelected) palette.primary else palette.surface,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, if (isSelected) palette.primary else palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectFilterTab(tab) }
                        .testTag("task_tab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) palette.onPrimary else palette.textSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category & Priority Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = categoryLabels,
                key = { "cat_$it" },
                contentType = { "category_chip" }
            ) { catLabel ->
                val isSelected = uiState.selectedCategoryFilter.equals(catLabel, ignoreCase = true)
                Surface(
                    color = if (isSelected) palette.primarySoft else palette.surfaceMuted,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, if (isSelected) palette.primary else palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectCategoryFilter(catLabel) }
                ) {
                    Text(
                        text = catLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) palette.primary else palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                    )
                }
            }

            items(
                items = TaskPriority.entries,
                key = { "prio_${it.name}" },
                contentType = { "priority_chip" }
            ) { prio ->
                val isSelected = uiState.selectedPriorityFilter.equals(prio.label, ignoreCase = true)
                Surface(
                    color = if (isSelected) palette.primarySoft else palette.surfaceMuted,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, if (isSelected) palette.primary else palette.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectPriorityFilter(prio.label) }
                ) {
                    Text(
                        text = "${prio.label} Priority",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) palette.primary else palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Task List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredTasks.isEmpty()) {
                item(key = "tasks_empty_state", contentType = "empty_state") {
                    EmptyState(
                        title = "No matching tasks",
                        subtitle = "No tasks found in ${uiState.taskFilterTab.label}. Create a new task or adjust your filters.",
                        actionLabel = "+ Add New Task",
                        onAction = onAddTask
                    )
                }
            } else {
                items(
                    items = filteredTasks,
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
        }
    }
}
