package com.rateio.data.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.rateio.data.persistence.entity.ExpenseEntity
import com.rateio.data.persistence.entity.ExpenseSplitEntity
import com.rateio.data.persistence.entity.ExpenseWithSplitsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Transaction
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY createdAtEpochMillis DESC")
    fun getExpensesWithSplitsFlow(groupId: String): Flow<List<ExpenseWithSplitsEntity>>

    /**
     * T29.1: a one-off (non-`Flow`) read of an expense by id — used only to pre-fill the edit
     * form once, when opening the screen (`CreateExpenseViewModel.loadExpenseForEditing`).
     * Deliberately doesn't reuse [getExpensesWithSplitsFlow] + `.first()` (subscribing to the
     * whole list just to grab one item and cancel the subscription right after): that "Flow
     * cancelled right after emitting" pattern, closely followed by a write to the same table
     * (`onSaveClick`), proved unstable under Robolectric's SQLite in testing (intermittent
     * deadlock) — a direct query avoids the `InvalidationTracker` entirely for this case.
     */
    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseWithSplitsById(expenseId: String): ExpenseWithSplitsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: String)

    /**
     * Replaces the expense + its splits in a single transaction. Necessary because `insertExpense`
     * is an upsert (`REPLACE`) and Room doesn't propagate that replace to the `expense_splits`
     * rows on its own — without `deleteSplitsForExpense` before reinserting, an edit that reduces
     * the number of participants in the split would leave orphaned splits from the expense's
     * previous version.
     */
    @Transaction
    suspend fun insertWithSplits(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>) {
        insertExpense(expense)
        deleteSplitsForExpense(expense.id)
        if (splits.isNotEmpty()) {
            insertSplits(splits)
        }
    }
}
