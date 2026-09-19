package com.rateio.domain.repository

import com.rateio.domain.model.Expense
import com.rateio.domain.model.Group
import com.rateio.domain.model.Participant

/**
 * Contrato de sincronização de um grupo local pro backend (T19, RF09: `POST /groups/sync`, T18).
 * `:domain` declara, `:data` implementa sobre Retrofit — nenhum tipo de rede (Retrofit/OkHttp)
 * vaza pra esta interface (Dependency Inversion, mesma convenção de [AuthRepository]).
 *
 * A ação só faz sentido pra usuário autenticado (não existe "sincronizar sem estar logado" —
 * ver T19-app-sincronizar-grupo.md); quem chama garante isso na UI (só mostra a ação com sessão
 * ativa). A implementação ainda valida de novo antes de chamar a rede, pra nunca depender só da UI
 * não deixar passar.
 */
interface RemoteGroupRepository {

    /**
     * Envia o estado local completo do grupo (grupo + participantes + despesas já lançadas) pro
     * backend pela primeira vez. Retorna o id remoto atribuído pelo servidor
     * (`SincronizarGrupoResponse.GrupoId`) — quem chama é responsável por persistir esse valor em
     * [Group.remoteId] e marcar [Group.isSynced]; esta função não muda nenhum estado local, só
     * fala com o backend.
     *
     * @throws GroupSyncException se não houver sessão autenticada, ou se a chamada de rede/HTTP
     * falhar — nunca deixa uma exceção de Retrofit/OkHttp vazar pra quem chama.
     */
    suspend fun syncGroup(group: Group, participants: List<Participant>, expenses: List<Expense>): String
}

/**
 * Falha de sincronização já traduzida pra uma mensagem apresentável — mesma convenção de
 * [AuthenticationFailedException]: quem chama [RemoteGroupRepository.syncGroup] nunca vê um tipo
 * de rede, só a mensagem já pronta pra tela (e o erro original em [cause], pra log).
 */
class GroupSyncException(message: String, cause: Throwable? = null) : Exception(message, cause)
