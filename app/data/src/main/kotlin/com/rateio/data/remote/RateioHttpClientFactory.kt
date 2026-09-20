package com.rateio.data.remote

import com.rateio.data.remote.auth.AuthApi
import com.rateio.data.remote.groups.GroupEventsApi
import com.rateio.data.remote.groups.GroupsApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Fábrica do client HTTP do app. T12 criou isto só com o suficiente pra montar [AuthApi]; T19
 * acrescenta [GroupsApi] (`POST /groups/sync`) reaproveitando o mesmo `Retrofit.Builder`. T40.2
 * acrescenta [GroupEventsApi] (`GET /groups/{id}/events`, fallback de pull T39). Ainda sem
 * interceptor de `Authorization`/refresh automático: cada endpoint resolve o próprio token
 * explicitamente (login não precisa de nenhum; os demais passam o `Authorization` por parâmetro,
 * ver [GroupsApi.sync]) — um interceptor genérico fica pra quando houver refresh automático de
 * token a justificar essa infraestrutura, não antes.
 */
object RateioHttpClientFactory {

    private val json = Json { ignoreUnknownKeys = true }

    fun createAuthApi(baseUrl: String): AuthApi = retrofit(baseUrl).create(AuthApi::class.java)

    fun createGroupsApi(baseUrl: String): GroupsApi = retrofit(baseUrl).create(GroupsApi::class.java)

    fun createGroupEventsApi(baseUrl: String): GroupEventsApi = retrofit(baseUrl).create(GroupEventsApi::class.java)

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
