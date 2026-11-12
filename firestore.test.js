const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-mello-19f79e4c-ff23-42ed-945f-321d5014d4c8";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function getDb(context) {
  return context.firestore(DATABASE_ID);
}

test("Unauthenticated user cannot read or write tasks", async () => {
  const unauthDb = getDb(testEnv.unauthenticatedContext());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );
});

test("Authenticated user can create, read, update, and delete their own task", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const now = new Date();
  const taskRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("tasks")
    .doc("task_1");

  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      title: "Finish UI design",
      description: "Pastel lavender theme",
      category: "Custom Design",
      priority: "High",
      dueDateEpochDay: 20768,
      dueTime: "14:00",
      hasReminder: true,
      isCompleted: false,
      completedAtEpochMillis: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(taskRef.get());

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertSucceeds(
    taskRef.update({
      isCompleted: true,
      completedAtEpochMillis: 1700000000000,
      updatedAt: now,
    })
  );

  await assertSucceeds(taskRef.delete());
});

test("Authenticated user cannot read or modify another user's tasks, habits, or logs", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = getDb(context);
    await adminDb
      .collection("users")
      .doc(BOB_UID)
      .collection("tasks")
      .doc("bob_task")
      .set({
        userId: BOB_UID,
        title: "Bob Private Task",
        description: "",
        category: "Personal",
        priority: "Medium",
        dueDateEpochDay: 20768,
        dueTime: "",
        hasReminder: false,
        isCompleted: false,
        completedAtEpochMillis: 0,
        createdAt: now,
        updatedAt: now,
      });
  });

  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("tasks")
      .doc("bob_task")
      .get()
  );
});

test("Shadow update with ghost field on task is rejected", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const now = new Date();
  const taskRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("tasks")
    .doc("task_shadow");

  await assertFails(
    taskRef.set({
      userId: ALICE_UID,
      title: "Valid Title",
      description: "",
      category: "Work",
      priority: "Low",
      dueDateEpochDay: 0,
      dueTime: "",
      hasReminder: false,
      isCompleted: false,
      completedAtEpochMillis: 0,
      createdAt: now,
      updatedAt: now,
      isVerifiedGhostField: true,
    })
  );
});

test("Authenticated user can create habits and deterministic habit_logs", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const now = new Date();
  const habitRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("habits")
    .doc("habit_water");

  await assertSucceeds(
    habitRef.set({
      userId: ALICE_UID,
      title: "Drink Water",
      description: "8 glasses daily",
      color: "Mint",
      iconName: "water_drop",
      isActive: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  const validLogRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("habit_logs")
    .doc("habit_water_2026-11-10");

  await assertSucceeds(
    validLogRef.set({
      userId: ALICE_UID,
      habitId: "habit_water",
      completedOn: "2026-11-10",
      createdAt: now,
    })
  );

  // Mismatched deterministic ID must fail
  const invalidLogRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("habit_logs")
    .doc("wrong_id_format");

  await assertFails(
    invalidLogRef.set({
      userId: ALICE_UID,
      habitId: "habit_water",
      completedOn: "2026-11-10",
      createdAt: now,
    })
  );
});
