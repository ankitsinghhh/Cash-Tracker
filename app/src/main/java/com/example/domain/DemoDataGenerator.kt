package com.example.domain

import com.example.data.local.AppDatabase
import com.example.data.local.populateInitialDefaults
import com.example.data.model.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

object DemoDataGenerator {

    suspend fun seedComprehensiveDemoData(database: AppDatabase) {
        clearAllData(database)
        populateInitialDefaults(database)

        val accountDao = database.accountDao()
        val categoryDao = database.categoryDao()
        val transactionDao = database.transactionDao()
        val budgetDao = database.budgetDao()
        val recurringDao = database.recurringDao()
        val installmentDao = database.installmentDao()
        val memoDao = database.memoDao()
        val bookmarkDao = database.bookmarkDao()

        // Safely fetch seeded accounts and categories
        val accounts = accountDao.getAllAccounts().first()
        val categories = categoryDao.getAllCategories().first()

        val cashAccount = accounts.find { it.type == AccountType.CASH } ?: accounts.first()
        val hdfcBank = accounts.find { it.name.contains("HDFC Bank") } ?: accounts.first()
        val iciciBank = accounts.find { it.name.contains("ICICI") } ?: accounts.first()
        val creditCard = accounts.find { it.type == AccountType.CREDIT_CARD } ?: accounts.first()
        val investmentAccount = accounts.find { it.type == AccountType.INVESTMENT } ?: accounts.first()

        val foodCat = categories.find { it.name.contains("Food") } ?: categories.first()
        val groceryCat = categories.find { it.name.contains("Groceries") } ?: categories.first()
        val transportCat = categories.find { it.name.contains("Transportation") } ?: categories.first()
        val fuelCat = categories.find { it.name.contains("Fuel") } ?: transportCat
        val shoppingCat = categories.find { it.name.contains("Shopping") } ?: categories.first()
        val billsCat = categories.find { it.name.contains("Bills") } ?: categories.first()
        val rentCat = categories.find { it.name.contains("Rent") } ?: billsCat
        val entertainmentCat = categories.find { it.name.contains("Entertainment") } ?: categories.first()
        val healthCat = categories.find { it.name.contains("Health") } ?: categories.first()
        val subCat = categories.find { it.name.contains("Subscriptions") } ?: categories.first()

        val salaryCat = categories.find { it.name.contains("Salary") } ?: categories.first()
        val bonusCat = categories.find { it.name.contains("Bonus") } ?: salaryCat
        val freelanceCat = categories.find { it.name.contains("Freelance") } ?: salaryCat
        val refundCat = categories.find { it.name.contains("Refund") } ?: salaryCat

        val now = Calendar.getInstance()
        val curMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(now.time)
        val curDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)

        val demoTransactions = mutableListOf<TransactionEntity>()

        // Helper to get time millis offset in days
        fun daysAgo(days: Int, hour: Int = 12, minute: Int = 30): Long {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -days)
            c.set(Calendar.HOUR_OF_DAY, hour)
            c.set(Calendar.MINUTE, minute)
            return c.timeInMillis
        }

        // --- Current Month Transactions ---
        // Salary on 1st of month
        val salaryCal = Calendar.getInstance()
        salaryCal.set(Calendar.DAY_OF_MONTH, 1)
        salaryCal.set(Calendar.HOUR_OF_DAY, 9)
        salaryCal.set(Calendar.MINUTE, 0)
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.INCOME,
                dateMillis = salaryCal.timeInMillis,
                amount = 14500000L, // ₹1,45,000.00
                accountId = hdfcBank.id,
                categoryId = salaryCat.id,
                payee = "Google Tech India",
                note = "Monthly salary credit",
                paymentMethod = PaymentMethod.BANK_TRANSFER
            )
        )

        // Rent payment
        val rentCal = Calendar.getInstance()
        rentCal.set(Calendar.DAY_OF_MONTH, 3)
        rentCal.set(Calendar.HOUR_OF_DAY, 11)
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = rentCal.timeInMillis,
                amount = 3200000L, // ₹32,000.00
                accountId = hdfcBank.id,
                categoryId = rentCat.id,
                payee = "Apartment Owner",
                note = "August Apartment Rent",
                paymentMethod = PaymentMethod.NET_BANKING
            )
        )

        // ATM Withdrawal (Transfer from Bank to Cash)
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                dateMillis = daysAgo(12, 16, 45),
                amount = 1000000L, // ₹10,000.00
                accountId = hdfcBank.id,
                toAccountId = cashAccount.id,
                transferFee = 0L,
                categoryId = 0L,
                payee = "HDFC ATM Indiranagar",
                note = "Monthly cash withdrawal",
                paymentMethod = PaymentMethod.CASH
            )
        )

        // Groceries DMart & Swiggy Instamart
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(1, 19, 20),
                amount = 465000L, // ₹4,650.00
                accountId = creditCard.id,
                categoryId = groceryCat.id,
                payee = "DMart Supermarket",
                note = "Monthly essentials & kitchen supplies",
                tags = "Household, Groceries",
                paymentMethod = PaymentMethod.CREDIT_CARD
            )
        )
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(3, 8, 15),
                amount = 48500L, // ₹485.00
                accountId = hdfcBank.id,
                categoryId = groceryCat.id,
                payee = "Swiggy Instamart",
                note = "Milk, bread, eggs",
                paymentMethod = PaymentMethod.UPI
            )
        )

        // Dining & Cafes
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(2, 21, 0),
                amount = 185000L, // ₹1,850.00
                accountId = creditCard.id,
                categoryId = foodCat.id,
                payee = "Toit Brewpub",
                note = "Dinner with team",
                tags = "Dining, Friends",
                paymentMethod = PaymentMethod.CREDIT_CARD
            )
        )
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(4, 17, 30),
                amount = 39000L, // ₹390.00
                accountId = cashAccount.id,
                categoryId = foodCat.id,
                payee = "Third Wave Coffee",
                note = "Cold brew & croissant",
                paymentMethod = PaymentMethod.CASH
            )
        )
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(0, 13, 10), // Today
                amount = 54000L, // ₹540.00
                accountId = hdfcBank.id,
                categoryId = foodCat.id,
                payee = "Swiggy",
                note = "Lunch delivery",
                paymentMethod = PaymentMethod.UPI
            )
        )

        // Fuel & Transport
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(5, 10, 0),
                amount = 250000L, // ₹2,500.00
                accountId = creditCard.id,
                categoryId = fuelCat.id,
                payee = "Shell Petrol Pump",
                note = "Car full tank V-Power",
                paymentMethod = PaymentMethod.CREDIT_CARD
            )
        )
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(6, 18, 40),
                amount = 32000L, // ₹320.00
                accountId = hdfcBank.id,
                categoryId = transportCat.id,
                payee = "Uber",
                note = "Cab from office",
                paymentMethod = PaymentMethod.UPI
            )
        )

        // Shopping & Electronics
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(7, 15, 20),
                amount = 349900L, // ₹3,499.00
                accountId = creditCard.id,
                categoryId = shoppingCat.id,
                payee = "Amazon India",
                note = "Ergonomic keyboard wrist rest & cables",
                tags = "Work, Electronics",
                paymentMethod = PaymentMethod.CREDIT_CARD
            )
        )

        // Utilities / Bills
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(8, 12, 0),
                amount = 149900L, // ₹1,499.00
                accountId = hdfcBank.id,
                categoryId = billsCat.id,
                payee = "Airtel Broadband",
                note = "Fiber internet monthly bill",
                paymentMethod = PaymentMethod.UPI
            )
        )
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(10, 14, 0),
                amount = 285000L, // ₹2,850.00
                accountId = hdfcBank.id,
                categoryId = billsCat.id,
                payee = "Electricity Board (BESCOM)",
                note = "Power bill",
                paymentMethod = PaymentMethod.NET_BANKING
            )
        )

        // Subscriptions
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(9, 10, 0),
                amount = 64900L, // ₹649.00
                accountId = creditCard.id,
                categoryId = subCat.id,
                payee = "Netflix",
                note = "Premium 4K plan",
                paymentMethod = PaymentMethod.CREDIT_CARD
            )
        )
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateMillis = daysAgo(11, 10, 0),
                amount = 17900L, // ₹179.00
                accountId = creditCard.id,
                categoryId = subCat.id,
                payee = "Spotify India",
                note = "Individual Premium",
                paymentMethod = PaymentMethod.CREDIT_CARD
            )
        )

        // SIP Investment Transfer (Bank -> Zerodha)
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                dateMillis = daysAgo(6, 9, 30),
                amount = 2500000L, // ₹25,000.00
                accountId = hdfcBank.id,
                toAccountId = investmentAccount.id,
                transferFee = 0L,
                categoryId = 0L,
                payee = "Zerodha AMC",
                note = "Monthly Nifty 50 Index Fund SIP",
                paymentMethod = PaymentMethod.NET_BANKING
            )
        )

        // Credit Card Repayment (Bank -> Credit Card Transfer)
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                dateMillis = daysAgo(14, 15, 0),
                amount = 1850000L, // ₹18,500.00
                accountId = hdfcBank.id,
                toAccountId = creditCard.id,
                transferFee = 0L,
                categoryId = 0L,
                payee = "HDFC Card Bill Pay",
                note = "Previous month statement settlement",
                paymentMethod = PaymentMethod.NET_BANKING
            )
        )

        // Freelance side income
        demoTransactions.add(
            TransactionEntity(
                type = TransactionType.INCOME,
                dateMillis = daysAgo(8, 18, 0),
                amount = 2500000L, // ₹25,000.00
                accountId = iciciBank.id,
                categoryId = freelanceCat.id,
                payee = "Acme Global Client",
                note = "UI Consultation Milestone 1",
                paymentMethod = PaymentMethod.BANK_TRANSFER
            )
        )

        // --- Previous Month (30-60 days ago) historical transactions ---
        for (monthOffset in 1..2) {
            val baseDay = monthOffset * 30
            // Previous salary
            demoTransactions.add(
                TransactionEntity(
                    type = TransactionType.INCOME,
                    dateMillis = daysAgo(baseDay + 28, 9, 0),
                    amount = 14500000L,
                    accountId = hdfcBank.id,
                    categoryId = salaryCat.id,
                    payee = "Google Tech India",
                    note = "Monthly salary credit"
                )
            )
            // Previous rent
            demoTransactions.add(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    dateMillis = daysAgo(baseDay + 26, 11, 0),
                    amount = 3200000L,
                    accountId = hdfcBank.id,
                    categoryId = rentCat.id,
                    payee = "Apartment Owner",
                    note = "Rent"
                )
            )
            // Previous groceries
            demoTransactions.add(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    dateMillis = daysAgo(baseDay + 15, 18, 0),
                    amount = 1240000L,
                    accountId = creditCard.id,
                    categoryId = groceryCat.id,
                    payee = "DMart Supermarket",
                    note = "Groceries"
                )
            )
            // Previous dining
            demoTransactions.add(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    dateMillis = daysAgo(baseDay + 10, 20, 0),
                    amount = 580000L,
                    accountId = creditCard.id,
                    categoryId = foodCat.id,
                    payee = "Mainland China Restaurant",
                    note = "Family dinner"
                )
            )
            // Previous SIP
            demoTransactions.add(
                TransactionEntity(
                    type = TransactionType.TRANSFER,
                    dateMillis = daysAgo(baseDay + 20, 9, 30),
                    amount = 2500000L,
                    accountId = hdfcBank.id,
                    toAccountId = investmentAccount.id,
                    transferFee = 0L,
                    categoryId = 0L,
                    payee = "Zerodha AMC",
                    note = "SIP investment"
                )
            )
        }

        transactionDao.insertAll(demoTransactions)

        // Budgets for Current Month
        val demoBudgets = listOf(
            Budget(categoryId = 0L, monthString = curMonth, amount = 8500000L), // Total budget ₹85,000
            Budget(categoryId = foodCat.id, monthString = curMonth, amount = 1200000L), // Food ₹12,000
            Budget(categoryId = groceryCat.id, monthString = curMonth, amount = 1500000L), // Groceries ₹15,000
            Budget(categoryId = transportCat.id, monthString = curMonth, amount = 800000L), // Transport ₹8,000
            Budget(categoryId = shoppingCat.id, monthString = curMonth, amount = 1000000L), // Shopping ₹10,000
            Budget(categoryId = billsCat.id, monthString = curMonth, amount = 3800000L), // Bills ₹38,000
            Budget(categoryId = entertainmentCat.id, monthString = curMonth, amount = 500000L) // Entertainment ₹5,000
        )
        budgetDao.insertAll(demoBudgets)

        // Recurring Transactions
        val nextMonthSalaryCal = Calendar.getInstance()
        nextMonthSalaryCal.add(Calendar.MONTH, 1)
        nextMonthSalaryCal.set(Calendar.DAY_OF_MONTH, 1)

        recurringDao.insert(
            RecurringTransaction(
                name = "Monthly Tech Salary",
                type = TransactionType.INCOME,
                amount = 14500000L,
                accountId = hdfcBank.id,
                categoryId = salaryCat.id,
                payee = "Google Tech India",
                note = "Automated salary entry",
                frequency = RecurringFrequency.MONTHLY,
                startDateMillis = System.currentTimeMillis(),
                nextDueDateMillis = nextMonthSalaryCal.timeInMillis
            )
        )

        recurringDao.insert(
            RecurringTransaction(
                name = "House Rent",
                type = TransactionType.EXPENSE,
                amount = 3200000L,
                accountId = hdfcBank.id,
                categoryId = rentCat.id,
                payee = "Apartment Owner",
                frequency = RecurringFrequency.MONTHLY,
                startDateMillis = System.currentTimeMillis(),
                nextDueDateMillis = nextMonthSalaryCal.timeInMillis
            )
        )

        recurringDao.insert(
            RecurringTransaction(
                name = "Netflix 4K Subscription",
                type = TransactionType.EXPENSE,
                amount = 64900L,
                accountId = creditCard.id,
                categoryId = subCat.id,
                payee = "Netflix",
                frequency = RecurringFrequency.MONTHLY,
                startDateMillis = System.currentTimeMillis(),
                nextDueDateMillis = System.currentTimeMillis() + (86400000L * 18)
            )
        )

        // Installment Plan
        installmentDao.insert(
            InstallmentPlan(
                name = "MacBook Pro M3 Max",
                totalAmount = 18000000L, // ₹1,80,000.00
                totalInstallments = 6,
                paidInstallments = 2,
                monthlyAmount = 3000000L, // ₹30,000 / month
                accountId = creditCard.id,
                categoryId = shoppingCat.id,
                payee = "Apple Store BKC",
                note = "No-cost EMI on HDFC Credit Card",
                startDateMillis = daysAgo(60),
                nextDueDateMillis = System.currentTimeMillis() + (86400000L * 15)
            )
        )

        // Daily Memos
        memoDao.insert(
            DailyMemo(
                dateString = curDay,
                memoText = "Team lunch at Indiranagar & review monthly investment portfolio",
                colorHex = "#10B981"
            )
        )

        // Bookmarks
        bookmarkDao.insert(
            Bookmark(
                title = "Regular Office Lunch",
                type = TransactionType.EXPENSE,
                amount = 25000L, // ₹250
                accountId = hdfcBank.id,
                categoryId = foodCat.id,
                payee = "Cafeteria / Swiggy",
                note = "Daily lunch",
                paymentMethod = PaymentMethod.UPI
            )
        )
        bookmarkDao.insert(
            Bookmark(
                title = "Petrol Refill",
                type = TransactionType.EXPENSE,
                amount = 200000L, // ₹2,000
                accountId = creditCard.id,
                categoryId = fuelCat.id,
                payee = "Shell Petrol Pump",
                paymentMethod = PaymentMethod.CREDIT_CARD
            )
        )
    }

    suspend fun clearAllData(database: AppDatabase) {
        database.transactionDao().deleteAll()
        database.accountDao().deleteAll()
        database.categoryDao().deleteAll()
        database.budgetDao().deleteAll()
        database.recurringDao().deleteAll()
        database.installmentDao().deleteAll()
        database.memoDao().deleteAll()
        database.bookmarkDao().deleteAll()
    }
}
