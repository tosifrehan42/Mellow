package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Habit
import com.example.data.model.Task
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MelloRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveTask_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = MelloRepository(firestore)
        val uniqueTaskId = "task_${UUID.randomUUID().toString().replace("-", "")}"

        val createdId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                Task(
                    id = uniqueTaskId,
                    userId = uid,
                    title = "Prepare study notes",
                    description = "Chapter 4 review",
                    category = "Study",
                    priority = "High",
                    dueDateEpochDay = 20768L,
                    dueTime = "10:00",
                    hasReminder = true,
                    isCompleted = false,
                    completedAtEpochMillis = 0L
                )
            )
        }
        assertEquals(uniqueTaskId, createdId)

        val tasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks(uid).first { list -> list.any { it.id == uniqueTaskId } }
        }
        assertTrue(tasks.any { it.id == uniqueTaskId && it.title == "Prepare study notes" })

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleTaskCompletion(uniqueTaskId, true, 1700000000000L)
        }
        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getTaskById(uniqueTaskId, uid)
        }
        assertNotNull(fetched)
        assertTrue(fetched!!.isCompleted)
    }

    @Test
    fun createHabitAndToggleLog_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = MelloRepository(firestore)
        val habitId = "habit_${UUID.randomUUID().toString().replace("-", "")}"

        val createdHabitId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createHabit(
                Habit(
                    id = habitId,
                    userId = uid,
                    title = "Morning Meditation",
                    description = "10 mins mindfulness",
                    color = "Lavender",
                    iconName = "self_improvement",
                    isActive = true
                )
            )
        }
        assertEquals(habitId, createdHabitId)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setHabitCompletedOnDate(habitId, "2026-11-10", true)
        }

        val logs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeHabitLogs(uid).first { list ->
                list.any { it.habitId == habitId && it.completedOn == "2026-11-10" }
            }
        }
        assertTrue(logs.any { it.id == "${habitId}_2026-11-10" })
    }

    @Test
    fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = MelloRepository(firestore)
        val taskId = "task_${UUID.randomUUID().toString().replace("-", "")}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTask(
                Task(
                    id = taskId,
                    userId = aliceUid,
                    title = "Alice Secret Task",
                    category = "Personal",
                    priority = "Medium"
                )
            )
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = MelloRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getTaskById(taskId, aliceUid)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = MelloRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks(aliceUid).first()
            }
            fail("Expected unauthenticated observeTasks to fail")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@mello.test"
        const val BOB_EMAIL = "bob@mello.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
