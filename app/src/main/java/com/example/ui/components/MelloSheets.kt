package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Habit
import com.example.data.model.HabitColor
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.ui.theme.MelloThemeColors
import com.example.ui.theme.cardPastelPair
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskSheet(
    initialTask: Task? = null,
    defaultDate: LocalDate = LocalDate.now(),
    customCategories: List<String> = emptyList(),
    onAddCustomCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDateEpochDay: Long,
        dueTime: String,
        hasReminder: Boolean
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val palette = MelloThemeColors.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(initialTask) { mutableStateOf(initialTask?.title ?: "") }
    var description by remember(initialTask) { mutableStateOf(initialTask?.description ?: "") }
    var category by remember(initialTask) { mutableStateOf(initialTask?.category ?: TaskCategory.PERSONAL.label) }
    var priority by remember(initialTask) { mutableStateOf(initialTask?.priority ?: TaskPriority.MEDIUM.label) }
    var dueDateEpochDay by remember(initialTask) {
        mutableLongStateOf(initialTask?.dueDateEpochDay ?: defaultDate.toEpochDay())
    }
    var dueTime by remember(initialTask) { mutableStateOf(initialTask?.dueTime ?: "09:30") }
    var hasReminder by remember(initialTask) { mutableStateOf(initialTask?.hasReminder ?: false) }
    var titleError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    var showCustomCategoryInput by remember { mutableStateOf(false) }
    var newCustomCategoryText by remember { mutableStateOf("") }

    val allCategoryLabels = remember(customCategories, category) {
        val defaults = TaskCategory.entries.map { it.label }
        (defaults + customCategories + listOfNotNull(category.takeIf { it.isNotBlank() }))
            .distinctBy { it.lowercase() }
    }

    val quickTimePresets = remember { listOf("", "09:00", "12:30", "15:00", "18:30", "21:00") }
    val datePresets = remember {
        val today = LocalDate.now()
        listOf(
            "Today" to today.toEpochDay(),
            "Tomorrow" to today.plusDays(1).toEpochDay(),
            "In 3 Days" to today.plusDays(3).toEpochDay(),
            "Next Week" to today.plusDays(7).toEpochDay(),
            "No Date" to 0L
        )
    }
    val formattedDueDate = remember(dueDateEpochDay) {
        if (dueDateEpochDay > 0L) {
            LocalDate.ofEpochDay(dueDateEpochDay).format(com.example.data.model.MelloDateFormatters.SHORT_WEEKDAY_MONTH_DAY)
        } else {
            ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (initialTask == null) "New Task" else "Edit Task",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
                if (initialTask != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(initialTask.id)
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete task",
                            tint = palette.danger
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text("Task title *") },
                placeholder = { Text("e.g., Read 20 minutes of design book") },
                isError = titleError,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )
            if (titleError) {
                Text(
                    text = "Title is required",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.danger,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Short note (optional)") },
                placeholder = { Text("Add helpful details or checklist cues...") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_desc_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allCategoryLabels.forEach { catLabel ->
                    val isSelected = category.equals(catLabel, ignoreCase = true)
                    val colorKey = TaskCategory.colorKeyForCategory(catLabel)
                    val (bg, fg) = palette.cardPastelPair(colorKey)
                    Surface(
                        color = if (isSelected) bg else palette.surfaceMuted,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) fg else palette.border
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { category = catLabel }
                    ) {
                        Text(
                            text = catLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) fg else palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }

                // + Custom category trigger chip
                Surface(
                    color = palette.primarySoft,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showCustomCategoryInput = !showCustomCategoryInput }
                        .testTag("add_custom_category_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add custom category",
                            tint = palette.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Custom",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primary
                        )
                    }
                }
            }

            if (showCustomCategoryInput) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newCustomCategoryText,
                        onValueChange = { newCustomCategoryText = it.take(30) },
                        placeholder = { Text("New category name...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primary,
                            unfocusedBorderColor = palette.border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_category_input")
                    )
                    TextButton(
                        onClick = {
                            val clean = newCustomCategoryText.trim()
                            if (clean.isNotBlank()) {
                                onAddCustomCategory(clean)
                                category = clean
                                newCustomCategoryText = ""
                                showCustomCategoryInput = false
                            }
                        }
                    ) {
                        Text("Add", color = palette.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Priority",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TaskPriority.entries.forEach { prio ->
                    val isSelected = priority.equals(prio.label, ignoreCase = true)
                    Surface(
                        color = if (isSelected) palette.primarySoft else palette.surfaceMuted,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) palette.primary else palette.border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable { priority = prio.label }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        ) {
                            Text(
                                text = prio.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) palette.primary else palette.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Due Date",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary
                )
                if (formattedDueDate.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDueDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                datePresets.forEach { (label, epochDay) ->
                    val isSelected = dueDateEpochDay == epochDay
                    Surface(
                        color = if (isSelected) palette.primarySoft else palette.surfaceMuted,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) palette.primary else palette.border
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { dueDateEpochDay = epochDay }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) palette.primary else palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Due Time (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickTimePresets.forEach { preset ->
                    val display = preset.ifBlank { "None" }
                    val isSelected = dueTime == preset
                    Surface(
                        color = if (isSelected) palette.primarySoft else palette.surfaceMuted,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) palette.primary else palette.border
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { dueTime = preset }
                    ) {
                        Text(
                            text = display,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) palette.primary else palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = palette.surfaceMuted,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Task Reminder",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Send a local Android notification for this task",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Switch(
                        checked = hasReminder,
                        onCheckedChange = { hasReminder = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = palette.onPrimary, checkedTrackColor = palette.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        isSubmitting = true
                        onSave(
                            initialTask?.id,
                            title,
                            description,
                            category,
                            priority,
                            dueDateEpochDay,
                            dueTime,
                            hasReminder
                        )
                        onDismiss()
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button")
            ) {
                Text(
                    text = if (initialTask == null) "Create Task" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHabitSheet(
    initialHabit: Habit? = null,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        title: String,
        description: String,
        color: String,
        iconName: String,
        isActive: Boolean
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val palette = MelloThemeColors.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(initialHabit) { mutableStateOf(initialHabit?.title ?: "") }
    var description by remember(initialHabit) { mutableStateOf(initialHabit?.description ?: "") }
    var color by remember(initialHabit) { mutableStateOf(initialHabit?.color ?: HabitColor.LAVENDER.key) }
    var iconName by remember(initialHabit) { mutableStateOf(initialHabit?.iconName ?: "spa") }
    var isActive by remember(initialHabit) { mutableStateOf(initialHabit?.isActive ?: true) }
    var titleError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val iconOptions = remember {
        listOf(
            "water_drop" to "Hydrate",
            "fitness_center" to "Exercise",
            "menu_book" to "Reading",
            "self_improvement" to "Mindful",
            "school" to "Coding/Study",
            "spa" to "Wellness"
        )
    }

    val quickHabitIdeas = remember {
        listOf(
            "Read 20 minutes",
            "Exercise",
            "Drink enough water",
            "Practice coding",
            "Study for one hour"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (initialHabit == null) "New Daily Habit" else "Edit Habit",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
                if (initialHabit != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(initialHabit.id)
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete habit",
                            tint = palette.danger
                        )
                    }
                }
            }

            if (initialHabit == null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Quick presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickHabitIdeas.forEach { idea ->
                        Surface(
                            color = palette.surfaceMuted,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { title = idea }
                        ) {
                            Text(
                                text = "+ $idea",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primary,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text("Habit name *") },
                placeholder = { Text("e.g., Read 20 minutes") },
                isError = titleError,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Daily goal / cue") },
                placeholder = { Text("e.g., Right after morning coffee") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.border
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pastel Card Color",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HabitColor.entries.forEach { hc ->
                    val isSelected = color.equals(hc.key, ignoreCase = true)
                    val (bg, fg) = palette.cardPastelPair(hc.key)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(bg)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) fg else palette.border,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { color = hc.key }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(fg)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = hc.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = fg,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Icon",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                iconOptions.forEach { (key, label) ->
                    val isSelected = iconName == key
                    Surface(
                        color = if (isSelected) palette.primarySoft else palette.surfaceMuted,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) palette.primary else palette.border
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { iconName = key }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = habitIconForName(key),
                                contentDescription = label,
                                tint = if (isSelected) palette.primary else palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) palette.primary else palette.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        isSubmitting = true
                        onSave(
                            initialHabit?.id,
                            title,
                            description,
                            color,
                            iconName,
                            isActive
                        )
                        onDismiss()
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_habit_button")
            ) {
                Text(
                    text = if (initialHabit == null) "Start Habit" else "Save Habit",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
