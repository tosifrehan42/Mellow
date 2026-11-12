package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Habit
import com.example.data.model.HabitLog
import com.example.data.model.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MelloRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- TASKS ---

    fun observeTasks(userId: String = requireUserId()): Flow<List<Task>> = flow {
        val path = "users/$userId/tasks"
        emitAll(
            db.collection("users").document(userId).collection("tasks")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .flowOn(Dispatchers.Default)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getTaskById(taskId: String, userId: String = requireUserId()): Task? {
        val docRef = db.collection("users").document(userId).collection("tasks").document(taskId)
        return try {
            val snap = docRef.get().await()
            snap.toObject(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createTask(task: Task): String {
        val uid = requireUserId()
        val docRef = if (task.id.isNotBlank()) {
            db.collection("users").document(uid).collection("tasks").document(task.id)
        } else {
            db.collection("users").document(uid).collection("tasks").document()
        }
        val payload = mapOf(
            "userId" to uid,
            "title" to task.title.trim().take(200),
            "description" to task.description.trim().take(1000),
            "category" to task.category,
            "priority" to task.priority,
            "dueDateEpochDay" to task.dueDateEpochDay.coerceAtLeast(0L),
            "dueTime" to task.dueTime.take(20),
            "hasReminder" to task.hasReminder,
            "isCompleted" to task.isCompleted,
            "completedAtEpochMillis" to task.completedAtEpochMillis.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTask(task: Task) {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
        val updates = mapOf(
            "title" to task.title.trim().take(200),
            "description" to task.description.trim().take(1000),
            "category" to task.category,
            "priority" to task.priority,
            "dueDateEpochDay" to task.dueDateEpochDay.coerceAtLeast(0L),
            "dueTime" to task.dueTime.take(20),
            "hasReminder" to task.hasReminder,
            "isCompleted" to task.isCompleted,
            "completedAtEpochMillis" to task.completedAtEpochMillis.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleTaskCompletion(taskId: String, isCompleted: Boolean, completedAtMillis: Long) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        val updates = mapOf(
            "isCompleted" to isCompleted,
            "completedAtEpochMillis" to if (isCompleted) completedAtMillis.coerceAtLeast(1L) else 0L,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- HABITS ---

    fun observeHabits(userId: String = requireUserId()): Flow<List<Habit>> = flow {
        val path = "users/$userId/habits"
        emitAll(
            db.collection("users").document(userId).collection("habits")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Habit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .flowOn(Dispatchers.Default)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createHabit(habit: Habit): String {
        val uid = requireUserId()
        val docRef = if (habit.id.isNotBlank()) {
            db.collection("users").document(uid).collection("habits").document(habit.id)
        } else {
            db.collection("users").document(uid).collection("habits").document()
        }
        val payload = mapOf(
            "userId" to uid,
            "title" to habit.title.trim().take(120),
            "description" to habit.description.trim().take(500),
            "color" to habit.color,
            "iconName" to habit.iconName.take(40).ifBlank { "spa" },
            "isActive" to habit.isActive,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateHabit(habit: Habit) {
        val uid = requireUserId()
        require(habit.id.isNotBlank()) { "Habit ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("habits").document(habit.id)
        val updates = mapOf(
            "title" to habit.title.trim().take(120),
            "description" to habit.description.trim().take(500),
            "color" to habit.color,
            "iconName" to habit.iconName.take(40).ifBlank { "spa" },
            "isActive" to habit.isActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteHabit(habitId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("habits").document(habitId)
        try {
            val logsSnap = db.collection("users").document(uid).collection("habit_logs")
                .whereEqualTo("userId", uid)
                .whereEqualTo("habitId", habitId)
                .get()
                .await()
            val batch = db.batch()
            batch.delete(docRef)
            for (logDoc in logsSnap.documents) {
                batch.delete(logDoc.reference)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- HABIT LOGS ---

    fun observeHabitLogs(userId: String = requireUserId()): Flow<List<HabitLog>> = flow {
        val path = "users/$userId/habit_logs"
        emitAll(
            db.collection("users").document(userId).collection("habit_logs")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(HabitLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .flowOn(Dispatchers.Default)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun setHabitCompletedOnDate(habitId: String, dateIso: String, completed: Boolean) {
        val uid = requireUserId()
        val logId = "${habitId}_${dateIso}"
        val docRef = db.collection("users").document(uid).collection("habit_logs").document(logId)
        try {
            if (completed) {
                val payload = mapOf(
                    "userId" to uid,
                    "habitId" to habitId,
                    "completedOn" to dateIso,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.delete().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (completed) OperationType.CREATE else OperationType.DELETE
            handleFirestoreError(e, op, docRef.path)
            throw e
        }
    }

    // --- STARTER HABITS SEEDING & ACCOUNT CLEANUP ---

    suspend fun seedStarterHabitsIfEmpty() {
        val uid = requireUserId()
        val habitsCollection = db.collection("users").document(uid).collection("habits")
        val habitsPath = "users/$uid/habits"
        try {
            val existing = habitsCollection
                .whereEqualTo("userId", uid)
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) return

            val starters = defaultStarterHabits(uid)
            val batch = db.batch()
            for (starter in starters) {
                val docRef = if (starter.id.isNotBlank()) {
                    habitsCollection.document(starter.id)
                } else {
                    habitsCollection.document()
                }
                val payload = mapOf(
                    "userId" to uid,
                    "title" to starter.title.trim().take(120),
                    "description" to starter.description.trim().take(500),
                    "color" to starter.color,
                    "iconName" to starter.iconName.take(40).ifBlank { "spa" },
                    "isActive" to starter.isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, habitsPath)
        }
    }

    suspend fun deleteAllUserData() {
        val uid = requireUserId()
        val collections = listOf("tasks", "habits", "habit_logs")
        for (col in collections) {
            val path = "users/$uid/$col"
            try {
                val snap = db.collection("users").document(uid).collection(col)
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()
                if (!snap.isEmpty) {
                    for (chunk in snap.documents.chunked(450)) {
                        val batch = db.batch()
                        for (doc in chunk) {
                            batch.delete(doc.reference)
                        }
                        batch.commit().await()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, path)
                throw e
            }
        }
    }

    companion object {
        fun defaultStarterHabits(userId: String): List<Habit> = listOf(
            Habit(
                id = "starter_reading",
                userId = userId,
                title = "Read 20 minutes",
                description = "Read 20 minutes of a book or article daily",
                color = "Lavender",
                iconName = "menu_book",
                isActive = true
            ),
            Habit(
                id = "starter_exercise",
                userId = userId,
                title = "Exercise",
                description = "Daily movement, workout or stretching session",
                color = "Mint",
                iconName = "fitness_center",
                isActive = true
            ),
            Habit(
                id = "starter_water",
                userId = userId,
                title = "Drink enough water",
                description = "Stay hydrated with 8 glasses of water",
                color = "Sky",
                iconName = "water_drop",
                isActive = true
            ),
            Habit(
                id = "starter_coding",
                userId = userId,
                title = "Practice coding",
                description = "Build or solve problems for 30 minutes",
                color = "Pink",
                iconName = "school",
                isActive = true
            ),
            Habit(
                id = "starter_studying",
                userId = userId,
                title = "Study for one hour",
                description = "Focused deep-work study block without distractions",
                color = "Butter",
                iconName = "self_improvement",
                isActive = true
            )
        )
    }
}
