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
     * T29.1: leitura pontual (não-`Flow`) de uma despesa por id — usada só pra pré-carregar o
     * formulário de edição uma vez, ao abrir a tela (`CreateExpenseViewModel.loadExpenseForEditing`).
     * Deliberadamente não reaproveita [getExpensesWithSplitsFlow] + `.first()` (assinar a lista
     * inteira só pra pegar um item e cancelar a assinatura em seguida): esse padrão de "Flow
     * cancelado logo depois de emitir", seguido de perto por uma escrita na mesma tabela
     * (`onSaveClick`), mostrou-se instável sob o SQLite do Robolectric em teste (deadlock
     * intermitente) — uma query direta evita o `InvalidationTracker` por completo pra esse caso.
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
