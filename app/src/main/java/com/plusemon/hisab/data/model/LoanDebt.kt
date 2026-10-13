package com.plusemon.hisab.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DebtType(val labelEn: String, val labelBn: String) {
    OWED_TO_ME("Gave Money", "পাওনা"),
    I_OWE("Took Money", "দেনা")
}

@Entity(
    tableName = "loans_debts",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserAccount::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("accountId")]
)
data class LoanDebt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val contactId: Long = 0,
    val personName: String,
    val accountId: Long = 1,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val type: DebtType,
    val dueDate: Long? = null,
    val note: String = "",
    val phone: String = "",
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val transactionId: Long? = null
) {
    val remainingAmount: Double
        get() = (amount - paidAmount).coerceAtLeast(0.0)
}
