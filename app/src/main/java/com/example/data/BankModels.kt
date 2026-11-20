package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.random.Random

@Keep
@IgnoreExtraProperties
data class BankProfile(
    val userId: String = "",
    val displayName: String = "ম্যানেজার",
    val bankName: String = "ইস্কাই ব্যাংক",
    val cashBdt: Long = 550000L,
    val debtBdt: Long = 1200000L,
    val energy: Long = 25L,
    val maxEnergy: Long = 25L,
    val level: Long = 1L,
    val xp: Long = 0L,
    val dayNumber: Long = 1L,
    val minuteOfDay: Long = 680L,
    val bankOwned: Boolean = false,
    val bankUpgraded: Boolean = false,
    val atmActive: Boolean = false,
    val carShowroomUnlocked: Boolean = false,
    val vaultLevel: Long = 1L,
    val counterCount: Long = 1L,
    val dailyRevenueBdt: Long = 0L,
    val dailyTargetBdt: Long = 60000L,
    val customersServed: Long = 0L,
    val fraudCaught: Long = 0L,
    val bankOpen: Boolean = false,
    val introSeen: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class TransactionType {
    DEPOSIT,
    WITHDRAW,
    CAR_FINANCE,
    FRAUD_REJECTED,
    UPGRADE,
    DEBT_PAYMENT,
    ATM_FEE;

    companion object {
        fun safeFrom(raw: String): TransactionType =
            entries.firstOrNull { it.name == raw } ?: DEPOSIT
    }
}

@Keep
@IgnoreExtraProperties
data class BankTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val accountNo: String = "",
    val txType: String = TransactionType.DEPOSIT.name,
    val amountBdt: Long = 0L,
    val commissionBdt: Long = 0L,
    val dayNumber: Long = 1L,
    val success: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class CustomerRequest(
    val id: String,
    val portalName: String,
    val portalAccountNo: String,
    val idCardName: String,
    val idCardAccountNo: String,
    val addressBn: String,
    val addressEn: String,
    val isFraud: Boolean,
    val txType: TransactionType,
    val amountBdt: Long,
    val commissionBdt: Long,
    val shirtColor: Long,
    val hairColor: Long
)

object BangladeshiCustomerFactory {
    private val names = listOf(
        "সাকিবুল হাসান" to "Sakibul Hasan",
        "নুসরাত জাহান" to "Nusrat Jahan",
        "রাহাত আহমেদ" to "Rahat Ahmed",
        "তানহা ইসলাম" to "Tanha Islam",
        "মাহমুদ করিম" to "Mahmud Karim",
        "ফারিহা তাবাসসুম" to "Fariha Tabassum",
        "আরেফিন শুভ" to "Arefin Shuvo",
        "মেহজাবীন চৌধুরী" to "Mehjabin Chowdhury",
        "তামিম ইকবাল" to "Tamim Iqbal",
        "সাদিয়া আফরিন" to "Sadia Afrin"
    )

    private val addresses = listOf(
        "বাড়ি ১২, রোড ৪, ধানমন্ডি, ঢাকা ১২০৫" to "House 12, Road 4, Dhanmondi, Dhaka",
        "সেক্টর ৭, উত্তরা, ঢাকা ১২৩০" to "Sector 7, Uttara, Dhaka 1230",
        "আগ্রাবাদ বাণিজ্যিক এলাকা, চট্টগ্রাম" to "Agrabad C/A, Chattogram",
        "জিন্দাবাজার মেইন রোড, সিলেট" to "Zindabazar Main Road, Sylhet",
        "গুলশান ২ এভিনিউ, ঢাকা ১২১২" to "Gulshan 2 Avenue, Dhaka 1212",
        "সাহেব বাজার, রাজশাহী ৬১০০" to "Shaheb Bazar, Rajshahi 6100",
        "শিববাড়ি মোড়, খুলনা ৯১০০" to "Shibbari More, Khulna 9100",
        "বনানী রোড ১১, ঢাকা ১২১৩" to "Banani Road 11, Dhaka 1213"
    )

    private val shirtColors = listOf(
        0xFFE63946L, 0xFF2A9D8FL, 0xFF457B9DL, 0xFFF4A261L, 0xFF6D597AL, 0xFF1D3557L
    )
    private val hairColors = listOf(
        0xFF1A1A1AL, 0xFF2B2118L, 0xFF4A3728L
    )

    fun nextCustomer(level: Int, carShowroomUnlocked: Boolean, bengali: Boolean): CustomerRequest {
        val namePair = names[Random.nextInt(names.size)]
        val fakeNamePair = names.filter { it != namePair }.random()
        val addrPair = addresses[Random.nextInt(addresses.size)]
        val acctNum = "SKY-${Random.nextInt(40100000, 40999999)}"
        val isFraud = Random.nextFloat() < 0.28f

        val txType = when {
            carShowroomUnlocked && Random.nextFloat() < 0.25f -> TransactionType.CAR_FINANCE
            Random.nextBoolean() -> TransactionType.DEPOSIT
            else -> TransactionType.WITHDRAW
        }

        val amount = when (txType) {
            TransactionType.CAR_FINANCE -> Random.nextInt(25, 85) * 1000L
            TransactionType.DEPOSIT -> Random.nextInt(5, 45 + level * 8) * 100L
            TransactionType.WITHDRAW -> Random.nextInt(4, 35 + level * 6) * 100L
            else -> 5000L
        }

        val commission = when (txType) {
            TransactionType.CAR_FINANCE -> (amount * 0.14).toLong().coerceAtLeast(3500L)
            else -> (amount * 0.12).toLong().coerceAtLeast(450L)
        }

        val portalName = if (bengali) namePair.first else namePair.second
        val cardName = if (isFraud && Random.nextBoolean()) {
            if (bengali) fakeNamePair.first else fakeNamePair.second
        } else {
            portalName
        }
        val cardAcct = if (isFraud && cardName == portalName) {
            "SKY-${Random.nextInt(40100000, 40999999)}"
        } else if (isFraud && Random.nextBoolean()) {
            "SKY-${Random.nextInt(40100000, 40999999)}"
        } else {
            acctNum
        }

        return CustomerRequest(
            id = "cust_${System.nanoTime()}",
            portalName = portalName,
            portalAccountNo = acctNum,
            idCardName = cardName,
            idCardAccountNo = cardAcct,
            addressBn = addrPair.first,
            addressEn = addrPair.second,
            isFraud = isFraud && (cardName != portalName || cardAcct != acctNum),
            txType = txType,
            amountBdt = amount,
            commissionBdt = commission,
            shirtColor = shirtColors[Random.nextInt(shirtColors.size)],
            hairColor = hairColors[Random.nextInt(hairColors.size)]
        )
    }
}
