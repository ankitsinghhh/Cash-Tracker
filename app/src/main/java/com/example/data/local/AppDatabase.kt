package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Account::class,
        Category::class,
        TransactionEntity::class,
        Budget::class,
        RecurringTransaction::class,
        InstallmentPlan::class,
        DailyMemo::class,
        Bookmark::class,
        AppSetting::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun memoDao(): MemoDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_manager_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialDefaults(database)
                }
            }
        }
    }
}

suspend fun populateInitialDefaults(db: AppDatabase) {
    // 1. Initial Categories
    val expenseCategories = listOf(
        Category(name = "Food & Dining", type = TransactionType.EXPENSE, iconName = "restaurant", colorHex = "#EF4444", sortOrder = 1),
        Category(name = "Groceries", type = TransactionType.EXPENSE, iconName = "shopping_cart", colorHex = "#F97316", sortOrder = 2),
        Category(name = "Transportation", type = TransactionType.EXPENSE, iconName = "directions_car", colorHex = "#F59E0B", sortOrder = 3),
        Category(name = "Fuel / Petrol", type = TransactionType.EXPENSE, iconName = "local_gas_station", colorHex = "#D97706", sortOrder = 4),
        Category(name = "Shopping", type = TransactionType.EXPENSE, iconName = "shopping_bag", colorHex = "#EC4899", sortOrder = 5),
        Category(name = "Bills & Utilities", type = TransactionType.EXPENSE, iconName = "receipt_long", colorHex = "#3B82F6", sortOrder = 6),
        Category(name = "Rent / Housing", type = TransactionType.EXPENSE, iconName = "home", colorHex = "#6366F1", sortOrder = 7),
        Category(name = "Entertainment", type = TransactionType.EXPENSE, iconName = "movie", colorHex = "#8B5CF6", sortOrder = 8),
        Category(name = "Health & Medical", type = TransactionType.EXPENSE, iconName = "medical_services", colorHex = "#10B981", sortOrder = 9),
        Category(name = "Subscriptions", type = TransactionType.EXPENSE, iconName = "subscriptions", colorHex = "#06B6D4", sortOrder = 10),
        Category(name = "Travel & Holiday", type = TransactionType.EXPENSE, iconName = "flight", colorHex = "#14B8A6", sortOrder = 11),
        Category(name = "Personal Care", type = TransactionType.EXPENSE, iconName = "spa", colorHex = "#F43F5E", sortOrder = 12),
        Category(name = "Education", type = TransactionType.EXPENSE, iconName = "school", colorHex = "#4F46E5", sortOrder = 13),
        Category(name = "Gifts & Donations", type = TransactionType.EXPENSE, iconName = "card_giftcard", colorHex = "#A855F7", sortOrder = 14),
        Category(name = "Investments Out", type = TransactionType.EXPENSE, iconName = "trending_up", colorHex = "#059669", sortOrder = 15),
        Category(name = "Other Expense", type = TransactionType.EXPENSE, iconName = "category", colorHex = "#64748B", sortOrder = 16)
    )
    val savedExpenseIds = db.categoryDao().insertAll(expenseCategories)

    // Subcategories for Food
    if (savedExpenseIds.isNotEmpty()) {
        val foodId = savedExpenseIds[0]
        db.categoryDao().insertAll(
            listOf(
                Category(name = "Restaurants", type = TransactionType.EXPENSE, iconName = "restaurant", colorHex = "#EF4444", parentId = foodId),
                Category(name = "Coffee & Snacks", type = TransactionType.EXPENSE, iconName = "local_cafe", colorHex = "#EF4444", parentId = foodId),
                Category(name = "Food Delivery", type = TransactionType.EXPENSE, iconName = "delivery_dining", colorHex = "#EF4444", parentId = foodId)
            )
        )
    }

    val incomeCategories = listOf(
        Category(name = "Salary", type = TransactionType.INCOME, iconName = "payments", colorHex = "#10B981", sortOrder = 1),
        Category(name = "Bonus", type = TransactionType.INCOME, iconName = "redeem", colorHex = "#059669", sortOrder = 2),
        Category(name = "Freelance / Business", type = TransactionType.INCOME, iconName = "work", colorHex = "#0D9488", sortOrder = 3),
        Category(name = "Investments & Dividends", type = TransactionType.INCOME, iconName = "show_chart", colorHex = "#14B8A6", sortOrder = 4),
        Category(name = "Interest Income", type = TransactionType.INCOME, iconName = "savings", colorHex = "#06B6D4", sortOrder = 5),
        Category(name = "Rental Income", type = TransactionType.INCOME, iconName = "apartment", colorHex = "#3B82F6", sortOrder = 6),
        Category(name = "Refunds / Cashback", type = TransactionType.INCOME, iconName = "currency_exchange", colorHex = "#8B5CF6", sortOrder = 7),
        Category(name = "Gifts Received", type = TransactionType.INCOME, iconName = "card_giftcard", colorHex = "#EC4899", sortOrder = 8),
        Category(name = "Other Income", type = TransactionType.INCOME, iconName = "attach_money", colorHex = "#64748B", sortOrder = 9)
    )
    db.categoryDao().insertAll(incomeCategories)

    // 2. Default Accounts
    val defaultAccounts = listOf(
        Account(
            name = "Cash Wallet",
            type = AccountType.CASH,
            groupName = "Cash",
            institution = "Physical Cash",
            initialBalance = 500000L, // ₹5,000.00
            currency = "INR",
            sortOrder = 1
        ),
        Account(
            name = "HDFC Bank",
            type = AccountType.BANK,
            groupName = "Banks",
            institution = "HDFC Bank",
            initialBalance = 8500000L, // ₹85,000.00
            currency = "INR",
            sortOrder = 2
        ),
        Account(
            name = "ICICI Bank",
            type = AccountType.BANK,
            groupName = "Banks",
            institution = "ICICI Bank",
            initialBalance = 4200000L, // ₹42,000.00
            currency = "INR",
            sortOrder = 3
        ),
        Account(
            name = "HDFC Regalia Credit Card",
            type = AccountType.CREDIT_CARD,
            groupName = "Cards",
            institution = "HDFC Bank",
            initialBalance = 0L,
            creditLimit = 30000000L, // ₹3,00,000 limit
            statementDay = 15,
            paymentDueDay = 5,
            currency = "INR",
            sortOrder = 4
        ),
        Account(
            name = "Mutual Funds / SIP",
            type = AccountType.INVESTMENT,
            groupName = "Investments",
            institution = "Zerodha",
            initialBalance = 25000000L, // ₹2,50,000.00
            currency = "INR",
            sortOrder = 5
        ),
        Account(
            name = "Emergency Savings",
            type = AccountType.SAVINGS,
            groupName = "Savings",
            institution = "SBI",
            initialBalance = 15000000L, // ₹1,50,000.00
            currency = "INR",
            sortOrder = 6
        )
    )
    db.accountDao().insertAll(defaultAccounts)

    // 3. Default Settings
    db.settingDao().setSetting(AppSetting("primary_currency", "INR"))
    db.settingDao().setSetting(AppSetting("currency_code", "INR"))
    db.settingDao().setSetting(AppSetting("indian_number_format", "true"))
    db.settingDao().setSetting(AppSetting("theme_mode", "SYSTEM"))
    db.settingDao().setSetting(AppSetting("accent_color", "TEAL"))
    db.settingDao().setSetting(AppSetting("first_day_of_week", "SUNDAY"))
    db.settingDao().setSetting(AppSetting("monthly_carry_over", "false"))
    db.settingDao().setSetting(AppSetting("onboarding_completed", "false"))
}
