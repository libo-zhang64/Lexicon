const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    displayName: "Alice Learner",
    currentStreak: 3,
    longestStreak: 7,
    lastPracticeDate: "2026-11-22",
    totalLessonsCompleted: 4,
    totalQuizzesCompleted: 2,
    cumulativeCorrectAnswers: 8,
    cumulativeQuizQuestions: 10,
    createdAt: now,
    updatedAt: now,
  };
}

function validGoalPayload(now = new Date(Date.now() - 2000)) {
  return {
    title: "Learn 5 new idioms",
    cadence: "WEEKLY",
    metricType: "LEARN_NEW",
    categoryFilter: "IDIOM",
    targetCount: 5,
    active: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validProgressPayload(phraseId, now = new Date(Date.now() - 2000)) {
  return {
    phraseId,
    category: "PHRASAL_VERB",
    bookmarked: true,
    lessonCompleted: true,
    lessonCompletedDay: "2026-11-22",
    lessonCompletedWeek: "2026-W47",
    masteryLevel: 2,
    masteredDay: "",
    masteredWeek: "",
    correctCount: 2,
    attemptCount: 3,
    missCount: 1,
    consecutiveCorrect: 1,
    missedClozeCount: 1,
    missedRegisterCount: 0,
    missedMeaningCount: 0,
    lastReviewedDay: "2026-11-22",
    createdAt: now,
    updatedAt: now,
  };
}

function validCustomPhrasePayload(now = new Date(Date.now() - 2000)) {
  return {
    term: "boil the ocean",
    ipa: "/bɔɪl ði ˈoʊʃən/",
    category: "WORKPLACE",
    register: "PROFESSIONAL",
    cefrLevel: "C1",
    definition: "To attempt an impossibly large or unfocused scope of work.",
    nuanceSummary: "Often used to diplomatically narrow a project scope in US corporate meetings.",
    dialogue: [
      { speaker: "Maya", line: "Should we rebuild the whole onboarding flow this sprint?" },
      { speaker: "Devon", line: "Let's not boil the ocean—let's ship the checklist first." }
    ],
    etymology: "Coined in American management consulting in the late 20th century as a humorous image of wasted effort.",
    synonyms: [
      { term: "bite off more than you can chew", nuanceDifference: "More informal and personal." }
    ],
    antonyms: ["focus on quick wins", "scope down"],
    usageNotes: ["Common in US tech and consulting; neutral-professional tone."],
    createdAt: now,
    updatedAt: now,
  };
}

function validQuizSessionPayload(now = new Date(Date.now() - 2000)) {
  return {
    mode: "SMART_REVIEW",
    category: "ALL",
    score: 4,
    totalQuestions: 5,
    missedPhraseIds: ["iron_out"],
    missedCloze: 1,
    missedRegister: 0,
    missedMeaning: 0,
    periodDay: "2026-11-22",
    periodWeek: "2026-W47",
    completedAt: now,
  };
}

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

test("Unauthenticated users cannot read or write any Lexicon collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.doc(`users/${ALICE_UID}`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}`).set(validProfilePayload(ALICE_UID)));
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/learningGoals`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/phraseProgress`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/customPhrases`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/quizSessions`).get());
});

test("UserProfile: owner can create and partially update, rejects spoofing and ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}`).set(validProfilePayload(ALICE_UID, created)));
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}`).get());

  // Partial update preserving createdAt and userId
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}`).update({
      currentStreak: 4,
      totalLessonsCompleted: 5,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field shadow update
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}`).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}`).update({
      currentStreak: 5,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}`).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user access
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}`).update({ displayName: "Hijacked" }));
});

test("LearningGoals: owner CRUD and list succeed; invalid cadence/target/cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/learningGoals/goal_idioms`).set(validGoalPayload(created))
  );
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/learningGoals`).get());

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/learningGoals/goal_idioms`).update({
      targetCount: 8,
      active: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject @DocumentId 'id' in payload
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/learningGoals/goal_bad_id`).set({
      ...validGoalPayload(created),
      id: "goal_bad_id",
    })
  );

  // Reject out-of-range targetCount
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/learningGoals/goal_invalid`).set({
      ...validGoalPayload(created),
      targetCount: 0,
    })
  );

  // Cross-user list & write denied
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/learningGoals`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/learningGoals/goal_idioms`).delete()
  );
});

test("PhraseProgress: owner tracks lessons, mastery, and mistakes; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/phraseProgress/iron_out`).set(validProgressPayload("iron_out", created))
  );
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/phraseProgress`).get());

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/phraseProgress/iron_out`).update({
      masteryLevel: 3,
      masteredDay: "2026-11-22",
      masteredWeek: "2026-W47",
      correctCount: 3,
      attemptCount: 4,
      consecutiveCorrect: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject mismatched phraseId
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/phraseProgress/pan_out`).set(validProgressPayload("iron_out", created))
  );

  // Reject masteryLevel > 3
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/phraseProgress/iron_out`).update({
      masteryLevel: 9,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/phraseProgress/iron_out`).get());
});

test("CustomPhrases & QuizSessions: owner operations succeed, quiz history is immutable", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/customPhrases/boil_the_ocean`).set(validCustomPhrasePayload(created))
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/customPhrases/gre_extracted_word`).set({
      ...validCustomPhrasePayload(created),
      term: "perspicacious",
      category: "GRE_VOCAB",
      register: "FORMAL",
    })
  );
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/customPhrases`).get());

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/quizSessions/session_1`).set(validQuizSessionPayload(created))
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/quizSessions/session_transfer`).set({
      ...validQuizSessionPayload(created),
      mode: "TRANSFER_CHALLENGE",
      category: "GRE_VOCAB",
      missedPhraseIds: ["transfer_trenchant"],
    })
  );
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/quizSessions`).get());

  // Quiz sessions are append-only (no updates)
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/quizSessions/session_1`).update({ score: 5 })
  );

  // Reject score > totalQuestions
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/quizSessions/session_bad`).set({
      ...validQuizSessionPayload(created),
      score: 6,
      totalQuestions: 5,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/customPhrases`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/quizSessions`).get());
});
