package com.rateio.data.remote

import com.rateio.data.remote.auth.AuthApi
import com.rateio.data.remote.groups.GroupEventsApi
import com.rateio.data.remote.groups.GroupsApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Factory for the app's HTTP client. T12 created this with just enough to build [AuthApi]; T19
 * adds [GroupsApi] (`POST /groups/sync`), reusing the same `Retrofit.Builder`. T40.2 adds
 * [GroupEventsApi] (`GET /groups/{id}/events`, the T39 pull fallback). Still no
 * `Authorization`/automatic-refresh interceptor: each endpoint resolves its own token explicitly
 * (login needs none; the others pass `Authorization` as a parameter, see [GroupsApi.sync]) — a
 * generic interceptor is left for when automatic token refresh justifies that infrastructure, not
 * before.
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
