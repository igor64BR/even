package com.tally.data.remote

import com.tally.data.remote.auth.AuthApi
import com.tally.data.remote.groups.GroupEventsApi
import com.tally.data.remote.groups.GroupsApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Factory for the app's HTTP client. Builds [AuthApi]; also builds
 * [GroupsApi] (`POST /groups/sync`), reusing the same `Retrofit.Builder`, and
 * [GroupEventsApi] (`GET /groups/{id}/events`, the pull fallback). Still no
 * `Authorization`/automatic-refresh interceptor: each endpoint resolves its own token explicitly
 * (login needs none; the others pass `Authorization` as a parameter, see [GroupsApi.sync]) — a
 * generic interceptor is left for when automatic token refresh justifies that infrastructure, not
 * before.
 */
object TallyHttpClientFactory {

    private val json = Json { ignoreUnknownKeys = true }

    fun createAuthApi(baseUrl: String): AuthApi = retrofit(baseUrl).create(AuthApi::class.java)

    fun createGroupsApi(baseUrl: String): GroupsApi = retrofit(baseUrl).create(GroupsApi::class.java)

    fun createGroupEventsApi(baseUrl: String): GroupEventsApi = retrofit(baseUrl).create(GroupEventsApi::class.java)

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
