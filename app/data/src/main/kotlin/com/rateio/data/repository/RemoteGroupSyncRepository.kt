package com.rateio.data.repository

import com.rateio.data.local.auth.TokenStorage
import com.rateio.data.remote.groups.GroupsApi
import com.rateio.data.remote.groups.ParticipanteSincronizadoDto
import com.rateio.data.remote.groups.SincronizarGrupoRequestDto
import com.rateio.domain.model.Expense
import com.rateio.domain.model.Group
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.GroupSyncException
import com.rateio.domain.repository.RemoteGroupRepository
import java.io.IOException
import retrofit2.HttpException

/**
 * Implementação de [RemoteGroupRepository] sobre [GroupsApi] (`POST /groups/sync`, T18) +
 * [TokenStorage] (T12.2, leitura do access token) — T19.1.
 *
 * Duas lacunas de modelo herdadas de tasks anteriores, resolvidas aqui com o valor mais honesto
 * disponível (documentado, não inventado):
 * - [Group] (`:domain`, T7B) ainda não modela categoria (mesma lacuna que `CreateGroupViewModel`,
 *   T16, já registrou: a categoria escolhida no formulário não é persistida). Todo grupo
 *   sincroniza com [CATEGORIA_GRUPO_OUTRO] até uma task futura adicionar o campo de verdade.
 * - [Participant] (`:domain`) não modela vínculo com conta própria — o local-first do projeto
 *   (constitution.md, princípio 1) só exige nome. Todo participante sincroniza como convidado
 *   (`ehConvidado = true`); não existe hoje um jeito de um `Participant` corresponder a um usuário
 *   autenticado diferente do dono do aparelho.
 */
class RemoteGroupSyncRepository(
    private val groupsApi: GroupsApi,
    private val tokenStorage: TokenStorage,
) : RemoteGroupRepository {

    override suspend fun syncGroup(group: Group, participants: List<Participant>, expenses: List<Expense>): String {
        val accessToken = tokenStorage.read()?.accessToken
            ?: throw GroupSyncException("É preciso estar autenticado para sincronizar um grupo.")

        try {
            val response = groupsApi.sync(
                bearerToken = "Bearer $accessToken",
                request = group.toSyncRequest(participants, expenses),
            )
            return response.grupoId
        } catch (error: HttpException) {
            throw GroupSyncException("O servidor do Rateio recusou a sincronização.", error)
        } catch (error: IOException) {
            throw GroupSyncException("Sem conexão com o servidor do Rateio.", error)
        }
    }

    override suspend fun joinByCode(inviteCode: String): String {
        val accessToken = tokenStorage.read()?.accessToken
            ?: throw GroupSyncException("É preciso estar autenticado para entrar num grupo.")

        try {
            val response = groupsApi.join(bearerToken = "Bearer $accessToken", codigo = inviteCode)
            return response.grupoId
        } catch (error: HttpException) {
            throw GroupSyncException("Não foi possível entrar nesse grupo — verifique o código.", error)
        } catch (error: IOException) {
            throw GroupSyncException("Sem conexão com o servidor do Rateio.", error)
        }
    }
}

private fun Group.toSyncRequest(participants: List<Participant>, expenses: List<Expense>) =
    SincronizarGrupoRequestDto(
        nome = name,
        categoria = CATEGORIA_GRUPO_OUTRO,
        participantes = participants.map(Participant::toSyncDto),
        despesas = expenses.map(Expense::toSyncDto),
    )

private fun Participant.toSyncDto() = ParticipanteSincronizadoDto(
    id = id,
    nome = name,
    ehConvidado = true,
)

// Expense.toSyncDto()/tradução de splits vivem em ExpenseSyncMapper.kt (mesmo pacote,
// `internal` — compartilhado com RemoteExpenseSyncRepository, T29, pra não duplicar a mesma
// tradução Expense -> DespesaSincronizadaDto nos dois lugares).

// Espelha o valor ordinal de CategoriaGrupo.Outro (C#) — o backend não registra
// JsonStringEnumConverter, então System.Text.Json serializa/desserializa enum como Int.
private const val CATEGORIA_GRUPO_OUTRO = 4
