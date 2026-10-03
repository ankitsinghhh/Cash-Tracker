package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class AccountType {
    CASH,
    BANK,
    CREDIT_CARD,
    DEBIT_CARD,
    SAVINGS,
    INVESTMENT,
    LOAN,
    INSURANCE,
    FIXED_DEPOSIT,
    REAL_ESTATE,
    OTHER_ASSET,
    OTHER_LIABILITY
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    UPI("UPI"),
    DEBIT_CARD("Debit Card"),
    CREDIT_CARD("Credit Card"),
    NET_BANKING("Net Banking"),
    BANK_TRANSFER("Bank Transfer"),
    WALLET("Wallet"),
    CHEQUE("Cheque"),
    OTHER("Other")
}

enum class RecurringFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BIWEEKLY("Every 2 Weeks"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly")
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val groupName: String = "Default",
    val institution: String = "",
    val initialBalance: Long = 0L, // in minor units (paise/cents)
    val currency: String = "INR",
    val includeInNetWorth: Boolean = true,
    val note: String = "",
    val isHidden: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    // Credit card specific fields
    val creditLimit: Long = 0L,
    val statementDay: Int = 1,
    val paymentDueDay: Int = 20,
    // Debit card link
    val linkedBankAccountId: Long? = null,
    // Loan specific fields
    val interestRate: Double = 0.0,
    val lenderName: String = ""
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType, // EXPENSE or INCOME
    val iconName: String = "category",
    val colorHex: String = "#0D9488",
    val parentId: Long? = null, // null for main category, non-null for subcategory
    val sortOrder: Int = 0,
    val isHidden: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("toAccountId"),
        Index("categoryId"),
        Index("dateMillis")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val dateMillis: Long, // timestamp in millis
    val amount: Long, // in minor units (positive)
    val accountId: Long,
    val toAccountId: Long? = null, // for TRANSFER
    val transferFee: Long = 0L, // for TRANSFER
    val categoryId: Long,
    val subcategoryId: Long? = null,
    val payee: String = "", // Merchant or Payer
    val note: String = "",
    val tags: String = "", // Comma-separated tags
    val receiptUri: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isExcludedFromStats: Boolean = false,
    val recurringRuleId: Long? = null,
    val installmentId: Long? = null,
    val isBookmarked: Boolean = false
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "monthString"], unique = true)]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long, // 0 for total budget, or specific categoryId
    val monthString: String, // "YYYY-MM" or "DEFAULT"
    val amount: Long // in minor units
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long,
    val subcategoryId: Long? = null,
    val payee: String = "",
    val note: String = "",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val startDateMillis: Long,
    val endDateMillis: Long? = null,
    val nextDueDateMillis: Long,
    val lastProcessedDateMillis: Long? = null,
    val isActive: Boolean = true
)

@Entity(tableName = "installment_plans")
data class InstallmentPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val totalAmount: Long, // in minor units
    val totalInstallments: Int,
    val paidInstallments: Int = 0,
    val monthlyAmount: Long, // in minor units
    val accountId: Long,
    val categoryId: Long,
    val payee: String = "",
    val note: String = "",
    val startDateMillis: Long,
    val nextDueDateMillis: Long,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "daily_memos",
    indices = [Index(value = ["dateString"], unique = true)]
)
data class DailyMemo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // "YYYY-MM-DD"
    val memoText: String,
    val colorHex: String = "#F59E0B"
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: TransactionType,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long,
    val subcategoryId: Long? = null,
    val payee: String = "",
    val note: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)
