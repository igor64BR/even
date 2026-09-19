package com.rateio.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rateio.data.persistence.dao.ExpenseDao
import com.rateio.data.persistence.dao.GroupDao
import com.rateio.data.persistence.dao.ParticipantDao
import com.rateio.data.persistence.entity.ExpenseEntity
import com.rateio.data.persistence.entity.ExpenseSplitEntity
import com.rateio.data.persistence.entity.GroupEntity
import com.rateio.data.persistence.entity.ParticipantEntity

/**
 * Banco Room local — fonte da verdade para grupos não sincronizados (constitution.md,
 * princípio 1), funciona 100% sem rede.
 *
 * Versão 2 (T7B): adiciona `GroupEntity.isSynced` e a tabela `expense_splits`
 * (`ExpenseSplitEntity`). Versão 3 (T19.2): adiciona `GroupEntity.remoteId`, pra guardar o id do
 * grupo no servidor depois de sincronizado. Sem migration explícita e sem
 * `fallbackToDestructiveMigration` em nenhuma das duas — ainda não existe build distribuído
 * (nenhum usuário real a preservar), então recriar o schema do zero é seguro; uma migration real
 * entra assim que houver uma versão publicada para migrar a partir dela.
 *
 * `exportSchema = false`: histórico de schema para teste de migration é escopo de quando a
 * primeira migration real existir, não deste esqueleto inicial.
 */
@Database(
    entities = [
        GroupEntity::class,
        ParticipantEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class RateioDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun participantDao(): ParticipantDao
    abstract fun expenseDao(): ExpenseDao
}
