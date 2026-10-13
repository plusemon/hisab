package com.plusemon.hisab.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.plusemon.hisab.data.model.AccountType
import com.plusemon.hisab.data.model.Budget
import com.plusemon.hisab.data.model.Category
import com.plusemon.hisab.data.model.Contact
import com.plusemon.hisab.data.model.DebtType
import com.plusemon.hisab.data.model.Frequency
import com.plusemon.hisab.data.model.LoanDebt
import com.plusemon.hisab.data.model.LoanRepayment
import com.plusemon.hisab.data.model.RecurringRule
import com.plusemon.hisab.data.model.SavingsGoal
import com.plusemon.hisab.data.model.ShopCreditPayment
import com.plusemon.hisab.data.model.ShopCreditPurchase
import com.plusemon.hisab.data.model.TransactionRecord
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.data.model.User
import com.plusemon.hisab.data.model.UserAccount
import com.plusemon.hisab.data.model.UserSettings
import com.plusemon.hisab.data.model.Vendor

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? = value?.let { enumValueOf<AccountType>(it) }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let { enumValueOf<TransactionType>(it) }

    @TypeConverter
    fun fromDebtType(value: DebtType?): String? = value?.name

    @TypeConverter
    fun toDebtType(value: String?): DebtType? = value?.let { enumValueOf<DebtType>(it) }

    @TypeConverter
    fun fromFrequency(value: Frequency?): String? = value?.name

    @TypeConverter
    fun toFrequency(value: String?): Frequency? = value?.let { enumValueOf<Frequency>(it) }
}

@Database(
    entities = [
        User::class,
        UserAccount::class,
        Category::class,
        TransactionRecord::class,
        Budget::class,
        SavingsGoal::class,
        Contact::class,
        LoanDebt::class,
        LoanRepayment::class,
        Vendor::class,
        ShopCreditPurchase::class,
        ShopCreditPayment::class,
        RecurringRule::class,
        UserSettings::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun contactDao(): ContactDao
    abstract fun loanDebtDao(): LoanDebtDao
    abstract fun loanRepaymentDao(): LoanRepaymentDao
    abstract fun vendorDao(): VendorDao
    abstract fun shopCreditPurchaseDao(): ShopCreditPurchaseDao
    abstract fun shopCreditPaymentDao(): ShopCreditPaymentDao
    abstract fun recurringDao(): RecurringDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE loans_debts ADD COLUMN transactionId INTEGER DEFAULT NULL")
                } catch (e: Exception) {
                    // Ignored if column already exists
                }
                try {
                    db.execSQL("ALTER TABLE loan_repayments ADD COLUMN transactionId INTEGER DEFAULT NULL")
                } catch (e: Exception) {
                    // Ignored if column already exists
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hisab_database.db"
                )
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
