package com.rateio.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rateio.data.persistence.dao.ExpenseDao
import com.rateio.data.persistence.dao.GroupDao
import com.rateio.data.persistence.dao.ParticipantDao
import com.rateio.data.persistence.entity.ExpenseEntity
import com.rateio.data.persistence.entity.GroupEntity
import com.rateio.data.persistence.entity.ParticipantEntity

/**
 * Banco Room local — fonte da verdade para grupos não sincronizados (constitution.md,
 * princípio 1), funciona 100% sem rede. Versão 1 é o esqueleto mínimo de T7
 * (Group/Participant/Expense); migrations entram conforme o schema crescer.
 *
 * `exportSchema = false`: histórico de schema para teste de migration é escopo de quando a
 * primeira migration real existir, não deste esqueleto inicial.
 */
@Database(
    entities = [GroupEntity::class, ParticipantEntity::class, ExpenseEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RateioDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun participantDao(): ParticipantDao
    abstract fun expenseDao(): ExpenseDao
}
