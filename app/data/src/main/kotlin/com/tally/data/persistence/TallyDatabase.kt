package com.tally.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tally.data.persistence.dao.ExpenseDao
import com.tally.data.persistence.dao.GroupDao
import com.tally.data.persistence.dao.NotificationDao
import com.tally.data.persistence.dao.ParticipantDao
import com.tally.data.persistence.dao.SettlementDao
import com.tally.data.persistence.entity.ExpenseEntity
import com.tally.data.persistence.entity.ExpenseSplitEntity
import com.tally.data.persistence.entity.GroupEntity
import com.tally.data.persistence.entity.NotificationEntity
import com.tally.data.persistence.entity.ParticipantEntity
import com.tally.data.persistence.entity.SettlementEntity

/**
 * Local Room database — source of truth for unsynced groups, works 100% offline.
 *
 * Version 2 adds `GroupEntity.isSynced` and the `expense_splits` table
 * (`ExpenseSplitEntity`). Version 3 adds `GroupEntity.remoteId`, to store the group's id
 * on the server once synced. Version 4 adds the `settlements` table
 * (`SettlementEntity`) — the simplification engine already existed, but there was nowhere to
 * persist a settlement recorded from the "Settle debts" screen. Version 5 adds the
 * `notifications` table (`NotificationEntity`) — local persistence of group events received via
 * SignalR or recovered by the pull fallback. Version 6 adds
 * `SettlementEntity.createdAtEpochMillis`, needed to order the "Settlement history" screen
 * by date. No explicit migration and no `fallbackToDestructiveMigration` on any of
 * these — there's still no distributed build (no real user to preserve), so recreating the schema
 * from scratch is safe; a real migration arrives once there's a published version to migrate from.
 *
 * `exportSchema = false`: a schema history for migration testing is the scope of once the first
 * real migration exists, not this initial skeleton.
 */
@Database(
    entities = [
        GroupEntity::class,
        ParticipantEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        SettlementEntity::class,
        NotificationEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class TallyDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun participantDao(): ParticipantDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao
    abstract fun notificationDao(): NotificationDao
}
