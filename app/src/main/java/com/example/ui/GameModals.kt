package com.example.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BankTransaction
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CharcoalDark
import com.example.ui.theme.MatchGreen
import com.example.ui.theme.MismatchRed
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.SkyEmeraldDark
import com.example.ui.theme.TakaGold
import com.example.ui.theme.TakaGoldLight
import com.example.ui.theme.WarmParchment

@Composable
fun GameModals(
    hud: GameHudState,
    transactions: List<BankTransaction>,
    viewModel: GameViewModel,
    onSignOut: () -> Unit
) {
    val bn = hud.bengali
    val p = hud.profile

    if (hud.showShopDialog) {
        Dialog(onDismissRequest = { viewModel.closeShop() }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = WarmParchment,
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .border(2.dp, TakaGold, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (bn) "ইস্কাই ব্যাংক শপ ও আপগ্রেড" else "Sky Bank Shop & Upgrades",
                            color = SkyEmeraldDark,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.closeShop() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = CharcoalDark)
                        }
                    }

                    Text(
                        text = "${if (bn) "ব্যালেন্স:" else "Balance:"} ${AppStrings.formatBdt(p.cashBdt, bn)}  •  ${if (bn) "বকেয়া ঋণ:" else "Debt:"} ${AppStrings.formatBdt(p.debtBdt, bn)}",
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 310.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            ShopItemRow(
                                icon = Icons.Filled.Bolt,
                                title = if (bn) "চা-নাস্তা ও এনার্জি (+১৫)" else "Tea & Energy Refill (+15)",
                                subtitle = if (bn) "কাজের গতি বাড়াতে এনার্জি রিফিল" else "Restore stamina to serve customers",
                                priceText = AppStrings.formatBdt(500L, bn),
                                enabled = p.energy < p.maxEnergy,
                                onBuy = { viewModel.buyEnergyRefill() },
                                testTag = "buy_energy_button"
                            )
                        }
                        item {
                            val nextCost = p.counterCount * 25000L
                            ShopItemRow(
                                icon = Icons.Filled.Storefront,
                                title = if (bn) "নতুন টেলার কাউন্টার (${p.counterCount}/৪)" else "Extra Teller Counter (${p.counterCount}/4)",
                                subtitle = if (bn) "স্টাফ নিয়োগ — স্বয়ংক্রিয় আয় বাড়বে" else "Hire clerk for automatic income",
                                priceText = if (p.counterCount >= 4L) (if (bn) "পূর্ণ" else "MAX") else AppStrings.formatBdt(nextCost, bn),
                                enabled = p.counterCount < 4L && p.bankOwned,
                                onBuy = { viewModel.buyExtraCounter() },
                                testTag = "buy_counter_button"
                            )
                        }
                        item {
                            val vCost = p.vaultLevel * 30000L
                            ShopItemRow(
                                icon = Icons.Filled.Security,
                                title = if (bn) "ভল্ট সিকিউরিটি (লেভেল ${p.vaultLevel}/৫)" else "Vault Security (Lv.${p.vaultLevel}/5)",
                                subtitle = if (bn) "এটিএম আয় ও দৈনিক টার্গেট বোনাস বাড়ে" else "Boosts ATM fees & vault capacity",
                                priceText = if (p.vaultLevel >= 5L) (if (bn) "পূর্ণ" else "MAX") else AppStrings.formatBdt(vCost, bn),
                                enabled = p.vaultLevel < 5L && p.bankOwned,
                                onBuy = { viewModel.buyVaultUpgrade() },
                                testTag = "buy_vault_button"
                            )
                        }
                        item {
                            ShopItemRow(
                                icon = Icons.Filled.DirectionsCar,
                                title = if (bn) "কার ফাইন্যান্স ও শোরুম" else "Car Finance Showroom",
                                subtitle = if (bn) "৩ডি গাড়ি শোরুম ও বড় লোন কমিশন" else "3D car podium & high-value loans",
                                priceText = if (p.carShowroomUnlocked) (if (bn) "চালু" else "OWNED") else AppStrings.formatBdt(75000L, bn),
                                enabled = !p.carShowroomUnlocked && p.bankOwned,
                                onBuy = { viewModel.unlockCarShowroom() },
                                testTag = "buy_car_showroom_button"
                            )
                        }
                        item {
                            val payAmt = minOf(50000L, p.debtBdt)
                            ShopItemRow(
                                icon = Icons.Filled.Payments,
                                title = if (bn) "ব্যাংক ঋণ পরিশোধ" else "Pay Bank Debt",
                                subtitle = if (bn) "বকেয়া ঋণ: ${AppStrings.formatBdt(p.debtBdt, true)}" else "Remaining debt: ${AppStrings.formatBdt(p.debtBdt, false)}",
                                priceText = if (p.debtBdt <= 0L) (if (bn) "ঋণমুক্ত!" else "PAID") else AppStrings.formatBdt(payAmt, bn),
                                enabled = p.debtBdt > 0L,
                                onBuy = { viewModel.payDebtInstallment() },
                                testTag = "pay_debt_button"
                            )
                        }
                    }
                }
            }
        }
    }

    if (hud.showLedgerDialog) {
        Dialog(onDismissRequest = { viewModel.toggleLedgerDialog(false) }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = CharcoalDark,
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .border(1.5.dp, SkyCyan, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (bn) "লেনদেনের খাতা (Cloud Ledger)" else "Bank Transaction Ledger",
                            color = TakaGoldLight,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.toggleLedgerDialog(false) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (transactions.isEmpty()) {
                        Text(
                            text = if (bn) "এখনো কোনো লেনদেন রেকর্ড হয়নি। গ্রাহকদের সেবা দিন!" else "No transactions recorded yet.",
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(transactions, key = { it.id.ifBlank { "${it.accountNo}_${it.amountBdt}_${it.createdAt}" } }) { tx ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tx.customerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${tx.txType} • ${tx.accountNo}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = AppStrings.formatBdt(tx.amountBdt, bn),
                                                color = if (tx.success) CashGreen else MismatchRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (tx.commissionBdt > 0L) {
                                                Text(
                                                    text = "+${AppStrings.formatBdt(tx.commissionBdt, bn)}",
                                                    color = TakaGoldLight,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (hud.showEndOfDayDialog) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = WarmParchment,
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .border(2.dp, TakaGold, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (bn) "দিন ${AppStrings.toBengaliDigits("${p.dayNumber}")} — দৈনিক হিসাব" else "Day ${p.dayNumber} Report",
                        color = SkyEmeraldDark,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReportStatRow(
                        label = if (bn) "আজকের আয়" else "Daily Revenue",
                        value = AppStrings.formatBdt(p.dailyRevenueBdt, bn)
                    )
                    ReportStatRow(
                        label = if (bn) "দৈনিক লক্ষ্য" else "Daily Target",
                        value = AppStrings.formatBdt(p.dailyTargetBdt, bn)
                    )
                    ReportStatRow(
                        label = if (bn) "মোট গ্রাহক সেবা" else "Customers Served",
                        value = if (bn) AppStrings.toBengaliDigits("${p.customersServed}") else "${p.customersServed}"
                    )
                    ReportStatRow(
                        label = if (bn) "ভুয়া এনআইডি আটক" else "Fraud Caught",
                        value = if (bn) AppStrings.toBengaliDigits("${p.fraudCaught}") else "${p.fraudCaught}"
                    )
                    ReportStatRow(
                        label = if (bn) "টার্গেট বোনাস" else "Target Bonus",
                        value = "+${AppStrings.formatBdt(hud.dayBonusEarned, bn)}"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.startNextDay() },
                        colors = ButtonDefaults.buttonColors(containerColor = MatchGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("next_day_button")
                    ) {
                        Text(
                            text = if (bn) "পরবর্তী দিন শুরু করুন" else "Start Next Day",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (hud.showSettingsDialog) {
        Dialog(onDismissRequest = { viewModel.toggleSettingsDialog(false) }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = CharcoalDark,
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .border(1.5.dp, SkyCyan, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (bn) "সেটিংস (Settings)" else "Settings",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.toggleSettingsDialog(false) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (bn) "ভাষা: বাংলা (Bengali)" else "Language: English", color = Color.White)
                        Switch(checked = hud.bengali, onCheckedChange = { viewModel.toggleLanguage() })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (bn) "সাউন্ড এফেক্ট" else "Sound Effects", color = Color.White)
                        Switch(checked = hud.soundEnabled, onCheckedChange = { viewModel.toggleSound() })
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (bn) "ক্যামেরা সেনসিটিভিটি" else "Camera Sensitivity",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Slider(
                        value = hud.cameraSensitivity,
                        onValueChange = { viewModel.setCameraSensitivity(it) },
                        valueRange = 0.5f..2.0f
                    )

                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.manualSaveToCloud() },
                            colors = ButtonDefaults.buttonColors(containerColor = SkyCyan),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = CharcoalDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (bn) "ক্লাউড সেভ" else "Cloud Save", color = CharcoalDark, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                viewModel.toggleSettingsDialog(false)
                                onSignOut()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MismatchRed),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.Logout, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (bn) "লগ আউট" else "Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    priceText: String,
    enabled: Boolean,
    onBuy: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFD6B588), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = SkyEmeraldDark, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = CharcoalDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = Color(0xFF64748B), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onBuy,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MatchGreen,
                    disabledContainerColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp).testTag(testTag)
            ) {
                Text(priceText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ReportStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF475569), fontSize = 14.sp)
        Text(value, color = CharcoalDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
