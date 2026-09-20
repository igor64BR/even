package com.rateio.data.repository

import com.rateio.data.local.auth.TokenStorage
import com.rateio.data.remote.groups.GroupsApi
import com.rateio.domain.model.Expense
import com.rateio.domain.repository.GroupSyncException
import com.rateio.domain.repository.RemoteExpenseRepository
import java.io.IOException
import retrofit2.HttpException

/**
 * Implementação de [RemoteExpenseRepository] sobre [GroupsApi] (`PUT`/`DELETE
 * /groups/{id}/expenses/{expenseId}`, T28/T29) + [TokenStorage] (T12.2, leitura do access token) —
 * mesmo padrão de [RemoteGroupSyncRepository] (T19.1): nenhum tipo de rede vaza pra `:domain`,
 * toda falha de sessão/rede/HTTP vira [GroupSyncException] com mensagem pronta pra tela.
 * [Expense.toSyncDto] (`ExpenseSyncMapper.kt`) é a mesma tradução que [RemoteGroupSyncRepository]
 * usa no bulk de `POST /groups/sync`.
 */
class RemoteExpenseSyncRepository(
    private val groupsApi: GroupsApi,
    private val tokenStorage: TokenStorage,
) : RemoteExpenseRepository {

    override suspend fun updateExpense(remoteGroupId: String, expense: Expense) {
        val accessToken = requireAccessToken(acao = "editar")

        try {
            groupsApi.updateExpense(
                bearerToken = "Bearer $accessToken",
                id = remoteGroupId,
                expenseId = expense.id,
                request = expense.toSyncDto(),
            )
        } catch (error: HttpException) {
            throw GroupSyncException("O servidor do Rateio recusou a edição da despesa.", error)
        } catch (error: IOException) {
            throw GroupSyncException("Sem conexão com o servidor do Rateio.", error)
        }
    }

    override suspend fun deleteExpense(remoteGroupId: String, expenseId: String) {
        val accessToken = requireAccessToken(acao = "excluir")

        try {
            groupsApi.deleteExpense(bearerToken = "Bearer $accessToken", id = remoteGroupId, expenseId = expenseId)
        } catch (error: HttpException) {
            throw GroupSyncException("O servidor do Rateio recusou a exclusão da despesa.", error)
        } catch (error: IOException) {
            throw GroupSyncException("Sem conexão com o servidor do Rateio.", error)
        }
    }

    private fun requireAccessToken(acao: String): String =
        tokenStorage.read()?.accessToken
            ?: throw GroupSyncException("É preciso estar autenticado para $acao uma despesa sincronizada.")
}
