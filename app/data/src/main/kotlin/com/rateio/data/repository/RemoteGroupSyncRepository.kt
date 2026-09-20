package com.rateio.data.repository

import com.rateio.data.local.auth.TokenStorage
import com.rateio.data.remote.groups.DespesaSincronizadaDto
import com.rateio.data.remote.groups.GroupsApi
import com.rateio.data.remote.groups.ParticipacaoSincronizadaDto
import com.rateio.data.remote.groups.ParticipanteSincronizadoDto
import com.rateio.data.remote.groups.SincronizarGrupoRequestDto
import com.rateio.domain.model.Expense
import com.rateio.domain.model.ExpenseSplit
import com.rateio.domain.model.Group
import com.rateio.domain.model.Participant
import com.rateio.domain.repository.GroupSyncException
import com.rateio.domain.repository.RemoteGroupRepository
import java.io.IOException
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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

private fun Expense.toSyncDto() = DespesaSincronizadaDto(
    id = id,
    descricao = description,
    valorTotalCentavos = amountCents,
    pagadorId = paidByParticipantId,
    data = dataIsoFormatter.format(createdAt.atZone(ZoneOffset.UTC).toLocalDate()),
    tipoDivisao = tipoDivisaoOrdinalFor(splits),
    participacoes = splits.map(ExpenseSplit::toSyncDto),
)

private fun ExpenseSplit.toSyncDto(): ParticipacaoSincronizadaDto = when (this) {
    is ExpenseSplit.Equal -> ParticipacaoSincronizadaDto(participanteId = participantId)
    is ExpenseSplit.Weight -> ParticipacaoSincronizadaDto(participanteId = participantId, peso = weight)
    is ExpenseSplit.FixedAmount ->
        ParticipacaoSincronizadaDto(participanteId = participantId, valorCentavos = amount.cents)
}

/**
 * `TipoDivisao` é único por despesa, não por participação (mesma regra do
 * `DespesaSincronizadaRequest` do backend) — deriva do primeiro split. Lista vazia (despesa sem
 * divisão lançada; não existe ainda tela que crie `Expense` com splits, ver `Expense.kt`) cai no
 * default seguro [TIPO_DIVISAO_POR_IGUAL].
 */
private fun tipoDivisaoOrdinalFor(splits: List<ExpenseSplit>): Int = when (splits.firstOrNull()) {
    is ExpenseSplit.Weight -> TIPO_DIVISAO_POR_PESO
    is ExpenseSplit.FixedAmount -> TIPO_DIVISAO_POR_VALOR_FIXO
    is ExpenseSplit.Equal, null -> TIPO_DIVISAO_POR_IGUAL
}

private val dataIsoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

// Espelham os valores ordinais dos enums C# (CategoriaGrupo, TipoDivisaoRequest) — o backend não
// registra JsonStringEnumConverter, então System.Text.Json serializa/desserializa enum como Int.
private const val CATEGORIA_GRUPO_OUTRO = 4
private const val TIPO_DIVISAO_POR_IGUAL = 0
private const val TIPO_DIVISAO_POR_PESO = 1
private const val TIPO_DIVISAO_POR_VALOR_FIXO = 2
