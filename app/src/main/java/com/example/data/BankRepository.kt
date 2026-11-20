package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BankRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeBankProfile(userId: String): Flow<BankProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        BankProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String): Flow<List<BankTransaction>> = flow {
        val colRef = db.collection("users").document(userId).collection("transactions")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BankTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun createInitialProfile(userId: String, displayName: String): Result<BankProfile> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val safeName = displayName.trim().ifBlank { "ম্যানেজার" }.take(60)
            val docRef = db.collection("users").document(uid)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existing = existingSnap.toObject(
                    BankProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) return Result.success(existing)
            }
            val initial = BankProfile(
                userId = uid,
                displayName = safeName,
                bankName = "ইস্কাই ব্যাংক"
            )
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "bankName" to initial.bankName,
                "cashBdt" to initial.cashBdt,
                "debtBdt" to initial.debtBdt,
                "energy" to initial.energy,
                "maxEnergy" to initial.maxEnergy,
                "level" to initial.level,
                "xp" to initial.xp,
                "dayNumber" to initial.dayNumber,
                "minuteOfDay" to initial.minuteOfDay,
                "bankOwned" to initial.bankOwned,
                "bankUpgraded" to initial.bankUpgraded,
                "atmActive" to initial.atmActive,
                "carShowroomUnlocked" to initial.carShowroomUnlocked,
                "vaultLevel" to initial.vaultLevel,
                "counterCount" to initial.counterCount,
                "dailyRevenueBdt" to initial.dailyRevenueBdt,
                "dailyTargetBdt" to initial.dailyTargetBdt,
                "customersServed" to initial.customersServed,
                "fraudCaught" to initial.fraudCaught,
                "bankOpen" to initial.bankOpen,
                "introSeen" to initial.introSeen,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(initial)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveProfileProgress(userId: String, profile: BankProfile): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val safeDisplayName = profile.displayName.trim().ifBlank { "ম্যানেজার" }.take(60)
            val safeBankName = profile.bankName.trim().ifBlank { "ইস্কাই ব্যাংক" }.take(50)
            val updates = mapOf(
                "displayName" to safeDisplayName,
                "bankName" to safeBankName,
                "cashBdt" to profile.cashBdt.coerceIn(0L, 999_999_999L),
                "debtBdt" to profile.debtBdt.coerceIn(0L, 999_999_999L),
                "energy" to profile.energy.coerceIn(0L, 200L),
                "maxEnergy" to profile.maxEnergy.coerceIn(10L, 200L),
                "level" to profile.level.coerceIn(1L, 100L),
                "xp" to profile.xp.coerceIn(0L, 10_000_000L),
                "dayNumber" to profile.dayNumber.coerceIn(1L, 99_999L),
                "minuteOfDay" to profile.minuteOfDay.coerceIn(0L, 1440L),
                "bankOwned" to profile.bankOwned,
                "bankUpgraded" to profile.bankUpgraded,
                "atmActive" to profile.atmActive,
                "carShowroomUnlocked" to profile.carShowroomUnlocked,
                "vaultLevel" to profile.vaultLevel.coerceIn(1L, 10L),
                "counterCount" to profile.counterCount.coerceIn(1L, 6L),
                "dailyRevenueBdt" to profile.dailyRevenueBdt.coerceIn(0L, 999_999_999L),
                "dailyTargetBdt" to profile.dailyTargetBdt.coerceIn(1000L, 999_999_999L),
                "customersServed" to profile.customersServed.coerceIn(0L, 9_999_999L),
                "fraudCaught" to profile.fraudCaught.coerceIn(0L, 9_999_999L),
                "bankOpen" to profile.bankOpen,
                "introSeen" to profile.introSeen,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordTransaction(
        userId: String,
        customerName: String,
        accountNo: String,
        txType: TransactionType,
        amountBdt: Long,
        commissionBdt: Long,
        dayNumber: Long,
        success: Boolean
    ): Result<Unit> {
        var docPath = "users/$userId/transactions"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid).collection("transactions").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "customerName" to customerName.trim().ifBlank { "গ্রাহক" }.take(60),
                "accountNo" to accountNo.trim().ifBlank { "SKY-0000" }.take(30),
                "txType" to txType.name,
                "amountBdt" to amountBdt.coerceIn(0L, 999_999_999L),
                "commissionBdt" to commissionBdt.coerceIn(0L, 999_999_999L),
                "dayNumber" to dayNumber.coerceIn(1L, 99_999L),
                "success" to success,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
