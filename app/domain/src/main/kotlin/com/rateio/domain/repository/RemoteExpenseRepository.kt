package com.rateio.domain.repository

import com.rateio.domain.model.Expense

/**
 * Contrato de propagação de edição/exclusão de uma despesa pro backend, pra um grupo já
 * sincronizado (T29, RF... — edição/exclusão de despesa). `:domain` declara, `:data` implementa
 * sobre Retrofit — nenhum tipo de rede (Retrofit/OkHttp) vaza pra esta interface, mesma Dependency
 * Inversion de [RemoteGroupRepository].
 *
 * Interface separada de [RemoteGroupRepository] (Interface Segregation): sincronizar um grupo
 * inteiro (bulk, primeira sincronização) e editar/excluir uma despesa avulsa já sincronizada são
 * operações com formas bem diferentes — [RemoteGroupRepository.syncGroup] recebe o grupo inteiro
 * (participantes + despesas), aqui só a despesa que mudou.
 *
 * A ação só faz sentido pra um [com.rateio.domain.model.Group] com `isSynced = true` — quem chama
 * garante isso antes de invocar (mesma convenção de [RemoteGroupRepository.syncGroup]: local-first,
 * a mudança local já aconteceu antes desta chamada, e uma falha aqui nunca desfaz a mudança local —
 * ver `T29-app-editar-excluir-despesa.md`).
 *
 * PENDÊNCIA conhecida (documentada, não inventada): T29 roda em paralelo com T28 (backend,
 * `PUT`/`DELETE /groups/{id}/expenses/{expenseId}`). A implementação em `:data`
 * ([com.rateio.data.repository.RemoteExpenseSyncRepository]) foi escrita contra o contrato já
 * documentado na task (mesmo formato de `DespesaSincronizadaRequest` que
 * `POST /groups/{id}/expenses`, T23.1, já usa) — se a rota/payload real divergir quando T28
 * fechar, só o lado `:data` precisa mudar, esta interface não.
 */
interface RemoteExpenseRepository {

    /**
     * Envia a versão atual de [expense] (já persistida localmente) pro backend, substituindo a
     * despesa de id [Expense.id] dentro do grupo remoto [remoteGroupId].
     *
     * @throws GroupSyncException se não houver sessão autenticada, ou se a chamada de rede/HTTP
     * falhar — nunca deixa uma exceção de Retrofit/OkHttp vazar pra quem chama.
     */
    suspend fun updateExpense(remoteGroupId: String, expense: Expense)

    /**
     * Remove a despesa [expenseId] do grupo remoto [remoteGroupId].
     *
     * @throws GroupSyncException se não houver sessão autenticada, ou se a chamada de rede/HTTP
     * falhar — nunca deixa uma exceção de Retrofit/OkHttp vazar pra quem chama.
     */
    suspend fun deleteExpense(remoteGroupId: String, expenseId: String)
}
