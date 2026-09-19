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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: String)

    /**
     * Substitui despesa + partes numa única transação. Necessário porque `insertExpense` é um
     * upsert (`REPLACE`) e Room não propaga esse replace pras linhas de `expense_splits`
     * sozinho — sem o `deleteSplitsForExpense` antes de reinserir, uma edição que reduz o número
     * de participantes da divisão deixaria partes órfãs da versão anterior da despesa.
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
