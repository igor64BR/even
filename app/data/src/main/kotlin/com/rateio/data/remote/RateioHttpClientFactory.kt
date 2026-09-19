package com.rateio.data.remote

import com.rateio.data.remote.auth.AuthApi
import com.rateio.data.remote.groups.GroupsApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Fábrica do client HTTP do app. T12 criou isto só com o suficiente pra montar [AuthApi]; T19
 * acrescenta [GroupsApi] (`POST /groups/sync`) reaproveitando o mesmo `Retrofit.Builder`. Ainda
 * sem interceptor de `Authorization`/refresh automático: só dois endpoints existem hoje e cada um
 * resolve o próprio token explicitamente (login não precisa de nenhum; sync passa o
 * `Authorization` por parâmetro, ver [GroupsApi.sync]) — um interceptor genérico fica pra quando
 * houver refresh automático de token a justificar essa infraestrutura, não antes.
 */
object RateioHttpClientFactory {

    private val json = Json { ignoreUnknownKeys = true }

    fun createAuthApi(baseUrl: String): AuthApi = retrofit(baseUrl).create(AuthApi::class.java)

    fun createGroupsApi(baseUrl: String): GroupsApi = retrofit(baseUrl).create(GroupsApi::class.java)

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
