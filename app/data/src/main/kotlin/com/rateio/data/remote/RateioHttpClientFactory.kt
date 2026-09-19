package com.rateio.data.remote

import com.rateio.data.remote.auth.AuthApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Fábrica do client HTTP mínimo pedido por T12 — só o suficiente pra montar [AuthApi]. Não é um
 * client genérico reutilizável ainda: quando T19+ trouxer mais endpoints (grupos sincronizados,
 * SignalR), isto ganha interceptor de `Authorization`/refresh automático; antecipar isso agora
 * seria especular sobre requisitos que essas tasks ainda vão definir.
 */
object RateioHttpClientFactory {

    private val json = Json { ignoreUnknownKeys = true }

    fun createAuthApi(baseUrl: String): AuthApi = retrofit(baseUrl).create(AuthApi::class.java)

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
