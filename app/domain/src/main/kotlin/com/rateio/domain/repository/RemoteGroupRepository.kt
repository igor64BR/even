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

    /**
     * Entra num grupo existente via código de convite (T21, RF07: `POST /groups/join/{codigo}`,
     * qualquer usuário autenticado pode entrar — o código válido É a autorização, não há checagem
     * de dono). Retorna o id remoto do grupo (`grupoId`), no mesmo formato de resposta de
     * [syncGroup].
     *
     * LACUNA conhecida (documentada em `specs/001-mvp-expense-splitting/tasks/T22-app-entrar-via-link.md`,
     * não inventada aqui): T21 não devolve nome, participantes nem despesas do grupo, e não existe
     * hoje um endpoint de "obter grupo por id" completo pra baixar esses dados depois de entrar.
     * Quem chama recebe só o [String] do id remoto e não tem como montar um
     * [com.rateio.domain.model.Group] local fiel (nome de verdade, participantes, despesas) sem
     * inventar dados — por isso a tela de confirmação (T22.2) mostra sucesso genérico e não insere
     * um grupo "fake" na lista local. Isso fica pra quando o backend ganhar esse endpoint.
     *
     * @throws GroupSyncException se não houver sessão autenticada, ou se a chamada de rede/HTTP
     * falhar — inclui código de convite inválido/expirado (`CodigoConviteInvalidoException` no
     * backend vira um erro HTTP genérico, T21 não distingue isso de outra falha num corpo
     * estruturado, então a mensagem aqui também fica genérica, mesma convenção de [syncGroup]).
     */
    suspend fun joinByCode(inviteCode: String): String
}

/**
 * Falha de sincronização já traduzida pra uma mensagem apresentável — mesma convenção de
 * [AuthenticationFailedException]: quem chama [RemoteGroupRepository.syncGroup] nunca vê um tipo
 * de rede, só a mensagem já pronta pra tela (e o erro original em [cause], pra log).
 */
class GroupSyncException(message: String, cause: Throwable? = null) : Exception(message, cause)
