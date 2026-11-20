const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_skybank_01";
const BOB_UID = "bob_skybank_02";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, now) {
  return {
    userId: uid,
    displayName: "Rahat Khan",
    bankName: "ইস্কাই ব্যাংক",
    cashBdt: 550000,
    debtBdt: 1200000,
    energy: 25,
    maxEnergy: 25,
    level: 1,
    xp: 0,
    dayNumber: 1,
    minuteOfDay: 680,
    bankOwned: false,
    bankUpgraded: false,
    atmActive: false,
    carShowroomUnlocked: false,
    vaultLevel: 1,
    counterCount: 1,
    dailyRevenueBdt: 0,
    dailyTargetBdt: 60000,
    customersServed: 0,
    fraudCaught: 0,
    bankOpen: false,
    introSeen: false,
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated user cannot read or write bank profiles or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Owner can create, read, and partially update BankProfile; immutable fields & ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID, created)));
  await assertSucceeds(profileRef.get());

  // Partial update mirroring Kotlin BankProfile update
  await assertSucceeds(
    profileRef.update({
      cashBdt: 250000,
      bankOwned: true,
      introSeen: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject mutating createdAt
  await assertFails(
    profileRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject negative cash
  await assertFails(
    profileRef.update({
      cashBdt: -100,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can create and list transactions under users/{uid}/transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);
  const txCol = aliceDb.collection("users").doc(ALICE_UID).collection("transactions");

  await assertSucceeds(
    txCol.doc("tx_001").set({
      userId: ALICE_UID,
      customerName: "সাকিবুল হাসান",
      accountNo: "SKY-40201458",
      txType: "DEPOSIT",
      amountBdt: 5200,
      commissionBdt: 520,
      dayNumber: 1,
      success: true,
      createdAt: now,
    })
  );

  await assertSucceeds(txCol.get());

  // Reject invalid txType
  await assertFails(
    txCol.doc("tx_bad").set({
      userId: ALICE_UID,
      customerName: "Invalid",
      accountNo: "SKY-999",
      txType: "HACKED",
      amountBdt: 100,
      commissionBdt: 10,
      dayNumber: 1,
      success: true,
      createdAt: now,
    })
  );

  // Reject including @DocumentId `id` field
  await assertFails(
    txCol.doc("tx_with_id").set({
      id: "tx_with_id",
      userId: ALICE_UID,
      customerName: "Rahat",
      accountNo: "SKY-1234",
      txType: "DEPOSIT",
      amountBdt: 1000,
      commissionBdt: 100,
      dayNumber: 1,
      success: true,
      createdAt: now,
    })
  );
});

test("Cross-user isolation blocks Bob from reading or writing Alice's bank", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now)));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ cashBdt: 0, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});
