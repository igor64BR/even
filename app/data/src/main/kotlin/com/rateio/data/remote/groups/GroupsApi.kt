package com.rateio.data.remote.groups

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Espelha `POST /groups/sync` (backend: `GruposController.Sync`,
 * `backend/src/Rateio.Api/Controllers/GruposController.cs`, T18) — primeira sincronização de um
 * grupo local pro backend (RF09). Mesmo padrão Retrofit de
 * [com.rateio.data.remote.auth.AuthApi] (T12): interface fina, corpo/resposta em DTOs
 * `@Serializable` que espelham os records C# campo a campo (System.Text.Json/ASP.NET Core serializa
 * em camelCase por padrão, sem conversor de enum registrado — por isso `categoria`/`tipoDivisao`
 * vão como `Int`, o valor ordinal do enum C#, não como string).
 *
 * O endpoint exige JWT válido (`[Authorize]` no controller) — o `Authorization` vai por parâmetro
 * explícito (não por interceptor OkHttp global) porque hoje só este endpoint precisa dele; ver
 * decisão registrada em [com.rateio.data.remote.RateioHttpClientFactory].
 */
interface GroupsApi {
    @POST("groups/sync")
    suspend fun sync(
        @Header("Authorization") bearerToken: String,
        @Body request: SincronizarGrupoRequestDto,
    ): SincronizarGrupoResponseDto

    /**
     * Espelha `POST /groups/join/{codigo}` (backend: `GruposController.EntrarComCodigo`, T21.2) —
     * entra num grupo existente via código de convite (RF07). Resposta no mesmo formato de [sync]
     * (`SincronizarGrupoResponse(GrupoId)`), reaproveitado aqui em vez de um DTO próprio porque o
     * corpo é idêntico.
     */
    @POST("groups/join/{codigo}")
    suspend fun join(
        @Header("Authorization") bearerToken: String,
        @Path("codigo") codigo: String,
    ): SincronizarGrupoResponseDto
}

/** Corpo de `POST /groups/sync` — espelha `SincronizarGrupoRequest` do backend. */
@Serializable
data class SincronizarGrupoRequestDto(
    val nome: String,
    val categoria: Int,
    val participantes: List<ParticipanteSincronizadoDto>,
    val despesas: List<DespesaSincronizadaDto>,
)

/** Espelha `ParticipanteSincronizadoRequest(Id, Nome, EhConvidado)`. */
@Serializable
data class ParticipanteSincronizadoDto(
    val id: String,
    val nome: String,
    val ehConvidado: Boolean,
)

/**
 * Espelha `DespesaSincronizadaRequest`. [data] é `yyyy-MM-dd` (o formato padrão que
 * `System.Text.Json` usa pra `DateOnly`).
 */
@Serializable
data class DespesaSincronizadaDto(
    val id: String,
    val descricao: String,
    val valorTotalCentavos: Long,
    val pagadorId: String,
    val data: String,
    val tipoDivisao: Int,
    val participacoes: List<ParticipacaoSincronizadaDto>,
)

/**
 * Espelha `ParticipacaoSincronizadaRequest`. [peso] só é preenchido para divisão por peso,
 * [valorCentavos] só para divisão por valor fixo — mutuamente exclusivos, igual ao contrato do
 * backend.
 */
@Serializable
data class ParticipacaoSincronizadaDto(
    val participanteId: String,
    val peso: Long? = null,
    val valorCentavos: Long? = null,
)

/** Resposta de sucesso — espelha `SincronizarGrupoResponse(GrupoId)`. */
@Serializable
data class SincronizarGrupoResponseDto(val grupoId: String)
