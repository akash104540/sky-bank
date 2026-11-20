package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomerRequest
import com.example.data.TransactionType
import com.example.game3d.InteractableType
import com.example.game3d.ProjectedMarker
import com.example.game3d.TaskTarget
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CharcoalDark
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.EnergyAmber
import com.example.ui.theme.MatchGreen
import com.example.ui.theme.MismatchRed
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.SkyEmeraldDark
import com.example.ui.theme.TakaGold
import com.example.ui.theme.TakaGoldLight
import com.example.ui.theme.WarmParchment
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun GameHudOverlay(
    hud: GameHudState,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val bn = hud.bengali
    val p = hud.profile

    Box(modifier = modifier.fillMaxSize()) {
        // 1. 3D-Projected Floating Badges
        ProjectedWorldMarkers(
            markers = hud.projectedMarkers,
            bengali = bn,
            bankOpen = p.bankOpen,
            debtBdt = p.debtBdt
        )

        // 2. Cutscene Overlay (Screenshots 1 & 2)
        if (hud.showCutscene) {
            CutsceneOverlay(
                phase = hud.cutscenePhase,
                debtBdt = p.debtBdt,
                bengali = bn,
                onNext = { viewModel.advanceCutscene() },
                onSkip = { viewModel.finishCutscene() }
            )
            return@Box
        }

        // 3. Main Gameplay HUD inside safe drawing insets
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Top HUD Row
            Column(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()) {
                TopStatusBar(
                    hud = hud,
                    onSettingsClick = { viewModel.toggleSettingsDialog(true) },
                    onLedgerClick = { viewModel.toggleLedgerDialog(true) },
                    onCashPlusClick = { viewModel.openShop() },
                    onEnergyPlusClick = { viewModel.openShop() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Shop Button (Screenshot 4)
                    Surface(
                        onClick = { viewModel.openShop() },
                        shape = RoundedCornerShape(12.dp),
                        color = TakaGoldLight,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .border(2.dp, Color(0xFFB45309), RoundedCornerShape(12.dp))
                            .testTag("shop_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = "Shop",
                                tint = CharcoalDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (bn) "দোকান" else "Shop",
                                    color = CharcoalDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    lineHeight = 15.sp
                                )
                                Text(
                                    text = if (bn) "আপগ্রেড কিনুন" else "Purchase Items",
                                    color = Color(0xFF451A03),
                                    fontSize = 10.sp,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    // Active Task Card on Right
                    TaskObjectiveChip(
                        taskTarget = hud.worldSnapshot.activeTaskTarget,
                        bengali = bn,
                        isSeated = hud.isSeatedAtDesk
                    )
                }
            }

            // Center Crosshair when walking in 3D
            if (!hud.isSeatedAtDesk && !hud.showBankDealDialog && !hud.isUpgrading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                        .border(1.dp, CharcoalDark, CircleShape)
                )
            }

            // Feedback Banner (Top-Center below HUD)
            AnimatedVisibility(
                visible = hud.feedbackBanner != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (hud.feedbackIsPositive) MatchGreen else MismatchRed,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = hud.feedbackBanner ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Upgrading Progress Bar (Screenshot 5)
            if (hud.isUpgrading) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CharcoalPanel,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp)
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (bn) "ব্যাংক আপগ্রেড হচ্ছে..." else "UPGRADING...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { hud.upgradeProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = CashGreen,
                            trackColor = Color(0xFF334155)
                        )
                    }
                }
            }

            // Bank Deal Negotiation Card (Screenshot 4)
            if (hud.showBankDealDialog) {
                BankDealCard(
                    bengali = bn,
                    onFairDeal = { viewModel.acceptBankDeal(450000L) },
                    onSmartDeal = { viewModel.acceptBankDeal(300000L) },
                    onDismiss = { viewModel.dismissBankDeal() },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            // Seated Teller Desk Mode (Screenshots 6, 7, 8)
            if (hud.isSeatedAtDesk) {
                SeatedTellerOverlay(
                    hud = hud,
                    viewModel = viewModel
                )
            } else if (!hud.showBankDealDialog && !hud.isUpgrading) {
                // Exploration Controls: Virtual Joystick (Bottom-Left) + Interact Button (Bottom-Right)
                VirtualJoystick(
                    onMove = { jx, jy -> viewModel.onJoystickMove(jx, jy) },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 8.dp)
                        .testTag("virtual_joystick")
                )

                val target = hud.worldSnapshot.targetedInteractable
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (p.bankOwned) {
                        Surface(
                            onClick = { viewModel.triggerEndOfDay() },
                            shape = RoundedCornerShape(12.dp),
                            color = CharcoalPanel,
                            modifier = Modifier
                                .border(1.dp, SkyCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .testTag("end_day_button")
                        ) {
                            Text(
                                text = if (bn) "দিন শেষ করুন" else "End Day",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    if (target != null) {
                        Button(
                            onClick = { viewModel.onInteractTapped() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MatchGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("interact_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = interactLabel(target, bn, p.bankOpen),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopStatusBar(
    hud: GameHudState,
    onSettingsClick: () -> Unit,
    onLedgerClick: () -> Unit,
    onCashPlusClick: () -> Unit,
    onEnergyPlusClick: () -> Unit
) {
    val bn = hud.bengali
    val p = hud.profile
    val xpNeeded = (p.level * 100L).coerceAtLeast(100L)
    val xpFrac = (p.xp.toFloat() / xpNeeded.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Blue Settings Gear
        Surface(
            onClick = onSettingsClick,
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0EA5E9),
            modifier = Modifier
                .size(42.dp)
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                .testTag("settings_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }

        // Ledger Book Button
        Surface(
            onClick = onLedgerClick,
            shape = RoundedCornerShape(10.dp),
            color = SkyEmeraldDark,
            modifier = Modifier
                .size(42.dp)
                .border(1.5.dp, TakaGold.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                .testTag("ledger_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ReceiptLong, contentDescription = "Ledger", tint = TakaGoldLight)
            }
        }

        // Cash Pill
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CharcoalPanel,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Payments, contentDescription = null, tint = CashGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = AppStrings.formatBdt(p.cashBdt, bn),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CashGreen)
                        .clickable { onCashPlusClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Cash", tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }

        // Energy Pill
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CharcoalPanel,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = EnergyAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(3.dp))
                val energyStr = if (bn) AppStrings.toBengaliDigits("${p.energy}") else "${p.energy}"
                Text(
                    text = energyStr,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CashGreen)
                        .clickable { onEnergyPlusClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Refill Energy", tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }

        // Level + XP Pill
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CharcoalPanel,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = EnergyAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.width(56.dp)) {
                    val lvlTxt = if (bn) "লেভেল ${AppStrings.toBengaliDigits("${p.level}")}" else "Lv.${p.level}"
                    Text(lvlTxt, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    LinearProgressIndicator(
                        progress = { xpFrac },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = EnergyAmber,
                        trackColor = Color(0xFF334155)
                    )
                }
            }
        }

        // Day & Clock Calendar Badge
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            modifier = Modifier.border(1.5.dp, Color(0xFFDC2626), RoundedCornerShape(10.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                val dayLbl = if (bn) "দিন ${AppStrings.toBengaliDigits("${p.dayNumber}")}" else "Day ${p.dayNumber}"
                Text(
                    text = dayLbl,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFFDC2626), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
                Text(
                    text = AppStrings.formatClock(p.minuteOfDay, bn),
                    color = CharcoalDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TaskObjectiveChip(
    taskTarget: TaskTarget,
    bengali: Boolean,
    isSeated: Boolean
) {
    val label = when (taskTarget) {
        TaskTarget.OWNER -> if (bengali) "মালিকের সাথে কথা বলুন" else "Meet The Owner"
        TaskTarget.UPGRADE_PAD -> if (bengali) "ব্যাংক আপগ্রেড করুন" else "Upgrade The Bank"
        TaskTarget.ATM_MACHINE -> if (bengali) "এটিএম মেশিন চালু করুন" else "Turn on ATM Machine"
        TaskTarget.DOOR_SIGN -> if (bengali) "দরজার সাইন OPEN করুন" else "Open Bank Door"
        TaskTarget.TELLER_DESK -> if (isSeated) {
            if (bengali) "গ্রাহকদের সেবা দিন" else "Deal With Customers"
        } else {
            if (bengali) "টেলার ডেস্কে বসুন" else "Go To Teller Desk"
        }
        TaskTarget.CAR_SHOWROOM -> if (bengali) "কার শোরুম দেখুন" else "Visit Car Showroom"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xD96B4F35),
        modifier = Modifier.border(1.5.dp, Color(0xFFD6B588), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(
                text = if (bengali) "কাজ (TASK)" else "TASK",
                color = TakaGoldLight,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CutsceneOverlay(
    phase: Int,
    debtBdt: Long,
    bengali: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        // Bottom Cinematic Subtitle Bar (Screenshots 1 & 2)
        Surface(
            color = Color(0xCC090D12),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val caption = if (phase == 0) {
                    if (bengali) "ঋণের বোঝা নিয়ন্ত্রণের বাইরে চলে গেছে..." else "Debt has piled up beyond control."
                } else {
                    if (bengali) "মোট বকেয়া ঋণ: ${AppStrings.formatBdt(debtBdt, true)}"
                    else "Total Outstanding Debt: ${AppStrings.formatBdt(debtBdt, false)}"
                }
                Text(
                    text = caption,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSkip,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text(if (bengali) "এড়িয়ে যান" else "Skip", color = Color.White)
                    }
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(containerColor = TakaGold),
                        modifier = Modifier.testTag("cutscene_next_button")
                    ) {
                        Text(
                            text = if (phase == 0) {
                                if (bengali) "পরবর্তী" else "Next"
                            } else {
                                if (bengali) "শুরু করুন" else "Play"
                            },
                            color = CharcoalDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BankDealCard(
    bengali: Boolean,
    onFairDeal: () -> Unit,
    onSmartDeal: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = WarmParchment,
        shadowElevation = 12.dp,
        modifier = modifier
            .widthIn(max = 310.dp)
            .border(2.dp, Color(0xFF8C6239), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (bengali) "ব্যাংক ডিল (Bank Deal)" else "Bank Deal",
                    color = Color(0xFF78350F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = CharcoalDark)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF8C5A32),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val lines = if (bengali) {
                        listOf(
                            "নমস্কার!",
                            "আমি এই ইস্কাই ব্যাংকের মালিক।",
                            "ব্যাংকটি পুরোপুরি চালু আছে।",
                            "আমি এটির জন্য ৳৫,০০,০০০ চাই।"
                        )
                    } else {
                        listOf(
                            "Hello.",
                            "I'm the owner of Sky Bank.",
                            "It's fully operational.",
                            "I want ৳5,00,000 for it."
                        )
                    }
                    lines.forEach { msg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = CharcoalDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onFairDeal,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("deal_fair_button")
                ) {
                    Text(
                        text = AppStrings.formatBdt(450000L, bengali),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                Button(
                    onClick = onSmartDeal,
                    colors = ButtonDefaults.buttonColors(containerColor = MatchGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("deal_smart_button")
                ) {
                    Text(
                        text = AppStrings.formatBdt(300000L, bengali),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SeatedTellerOverlay(
    hud: GameHudState,
    viewModel: GameViewModel
) {
    val bn = hud.bengali
    val p = hud.profile
    val cust = hud.activeCustomer

    Box(modifier = Modifier.fillMaxSize()) {
        // Daily Target Progress Bar (Screenshots 6-8)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xE65C4028),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .widthIn(max = 260.dp)
                .border(1.5.dp, TakaGold, RoundedCornerShape(10.dp))
        ) {
            val revFrac = (p.dailyRevenueBdt.toFloat() / p.dailyTargetBdt.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(revFrac)
                        .height(26.dp)
                        .background(MatchGreen)
                )
                Text(
                    text = "${AppStrings.formatBdt(p.dailyRevenueBdt, bn)} / ${AppStrings.formatBdt(p.dailyTargetBdt, bn)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Stand Up Button (Bottom-Left)
        Button(
            onClick = { viewModel.standUpFromDesk() },
            colors = ButtonDefaults.buttonColors(containerColor = CharcoalPanel),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 8.dp)
                .height(48.dp)
                .testTag("stand_up_button")
        ) {
            Icon(Icons.Filled.DirectionsWalk, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (bn) "চেয়ার ছাড়ুন" else "Stand Up", color = Color.White, fontWeight = FontWeight.Bold)
        }

        if (cust == null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CharcoalPanel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = if (!p.bankOpen) {
                        if (bn) "গ্রাহক পেতে সামনের দরজায় OPEN চাপুন!" else "Open the front door to invite customers!"
                    } else {
                        if (bn) "নতুন গ্রাহক আসছে, একটু অপেক্ষা করুন..." else "Waiting for next customer..."
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(14.dp)
                )
            }
            return
        }

        // Left-Center Customer Portal Screen Card (aligned cleanly for instant readability)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xF00B1117),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 40.dp)
                .widthIn(max = 265.dp)
                .border(2.dp, SkyCyan, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (bn) "ইস্কাই গ্রাহক পোর্টাল" else "Sky Customer Portal",
                    color = TakaGoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${if (bn) "নাম:" else "Name:"} ${cust.portalName}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (bn) "হিসাব:" else "Account:"} ${cust.portalAccountNo}",
                    color = Color(0xFFBAE6FD),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (hud.deskStage == DeskStage.VERIFY_NID) {
                    Text(
                        text = if (bn) "স্ট্যাটাস: এনআইডি যাচাই করুন" else "STATUS: Verify Identity",
                        color = CashGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                } else {
                    val txLabel = when (cust.txType) {
                        TransactionType.DEPOSIT -> if (bn) "জমা (Deposit)" else "Deposit"
                        TransactionType.WITHDRAW -> if (bn) "উত্তোলন (Withdraw)" else "Withdraw"
                        TransactionType.CAR_FINANCE -> if (bn) "কার ফাইন্যান্স" else "Car Finance"
                        else -> "Tx"
                    }
                    Text(
                        text = "$txLabel : ${AppStrings.formatBdt(cust.amountBdt, bn)}",
                        color = TakaGoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val enteredText = if (hud.keypadInput.isEmpty()) {
                        if (bn) "পরিমাণ লিখুন..." else "Enter Amount..."
                    } else {
                        val raw = hud.keypadInput
                        if (bn) "৳${AppStrings.toBengaliDigits(raw)}" else "৳$raw"
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = enteredText,
                            color = CashGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Right-Side NID Identity Card OR Numeric Keypad (Screenshots 7 & 8)
        if (hud.deskStage == DeskStage.VERIFY_NID) {
            IdentityCardPanel(
                cust = cust,
                bengali = bn,
                onMismatch = { viewModel.verifyCustomerIdentity(false) },
                onMatch = { viewModel.verifyCustomerIdentity(true) },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        } else {
            NumericKeypadPanel(
                bengali = bn,
                onKey = { viewModel.onKeypadPress(it) },
                onSubmit = { viewModel.submitKeypadAmount() },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun IdentityCardPanel(
    cust: CustomerRequest,
    bengali: Boolean,
    onMismatch: () -> Unit,
    onMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.widthIn(max = 285.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = WarmParchment,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color(0xFF8C6239), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (bengali) "জাতীয় পরিচয়পত্র (Identity Card)" else "Identity Card",
                    color = Color(0xFF8C5A32),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF9E6B3B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3F2E1E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = TakaGoldLight, modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (bengali) "নাম:" else "Name:",
                                    color = Color(0xFFFDE68A),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = cust.idCardName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${if (bengali) "হিসাব:" else "Acc:"} ${cust.idCardAccountNo}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF7D522B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${if (bengali) "ঠিকানা:" else "Address:"} ${if (bengali) cust.addressBn else cust.addressEn}",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onMismatch,
                colors = ButtonDefaults.buttonColors(containerColor = MismatchRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("nid_mismatch_button")
            ) {
                Text(if (bengali) "মিল নেই" else "Mismatch", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onMatch,
                colors = ButtonDefaults.buttonColors(containerColor = MatchGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("nid_match_button")
            ) {
                Text(if (bengali) "মিলেছে" else "Match", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NumericKeypadPanel(
    bengali: Boolean,
    onKey: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = WarmParchment,
        shadowElevation = 10.dp,
        modifier = modifier
            .widthIn(max = 225.dp)
            .border(2.dp, Color(0xFF8C6239), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", ".")
            )
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { key ->
                        val bg = if (key == "C") MismatchRed else Color(0xFF8C6239)
                        val label = if (bengali && key != "C" && key != ".") AppStrings.toBengaliDigits(key) else key
                        Surface(
                            onClick = { onKey(key) },
                            shape = RoundedCornerShape(8.dp),
                            color = bg,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("keypad_$key")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = MatchGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("keypad_submit_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (bengali) "সম্পন্ন করুন" else "Confirm",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VirtualJoystick(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val outerRadiusPx = with(LocalDensity.current) { 56.dp.toPx() }
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(124.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobX = 0f
                        knobY = 0f
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        knobX = 0f
                        knobY = 0f
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nx = knobX + dragAmount.x
                        val ny = knobY + dragAmount.y
                        val dist = hypot(nx, ny)
                        val maxR = outerRadiusPx * 0.72f
                        if (dist > maxR && dist > 0f) {
                            knobX = nx * (maxR / dist)
                            knobY = ny * (maxR / dist)
                        } else {
                            knobX = nx
                            knobY = ny
                        }
                        onMove((knobX / maxR).coerceIn(-1f, 1f), (knobY / maxR).coerceIn(-1f, 1f))
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 4f
            drawCircle(color = Color(0x66111827), radius = radius, center = center)
            drawCircle(color = Color.White.copy(alpha = 0.65f), radius = radius, center = center, style = Stroke(width = 3f))
            // Knob
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, Color(0xFFCBD5E1))),
                radius = radius * 0.38f,
                center = Offset(center.x + knobX, center.y + knobY)
            )
        }
        Icon(
            imageVector = Icons.Filled.ArrowUpward,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .size(14.dp)
        )
    }
}

@Composable
private fun ProjectedWorldMarkers(
    markers: List<ProjectedMarker>,
    bengali: Boolean,
    bankOpen: Boolean,
    debtBdt: Long
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()

        markers.forEach { m ->
            if (!m.visible) return@forEach
            val px = (m.normX * wPx).roundToInt()
            val py = (m.normY * hPx).roundToInt()

            when (m.id) {
                "angry_emoji" -> {
                    Text(
                        text = "😠",
                        fontSize = 34.sp,
                        modifier = Modifier.offset { IntOffset(px - 40, py - 40) }
                    )
                }
                "owner_debt" -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CharcoalDark.copy(alpha = 0.9f),
                        modifier = Modifier
                            .offset { IntOffset(px - 90, py - 30) }
                            .border(1.5.dp, Color(0xFF94A3B8), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("DEBT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "-${AppStrings.formatBdt(debtBdt, bengali)}",
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                "upgrade_pad" -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SkyEmeraldDark.copy(alpha = 0.9f),
                        modifier = Modifier.offset { IntOffset(px - 75, py - 24) }
                    ) {
                        Text(
                            text = if (bengali) "ব্যাংক আপগ্রেড" else "UPGRADE BANK",
                            color = SkyCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                "car_showroom" -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CharcoalDark.copy(alpha = 0.85f),
                        modifier = Modifier.offset { IntOffset(px - 80, py - 24) }
                    ) {
                        Text(
                            text = if (bengali) "🔒 কার শোরুম" else "🔒 CAR SHOWROOM",
                            color = Color(0xFF86EFAC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                "door_sign" -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (bankOpen) MatchGreen else MismatchRed,
                        modifier = Modifier
                            .offset { IntOffset(px - 42, py - 18) }
                            .border(1.5.dp, Color.White, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = if (bankOpen) "OPEN" else "CLOSE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun interactLabel(target: InteractableType, bengali: Boolean, bankOpen: Boolean): String {
    return when (target) {
        InteractableType.OWNER -> if (bengali) "মালিকের সাথে ডিল করুন" else "Talk to Owner"
        InteractableType.UPGRADE_PAD -> if (bengali) "ব্যাংক আপগ্রেড করুন" else "Upgrade Bank"
        InteractableType.ATM_MACHINE -> if (bengali) "এটিএম চালু করুন" else "Use / Turn On ATM"
        InteractableType.DOOR_SIGN -> if (bankOpen) {
            if (bengali) "ব্যাংক বন্ধ করুন" else "Close Door"
        } else {
            if (bengali) "দরজা OPEN করুন" else "Open Bank Door"
        }
        InteractableType.TELLER_DESK -> if (bengali) "টেলার ডেস্কে বসুন" else "Sit at Desk"
        InteractableType.CAR_SHOWROOM -> if (bengali) "কার শোরুম খুলুন" else "Car Showroom"
    }
}
