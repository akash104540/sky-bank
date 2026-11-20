package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffects
import com.example.data.BangladeshiCustomerFactory
import com.example.data.BankProfile
import com.example.data.BankRepository
import com.example.data.BankTransaction
import com.example.data.CustomerRequest
import com.example.data.TransactionType
import com.example.game3d.InteractableType
import com.example.game3d.Math3D
import com.example.game3d.ProjectedMarker
import com.example.game3d.TaskTarget
import com.example.game3d.WorldRenderSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Ready(val profile: BankProfile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

enum class DeskStage {
    VERIFY_NID,
    ENTER_AMOUNT
}

data class GameHudState(
    val profile: BankProfile = BankProfile(),
    val isProfileLoaded: Boolean = false,
    val bengali: Boolean = true,
    val soundEnabled: Boolean = true,
    val cameraSensitivity: Float = 1.0f,
    val showCutscene: Boolean = false,
    val cutscenePhase: Int = 0,
    val showBankDealDialog: Boolean = false,
    val isUpgrading: Boolean = false,
    val upgradeProgress: Float = 0f,
    val isSeatedAtDesk: Boolean = false,
    val deskStage: DeskStage = DeskStage.VERIFY_NID,
    val activeCustomer: CustomerRequest? = null,
    val customerQueue: List<CustomerRequest> = emptyList(),
    val keypadInput: String = "",
    val feedbackBanner: String? = null,
    val feedbackIsPositive: Boolean = true,
    val showShopDialog: Boolean = false,
    val showLedgerDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showEndOfDayDialog: Boolean = false,
    val dayBonusEarned: Long = 0L,
    val isSavingCloud: Boolean = false,
    val cloudError: String? = null,
    val worldSnapshot: WorldRenderSnapshot = WorldRenderSnapshot(),
    val projectedMarkers: List<ProjectedMarker> = emptyList()
)

class GameViewModel(
    private val repository: BankRepository,
    private val currentUserId: String,
    private val userDisplayName: String
) : ViewModel() {

    val soundEffects = SoundEffects()

    private val _hudState = MutableStateFlow(GameHudState())
    val hudState: StateFlow<GameHudState> = _hudState.asStateFlow()

    private val _profileLoadState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileLoadState: StateFlow<ProfileUiState> = _profileLoadState.asStateFlow()

    val transactionsState: StateFlow<List<BankTransaction>> = repository.observeTransactions(currentUserId)
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing transactions", fsErr ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private var playerX = 0f
    private var playerY = 1.68f
    private var playerZ = 9.2f
    private var yawDeg = 0f
    private var pitchDeg = -4f
    private var joyX = 0f
    private var joyY = 0f
    private var initializedFromServer = false
    private var tickCounter = 0

    init {
        observeUserProfile()
        startGameLoop()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            repository.observeBankProfile(currentUserId)
                .map<BankProfile?, ProfileUiState> { profile ->
                    if (profile == null) {
                        val created = repository.createInitialProfile(currentUserId, userDisplayName)
                        created.fold(
                            onSuccess = { ProfileUiState.Ready(it) },
                            onFailure = { ProfileUiState.Error(it.localizedMessage ?: "প্রোফাইল তৈরি করা যায়নি") }
                        )
                    } else {
                        ProfileUiState.Ready(profile)
                    }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val root = fsErr ?: error
                    Log.w(TAG, "Profile stream error", root)
                    emit(ProfileUiState.Error(root.localizedMessage ?: "ডাটাবেস লোড করা যায়নি"))
                }
                .collectLatest { state ->
                    _profileLoadState.value = state
                    if (state is ProfileUiState.Ready && !initializedFromServer) {
                        initializedFromServer = true
                        val loaded = state.profile
                        if (loaded.bankOwned) {
                            playerX = 0f
                            playerZ = 2.0f
                        } else {
                            playerX = 0f
                            playerZ = 9.2f
                        }
                        _hudState.update {
                            it.copy(
                                profile = loaded,
                                isProfileLoaded = true,
                                showCutscene = !loaded.introSeen,
                                cutscenePhase = 0
                            )
                        }
                        syncWorldSnapshot()
                    }
                }
        }
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (true) {
                delay(33L) // ~30 FPS simulation tick
                val state = _hudState.value
                if (!state.isProfileLoaded) continue

                tickCounter++

                // Movement physics when exploring
                if (!state.showCutscene && !state.isSeatedAtDesk && !state.showBankDealDialog && !state.isUpgrading) {
                    if (joyX != 0f || joyY != 0f) {
                        val (fx, fz) = Math3D.forwardXZ(yawDeg)
                        val (rx, rz) = Math3D.rightXZ(yawDeg)
                        val speed = 0.14f
                        val moveX = (fx * (-joyY) + rx * joyX) * speed
                        val moveZ = (fz * (-joyY) + rz * joyX) * speed
                        attemptMove(moveX, moveZ, state.profile.bankOwned)
                    }
                }

                // In-game clock & passive income (every 30 ticks ~ 1 second)
                if (tickCounter % 30 == 0 && !state.showCutscene && !state.showEndOfDayDialog) {
                    var p = state.profile
                    var dirty = false

                    if (p.bankOwned && p.minuteOfDay < 1320L) {
                        p = p.copy(minuteOfDay = p.minuteOfDay + 2L)
                        dirty = true
                    }

                    // Passive ATM Fee income every 6 seconds
                    if (p.atmActive && p.bankOpen && tickCounter % 180 == 0) {
                        val atmFee = 250L * p.vaultLevel
                        p = p.copy(
                            cashBdt = p.cashBdt + atmFee,
                            dailyRevenueBdt = p.dailyRevenueBdt + atmFee
                        )
                        dirty = true
                    }

                    // Passive Extra Counter Clerks income every 9 seconds
                    if (p.counterCount > 1L && p.bankOpen && tickCounter % 270 == 0) {
                        val clerkIncome = (p.counterCount - 1L) * 450L
                        p = p.copy(
                            cashBdt = p.cashBdt + clerkIncome,
                            dailyRevenueBdt = p.dailyRevenueBdt + clerkIncome
                        )
                        dirty = true
                    }

                    // Spawn queue customers when bank is OPEN
                    if (p.bankOwned && p.bankOpen && state.customerQueue.size < 4 && tickCounter % 90 == 0) {
                        val nextCust = BangladeshiCustomerFactory.nextCustomer(
                            level = p.level.toInt(),
                            carShowroomUnlocked = p.carShowroomUnlocked,
                            bengali = state.bengali
                        )
                        _hudState.update { hud ->
                            val newQ = hud.customerQueue + nextCust
                            hud.copy(
                                customerQueue = newQ,
                                activeCustomer = hud.activeCustomer ?: newQ.firstOrNull()
                            )
                        }
                    }

                    if (dirty) {
                        _hudState.update { it.copy(profile = p) }
                        if (p.minuteOfDay >= 1320L) {
                            triggerEndOfDay()
                        } else if (tickCounter % 300 == 0) {
                            persistProfile(p)
                        }
                    }
                }

                syncWorldSnapshot()
            }
        }
    }

    private fun attemptMove(dx: Float, dz: Float, bankOwned: Boolean) {
        val nextX = (playerX + dx).coerceIn(-15f, 15f)
        val nextZ = (playerZ + dz).coerceIn(-8.6f, 15.5f)

        // Check front bank wall collision at z in [3.55, 4.55]
        val crossingWall = (playerZ >= 4.55f && nextZ < 4.55f) ||
            (playerZ <= 3.55f && nextZ > 3.55f) ||
            (nextZ in 3.55f..4.55f)
        val insideDoorway = nextX in -2.0f..2.0f && bankOwned

        if (!crossingWall || insideDoorway) {
            // Also keep outside main teller desk rectangle
            val hitDesk = nextX in -1.6f..1.2f && nextZ in -4.35f..-3.25f
            if (!hitDesk) {
                playerX = nextX
                playerZ = nextZ
            }
        } else {
            // Slide along X if blocked by wall
            val hitDesk = nextX in -1.6f..1.2f && playerZ in -4.35f..-3.25f
            if (!hitDesk) {
                playerX = nextX
            }
        }
    }

    private fun computeTargetedInteractable(state: GameHudState): InteractableType? {
        val p = state.profile
        if (!p.bankOwned) {
            if (Math3D.distance2D(playerX, playerZ, -1.8f, 6.2f) < 3.6f) {
                return InteractableType.OWNER
            }
            return null
        }
        if (!p.bankUpgraded && Math3D.distance2D(playerX, playerZ, 2.6f, -1.6f) < 2.3f) {
            return InteractableType.UPGRADE_PAD
        }
        if (Math3D.distance2D(playerX, playerZ, 6.4f, 2.0f) < 2.6f) {
            return InteractableType.ATM_MACHINE
        }
        if (Math3D.distance2D(playerX, playerZ, -3.4f, 4.12f) < 3.0f ||
            Math3D.distance2D(playerX, playerZ, -1.8f, 4.12f) < 2.4f
        ) {
            return InteractableType.DOOR_SIGN
        }
        if (Math3D.distance2D(playerX, playerZ, -0.2f, -3.8f) < 3.2f) {
            return InteractableType.TELLER_DESK
        }
        if (Math3D.distance2D(playerX, playerZ, -6.4f, -1.5f) < 2.9f) {
            return InteractableType.CAR_SHOWROOM
        }
        return null
    }

    private fun computeActiveTaskTarget(p: BankProfile): TaskTarget {
        return when {
            !p.bankOwned -> TaskTarget.OWNER
            !p.bankUpgraded -> TaskTarget.UPGRADE_PAD
            !p.atmActive -> TaskTarget.ATM_MACHINE
            !p.bankOpen -> TaskTarget.DOOR_SIGN
            else -> TaskTarget.TELLER_DESK
        }
    }

    private fun syncWorldSnapshot() {
        val state = _hudState.value
        val p = state.profile
        val activeCust = state.activeCustomer
        val takaStacks = ((p.dailyRevenueBdt / 4000L) + 2L).toInt().coerceIn(1, 12)
        val targeted = computeTargetedInteractable(state)
        val taskTarget = computeActiveTaskTarget(p)

        _hudState.update {
            it.copy(
                worldSnapshot = WorldRenderSnapshot(
                    playerX = playerX,
                    playerY = playerY,
                    playerZ = playerZ,
                    yawDeg = yawDeg,
                    pitchDeg = pitchDeg,
                    isCutscene = state.showCutscene,
                    cutscenePhase = state.cutscenePhase,
                    isSeatedAtDesk = state.isSeatedAtDesk,
                    bankOwned = p.bankOwned,
                    bankUpgraded = p.bankUpgraded,
                    isUpgrading = state.isUpgrading,
                    atmActive = p.atmActive,
                    bankOpen = p.bankOpen,
                    carShowroomUnlocked = p.carShowroomUnlocked,
                    counterCount = p.counterCount.toInt(),
                    takaStacksOnTray = takaStacks,
                    activeTaskTarget = taskTarget,
                    targetedInteractable = targeted,
                    hasActiveCustomer = activeCust != null,
                    customerShirtColor = activeCust?.shirtColor ?: 0xFFE63946L,
                    customerHairColor = activeCust?.hairColor ?: 0xFF1A1A1AL,
                    queueCount = state.customerQueue.size,
                    isNightTime = p.minuteOfDay >= 1110L
                )
            )
        }
    }

    fun onJoystickMove(x: Float, y: Float) {
        joyX = x
        joyY = y
    }

    fun onCameraDelta(deltaYaw: Float, deltaPitch: Float) {
        val state = _hudState.value
        if (state.showCutscene || state.isSeatedAtDesk) return
        yawDeg = (yawDeg + deltaYaw) % 360f
        pitchDeg = (pitchDeg + deltaPitch).coerceIn(-38f, 38f)
    }

    fun onMarkersUpdated(markers: List<ProjectedMarker>) {
        _hudState.update { it.copy(projectedMarkers = markers) }
    }

    fun advanceCutscene() {
        soundEffects.playClick()
        val phase = _hudState.value.cutscenePhase
        if (phase == 0) {
            _hudState.update { it.copy(cutscenePhase = 1) }
        } else {
            finishCutscene()
        }
    }

    fun finishCutscene() {
        soundEffects.playClick()
        val updated = _hudState.value.profile.copy(introSeen = true)
        _hudState.update {
            it.copy(
                profile = updated,
                showCutscene = false
            )
        }
        persistProfile(updated)
    }

    fun onInteractTapped() {
        val state = _hudState.value
        val target = state.worldSnapshot.targetedInteractable ?: return
        soundEffects.playClick()
        when (target) {
            InteractableType.OWNER -> {
                _hudState.update { it.copy(showBankDealDialog = true) }
            }
            InteractableType.UPGRADE_PAD -> {
                startBankUpgrade()
            }
            InteractableType.ATM_MACHINE -> {
                activateAtm()
            }
            InteractableType.DOOR_SIGN -> {
                toggleBankOpen()
            }
            InteractableType.TELLER_DESK -> {
                sitAtTellerDesk()
            }
            InteractableType.CAR_SHOWROOM -> {
                _hudState.update { it.copy(showShopDialog = true) }
            }
        }
    }

    fun acceptBankDeal(priceBdt: Long) {
        val state = _hudState.value
        val p = state.profile
        if (p.cashBdt < priceBdt) {
            showBanner(if (state.bengali) "পর্যাপ্ত টাকা নেই!" else "Not enough cash!", false)
            soundEffects.playFraudAlert()
            return
        }
        soundEffects.playUpgradeFanfare()
        val updated = p.copy(
            cashBdt = p.cashBdt - priceBdt,
            bankOwned = true
        )
        _hudState.update {
            it.copy(
                profile = updated,
                showBankDealDialog = false
            )
        }
        showBanner(
            if (state.bengali) "অভিনন্দন! আপনি এখন ইস্কাই ব্যাংকের মালিক!"
            else "Congratulations! You now own Sky Bank!",
            true
        )
        persistProfile(updated)
        logTx("ব্যাংক ক্রয়", "SKY-DEAL", TransactionType.UPGRADE, priceBdt, 0L, true)
    }

    fun dismissBankDeal() {
        soundEffects.playClick()
        _hudState.update { it.copy(showBankDealDialog = false) }
    }

    private fun startBankUpgrade() {
        val state = _hudState.value
        if (state.isUpgrading || state.profile.bankUpgraded) return
        soundEffects.playClick()
        _hudState.update { it.copy(isUpgrading = true, upgradeProgress = 0f) }
        viewModelScope.launch {
            for (step in 1..25) {
                delay(80L)
                _hudState.update { it.copy(upgradeProgress = step / 25f) }
            }
            val cur = _hudState.value.profile
            val updated = cur.copy(
                bankUpgraded = true,
                xp = cur.xp + 40L
            ).withLevelCheck()
            soundEffects.playUpgradeFanfare()
            _hudState.update {
                it.copy(
                    profile = updated,
                    isUpgrading = false,
                    upgradeProgress = 1f
                )
            }
            showBanner(
                if (_hudState.value.bengali) "ব্যাংক সংস্কার সম্পন্ন হয়েছে!"
                else "Bank renovation complete!",
                true
            )
            persistProfile(updated)
        }
    }

    private fun activateAtm() {
        val state = _hudState.value
        val p = state.profile
        if (p.atmActive) {
            showBanner(
                if (state.bengali) "এটিএম চালু আছে — অটো ইনকাম চলছে!"
                else "ATM is running — passive fees active!",
                true
            )
            return
        }
        soundEffects.playUpgradeFanfare()
        val updated = p.copy(atmActive = true, xp = p.xp + 25L).withLevelCheck()
        _hudState.update { it.copy(profile = updated) }
        showBanner(
            if (state.bengali) "এটিএম মেশিন চালু হয়েছে!" else "ATM machine turned ON!",
            true
        )
        persistProfile(updated)
    }

    fun toggleBankOpen() {
        val state = _hudState.value
        val p = state.profile
        if (!p.bankOwned) return
        val nowOpen = !p.bankOpen
        soundEffects.playClick()
        val updated = p.copy(bankOpen = nowOpen)
        val newQueue = if (nowOpen && state.customerQueue.isEmpty()) {
            listOf(
                BangladeshiCustomerFactory.nextCustomer(p.level.toInt(), p.carShowroomUnlocked, state.bengali),
                BangladeshiCustomerFactory.nextCustomer(p.level.toInt(), p.carShowroomUnlocked, state.bengali)
            )
        } else {
            state.customerQueue
        }
        _hudState.update {
            it.copy(
                profile = updated,
                customerQueue = newQueue,
                activeCustomer = it.activeCustomer ?: newQueue.firstOrNull()
            )
        }
        showBanner(
            if (nowOpen) {
                if (state.bengali) "ব্যাংক খোলা হয়েছে! গ্রাহকরা প্রবেশ করছেন" else "Bank is now OPEN!"
            } else {
                if (state.bengali) "ব্যাংক বন্ধ করা হয়েছে" else "Bank is CLOSED"
            },
            nowOpen
        )
        persistProfile(updated)
    }

    fun sitAtTellerDesk() {
        val state = _hudState.value
        if (!state.profile.bankOwned) return
        soundEffects.playClick()
        var queue = state.customerQueue
        if (state.profile.bankOpen && queue.isEmpty()) {
            queue = listOf(
                BangladeshiCustomerFactory.nextCustomer(
                    state.profile.level.toInt(),
                    state.profile.carShowroomUnlocked,
                    state.bengali
                )
            )
        }
        _hudState.update {
            it.copy(
                isSeatedAtDesk = true,
                deskStage = DeskStage.VERIFY_NID,
                keypadInput = "",
                customerQueue = queue,
                activeCustomer = it.activeCustomer ?: queue.firstOrNull()
            )
        }
    }

    fun standUpFromDesk() {
        soundEffects.playClick()
        _hudState.update {
            it.copy(
                isSeatedAtDesk = false,
                keypadInput = ""
            )
        }
    }

    fun verifyCustomerIdentity(playerSaysMatch: Boolean) {
        val state = _hudState.value
        val cust = state.activeCustomer ?: return
        val p = state.profile

        if (p.energy <= 0L) {
            soundEffects.playFraudAlert()
            showBanner(
                if (state.bengali) "এনার্জি শেষ! দোকান থেকে চা-নাস্তা কিনুন বা দিন শেষ করুন।"
                else "Out of energy! Refill in Shop or end day.",
                false
            )
            return
        }

        if (cust.isFraud) {
            if (!playerSaysMatch) {
                // Correctly caught fraudster!
                soundEffects.playCashSuccess()
                val bonus = 1500L
                val updated = p.copy(
                    cashBdt = p.cashBdt + bonus,
                    dailyRevenueBdt = p.dailyRevenueBdt + bonus,
                    fraudCaught = p.fraudCaught + 1L,
                    energy = (p.energy - 1L).coerceAtLeast(0L),
                    xp = p.xp + 35L
                ).withLevelCheck()
                advanceToNextCustomer(updated)
                showBanner(
                    if (state.bengali) "ভুয়া এনআইডি শনাক্ত! বোনাস +৳১,৫০০"
                    else "Fraud NID caught! Bonus +৳1,500",
                    true
                )
                persistProfile(updated)
                logTx(cust.portalName, cust.portalAccountNo, TransactionType.FRAUD_REJECTED, cust.amountBdt, bonus, true)
            } else {
                // Mistakenly approved fraudster!
                soundEffects.playFraudAlert()
                val penalty = 1200L
                val updated = p.copy(
                    cashBdt = (p.cashBdt - penalty).coerceAtLeast(0L),
                    energy = (p.energy - 1L).coerceAtLeast(0L)
                )
                advanceToNextCustomer(updated)
                showBanner(
                    if (state.bengali) "সতর্কতা! ভুয়া গ্রাহক অনুমোদনে -৳১,২০০ জরিমানা!"
                    else "Fraud approved! -৳1,200 penalty!",
                    false
                )
                persistProfile(updated)
                logTx(cust.portalName, cust.portalAccountNo, TransactionType.FRAUD_REJECTED, penalty, 0L, false)
            }
        } else {
            if (playerSaysMatch) {
                // Genuine customer verified -> move to keypad transaction!
                soundEffects.playClick()
                _hudState.update {
                    it.copy(
                        deskStage = DeskStage.ENTER_AMOUNT,
                        keypadInput = ""
                    )
                }
            } else {
                // Mistakenly rejected a real customer!
                soundEffects.playFraudAlert()
                val updated = p.copy(energy = (p.energy - 1L).coerceAtLeast(0L))
                advanceToNextCustomer(updated)
                showBanner(
                    if (state.bengali) "ভুল সিদ্ধান্ত! আসল গ্রাহক বিরক্ত হয়ে চলে গেছেন।"
                    else "Genuine customer rejected by mistake!",
                    false
                )
                persistProfile(updated)
            }
        }
    }

    fun onKeypadPress(key: String) {
        soundEffects.playClick()
        _hudState.update { state ->
            val next = when (key) {
                "C" -> ""
                "." -> if (state.keypadInput.contains(".")) state.keypadInput else state.keypadInput + "."
                else -> if (state.keypadInput.length < 8) state.keypadInput + key else state.keypadInput
            }
            state.copy(keypadInput = next)
        }
    }

    fun submitKeypadAmount() {
        val state = _hudState.value
        val cust = state.activeCustomer ?: return
        val p = state.profile
        val entered = state.keypadInput.toDoubleOrNull()?.toLong() ?: -1L

        if (entered != cust.amountBdt) {
            soundEffects.playFraudAlert()
            showBanner(
                if (state.bengali) "সঠিক পরিমাণ লিখুন: ${AppStrings.formatBdt(cust.amountBdt, true)}"
                else "Enter exact amount: ${AppStrings.formatBdt(cust.amountBdt, false)}",
                false
            )
            return
        }

        soundEffects.playCashSuccess()
        val profit = cust.commissionBdt
        val updated = p.copy(
            cashBdt = p.cashBdt + profit,
            dailyRevenueBdt = p.dailyRevenueBdt + profit,
            customersServed = p.customersServed + 1L,
            energy = (p.energy - 1L).coerceAtLeast(0L),
            xp = p.xp + 30L
        ).withLevelCheck()

        advanceToNextCustomer(updated)
        showBanner(
            if (state.bengali) "লেনদেন সফল! কমিশন +${AppStrings.formatBdt(profit, true)}"
            else "Transaction complete! +${AppStrings.formatBdt(profit, false)}",
            true
        )
        persistProfile(updated)
        logTx(cust.portalName, cust.portalAccountNo, cust.txType, cust.amountBdt, profit, true)
    }

    private fun advanceToNextCustomer(updatedProfile: BankProfile) {
        _hudState.update { state ->
            val remaining = if (state.customerQueue.isNotEmpty()) state.customerQueue.drop(1) else emptyList()
            val refilled = if (updatedProfile.bankOpen && remaining.isEmpty()) {
                listOf(
                    BangladeshiCustomerFactory.nextCustomer(
                        updatedProfile.level.toInt(),
                        updatedProfile.carShowroomUnlocked,
                        state.bengali
                    )
                )
            } else {
                remaining
            }
            state.copy(
                profile = updatedProfile,
                customerQueue = refilled,
                activeCustomer = refilled.firstOrNull(),
                deskStage = DeskStage.VERIFY_NID,
                keypadInput = ""
            )
        }
    }

    // Shop actions
    fun openShop() {
        soundEffects.playClick()
        _hudState.update { it.copy(showShopDialog = true) }
    }

    fun closeShop() {
        soundEffects.playClick()
        _hudState.update { it.copy(showShopDialog = false) }
    }

    fun buyEnergyRefill() {
        val state = _hudState.value
        val p = state.profile
        val cost = 500L
        if (p.cashBdt < cost) {
            soundEffects.playFraudAlert()
            showBanner(if (state.bengali) "পর্যাপ্ত টাকা নেই!" else "Not enough cash!", false)
            return
        }
        soundEffects.playCashSuccess()
        val updated = p.copy(
            cashBdt = p.cashBdt - cost,
            energy = (p.energy + 15L).coerceAtMost(p.maxEnergy)
        )
        _hudState.update { it.copy(profile = updated) }
        showBanner(if (state.bengali) "এনার্জি +১৫ রিফিল হয়েছে!" else "Energy +15 restored!", true)
        persistProfile(updated)
    }

    fun buyExtraCounter() {
        val state = _hudState.value
        val p = state.profile
        if (p.counterCount >= 4L) return
        val cost = p.counterCount * 25000L
        if (p.cashBdt < cost) {
            soundEffects.playFraudAlert()
            showBanner(if (state.bengali) "পর্যাপ্ত টাকা নেই!" else "Not enough cash!", false)
            return
        }
        soundEffects.playUpgradeFanfare()
        val updated = p.copy(
            cashBdt = p.cashBdt - cost,
            counterCount = p.counterCount + 1L,
            xp = p.xp + 60L
        ).withLevelCheck()
        _hudState.update { it.copy(profile = updated) }
        showBanner(if (state.bengali) "নতুন টেলার কাউন্টার চালু!" else "New Teller Counter unlocked!", true)
        persistProfile(updated)
        logTx("টেলার কাউন্টার", "SKY-UPG", TransactionType.UPGRADE, cost, 0L, true)
    }

    fun buyVaultUpgrade() {
        val state = _hudState.value
        val p = state.profile
        if (p.vaultLevel >= 5L) return
        val cost = p.vaultLevel * 30000L
        if (p.cashBdt < cost) {
            soundEffects.playFraudAlert()
            showBanner(if (state.bengali) "পর্যাপ্ত টাকা নেই!" else "Not enough cash!", false)
            return
        }
        soundEffects.playUpgradeFanfare()
        val updated = p.copy(
            cashBdt = p.cashBdt - cost,
            vaultLevel = p.vaultLevel + 1L,
            dailyTargetBdt = p.dailyTargetBdt + 20000L,
            xp = p.xp + 75L
        ).withLevelCheck()
        _hudState.update { it.copy(profile = updated) }
        showBanner(if (state.bengali) "ভল্ট লেভেল ${updated.vaultLevel} আপগ্রেড!" else "Vault upgraded to Lv.${updated.vaultLevel}!", true)
        persistProfile(updated)
        logTx("ভল্ট আপগ্রেড", "SKY-VLT", TransactionType.UPGRADE, cost, 0L, true)
    }

    fun unlockCarShowroom() {
        val state = _hudState.value
        val p = state.profile
        if (p.carShowroomUnlocked) return
        val cost = 75000L
        if (p.cashBdt < cost) {
            soundEffects.playFraudAlert()
            showBanner(if (state.bengali) "পর্যাপ্ত টাকা নেই!" else "Not enough cash!", false)
            return
        }
        soundEffects.playUpgradeFanfare()
        val updated = p.copy(
            cashBdt = p.cashBdt - cost,
            carShowroomUnlocked = true,
            xp = p.xp + 120L
        ).withLevelCheck()
        _hudState.update { it.copy(profile = updated) }
        showBanner(if (state.bengali) "কার ফাইন্যান্স শোরুম আনলক হয়েছে!" else "Car Finance Showroom unlocked!", true)
        persistProfile(updated)
        logTx("কার শোরুম", "SKY-CAR", TransactionType.UPGRADE, cost, 0L, true)
    }

    fun payDebtInstallment() {
        val state = _hudState.value
        val p = state.profile
        if (p.debtBdt <= 0L) return
        val pay = minOf(50000L, p.debtBdt)
        if (p.cashBdt < pay) {
            soundEffects.playFraudAlert()
            showBanner(if (state.bengali) "পর্যাপ্ত টাকা নেই!" else "Not enough cash!", false)
            return
        }
        soundEffects.playCashSuccess()
        val updated = p.copy(
            cashBdt = p.cashBdt - pay,
            debtBdt = (p.debtBdt - pay).coerceAtLeast(0L),
            xp = p.xp + 80L
        ).withLevelCheck()
        _hudState.update { it.copy(profile = updated) }
        showBanner(
            if (updated.debtBdt == 0L) {
                if (state.bengali) "অভিনন্দন! ব্যাংকের সব ঋণ পরিশোধ হয়েছে!" else "All bank debt cleared!"
            } else {
                if (state.bengali) "ঋণের কিস্তি ${AppStrings.formatBdt(pay, true)} পরিশোধিত!"
                else "Paid ${AppStrings.formatBdt(pay, false)} toward debt!"
            },
            true
        )
        persistProfile(updated)
        logTx("ঋণ পরিশোধ", "SKY-DEBT", TransactionType.DEBT_PAYMENT, pay, 0L, true)
    }

    fun triggerEndOfDay() {
        val state = _hudState.value
        val p = state.profile
        val bonus = if (p.dailyRevenueBdt >= p.dailyTargetBdt) 5000L * p.level else 0L
        _hudState.update {
            it.copy(
                showEndOfDayDialog = true,
                dayBonusEarned = bonus,
                isSeatedAtDesk = false
            )
        }
    }

    fun startNextDay() {
        val state = _hudState.value
        val p = state.profile
        val bonus = state.dayBonusEarned
        soundEffects.playUpgradeFanfare()
        val updated = p.copy(
            dayNumber = p.dayNumber + 1L,
            minuteOfDay = 540L, // 09:00 AM
            cashBdt = p.cashBdt + bonus,
            energy = p.maxEnergy,
            dailyRevenueBdt = 0L,
            dailyTargetBdt = (60000L + (p.dayNumber) * 10000L).coerceAtMost(500000L)
        )
        _hudState.update {
            it.copy(
                profile = updated,
                showEndOfDayDialog = false,
                dayBonusEarned = 0L
            )
        }
        persistProfile(updated)
    }

    fun toggleLedgerDialog(show: Boolean) {
        soundEffects.playClick()
        _hudState.update { it.copy(showLedgerDialog = show) }
    }

    fun toggleSettingsDialog(show: Boolean) {
        soundEffects.playClick()
        _hudState.update { it.copy(showSettingsDialog = show) }
    }

    fun toggleLanguage() {
        soundEffects.playClick()
        _hudState.update { it.copy(bengali = !it.bengali) }
    }

    fun toggleSound() {
        val next = !_hudState.value.soundEnabled
        soundEffects.enabled = next
        _hudState.update { it.copy(soundEnabled = next) }
        if (next) soundEffects.playClick()
    }

    fun setCameraSensitivity(sens: Float) {
        _hudState.update { it.copy(cameraSensitivity = sens) }
    }

    fun clearCloudError() {
        _hudState.update { it.copy(cloudError = null) }
    }

    fun manualSaveToCloud() {
        soundEffects.playClick()
        persistProfile(_hudState.value.profile, showSuccessBanner = true)
    }

    private fun BankProfile.withLevelCheck(): BankProfile {
        var curLevel = level
        var curXp = xp
        var curMaxEnergy = maxEnergy
        while (curXp >= curLevel * 100L && curLevel < 100L) {
            curXp -= curLevel * 100L
            curLevel++
            curMaxEnergy = (curMaxEnergy + 5L).coerceAtMost(100L)
        }
        return copy(
            level = curLevel,
            xp = curXp,
            maxEnergy = curMaxEnergy,
            energy = if (curLevel > level) curMaxEnergy else energy
        )
    }

    private fun showBanner(msg: String, positive: Boolean) {
        _hudState.update { it.copy(feedbackBanner = msg, feedbackIsPositive = positive) }
        viewModelScope.launch {
            delay(2600L)
            _hudState.update { current ->
                if (current.feedbackBanner == msg) current.copy(feedbackBanner = null) else current
            }
        }
    }

    private fun persistProfile(profile: BankProfile, showSuccessBanner: Boolean = false) {
        viewModelScope.launch {
            _hudState.update { it.copy(isSavingCloud = true) }
            val result = repository.saveProfileProgress(currentUserId, profile)
            _hudState.update { state ->
                state.copy(
                    isSavingCloud = false,
                    cloudError = result.exceptionOrNull()?.localizedMessage
                )
            }
            if (result.isSuccess && showSuccessBanner) {
                showBanner(
                    if (_hudState.value.bengali) "ক্লাউডে সফলভাবে সেভ হয়েছে!" else "Saved to Cloud!",
                    true
                )
            }
        }
    }

    private fun logTx(
        customerName: String,
        accountNo: String,
        type: TransactionType,
        amount: Long,
        commission: Long,
        success: Boolean
    ) {
        val day = _hudState.value.profile.dayNumber
        viewModelScope.launch {
            repository.recordTransaction(
                userId = currentUserId,
                customerName = customerName,
                accountNo = accountNo,
                txType = type,
                amountBdt = amount,
                commissionBdt = commission,
                dayNumber = day,
                success = success
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundEffects.release()
    }

    private companion object {
        const val TAG = "SkyBankVM"
    }
}
