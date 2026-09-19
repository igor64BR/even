package com.rateio.app.di

import android.content.Context
import androidx.room.Room
import com.rateio.data.persistence.RateioDatabase
import com.rateio.data.repository.RoomGroupRepository
import com.rateio.data.repository.RoomParticipantRepository
import com.rateio.domain.repository.GroupRepository
import com.rateio.domain.repository.ParticipantRepository

/**
 * Raiz de composição manual do módulo `:app` — não há framework de DI no projeto ainda. Monta o
 * Room de `:data` uma única vez por processo e expõe só os contratos de `:domain` que as telas
 * consomem (Dependency Inversion: telas dependem de interface, não de `RoomGroupRepository`).
 */
class AppContainer(context: Context) {

    private val database: RateioDatabase = Room.databaseBuilder(
        context.applicationContext,
        RateioDatabase::class.java,
        DATABASE_NAME,
    ).build()

    val groupRepository: GroupRepository by lazy { RoomGroupRepository(database.groupDao()) }
    val participantRepository: ParticipantRepository by lazy { RoomParticipantRepository(database.participantDao()) }

    private companion object {
        const val DATABASE_NAME = "rateio.db"
    }
}
