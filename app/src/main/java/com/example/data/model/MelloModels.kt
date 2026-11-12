package com.example.data.model

import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
@IgnoreExtraProperties
data class Task(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Personal",
    val priority: String = "Medium",
    val dueDateEpochDay: Long = 0L,
    val dueTime: String = "",
    val hasReminder: Boolean = false,
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    val completedAtEpochMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@IgnoreExtraProperties
data class Habit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val color: String = "Lavender",
    val iconName: String = "spa",
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
@IgnoreExtraProperties
data class HabitLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val habitId: String = "",
    val completedOn: String = "", // YYYY-MM-DD
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class TaskCategory(val label: String, val colorKey: String) {
    PERSONAL("Personal", "Pink"),
    STUDY("Study", "Sky"),
    WORK("Work", "Lavender"),
    HEALTH("Health", "Mint"),
    OTHER("Other", "Butter");

    companion object {
        private val customColorCycle = listOf("Lavender", "Pink", "Mint", "Butter", "Sky", "Peach")

        fun fromLabel(label: String): TaskCategory =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: OTHER

        fun colorKeyForCategory(category: String): String {
            val match = entries.firstOrNull { it.label.equals(category, ignoreCase = true) }
            if (match != null) return match.colorKey
            if (category.equals("Wellness", ignoreCase = true)) return "Mint"
            if (category.equals("Errands", ignoreCase = true)) return "Butter"
            val idx = (category.lowercase().hashCode() and 0x7fffffff) % customColorCycle.size
            return customColorCycle[idx]
        }
    }
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    companion object {
        fun fromLabel(label: String): TaskPriority =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: MEDIUM
    }
}

enum class TaskFilterTab(val label: String) {
    TODAY("Today"),
    UPCOMING("Upcoming"),
    ALL("All"),
    COMPLETED("Completed"),
    OVERDUE("Overdue")
}

enum class TaskSortOption(val label: String) {
    DUE_DATE("Due Date"),
    CREATED_DATE("Created Date")
}

enum class HabitColor(val key: String, val displayName: String) {
    LAVENDER("Lavender", "Soft Lavender"),
    PINK("Pink", "Pastel Pink"),
    MINT("Mint", "Mint Green"),
    BUTTER("Butter", "Butter Yellow"),
    SKY("Sky", "Soft Sky"),
    PEACH("Peach", "Warm Peach");

    companion object {
        fun fromKey(key: String): HabitColor =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: LAVENDER
    }
}

enum class ThemePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val isDark: Boolean = false
) {
    SOFT_LAVENDER("soft_lavender", "Soft Lavender", "Calm pastel purple & airy whites"),
    ROSE_QUARTZ("rose_quartz", "Rose Quartz", "Warm blush tones & petal accents"),
    SAGE_GREEN("sage_green", "Sage Green", "Botanical mint & grounding sage"),
    BUTTER_CREAM("butter_cream", "Butter Cream", "Sunlit warm ivory & honey gold"),
    MIDNIGHT("midnight", "Midnight", "Cozy deep plum & luminous lavender", isDark = true),
    MINIMAL_MONOCHROME("minimal_monochrome", "Minimal Monochrome", "Crisp ink & warm editorial paper");

    companion object {
        fun fromId(id: String): ThemePreset =
            entries.firstOrNull { it.id == id } ?: SOFT_LAVENDER
    }
}

@Immutable
data class HabitInsights(
    val averageCompletionPercent: Int = 0,
    val currentOverallStreak: Int = 0,
    val longestOverallStreak: Int = 0,
    val weeklyCompletionByDay: List<Pair<String, Float>> = emptyList(),
    val habitStreaks: Map<String, Pair<Int, Int>> = emptyMap() // habitId -> (currentStreak, longestStreak)
)

/**
 * Shared thread-safe DateTimeFormatter singletons to avoid allocating formatters during composition or scrolling.
 */
object MelloDateFormatters {
    val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val SHORT_MONTH_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val FULL_WEEKDAY_MONTH_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    val SHORT_WEEKDAY_MONTH_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
}
