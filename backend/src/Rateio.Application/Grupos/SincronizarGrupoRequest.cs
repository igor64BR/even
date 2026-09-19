using Rateio.Domain;

namespace Rateio.Application.Grupos;

/// <summary>
/// Payload de <c>POST /groups/sync</c> (T18.1): o estado local completo de um grupo (Room, app)
/// que ainda nunca tocou o backend. Não carrega nenhum campo de "dono"/"usuário" — quem sincroniza
/// é sempre o usuário autenticado do próprio JWT (RNF07), nunca um valor recebido no corpo da
/// requisição, então não existe aqui um jeito de um usuário sincronizar em nome de outro.
///
/// Vive em Application (não em Api.Contracts) porque é consumido diretamente pelo
/// <see cref="SincronizarGrupoUseCase"/> — ver <see cref="ConstrutorDeGrupoSincronizado"/> para o
/// mapeamento DTO→Domínio.
/// </summary>
public sealed record SincronizarGrupoRequest(
    string Nome,
    CategoriaGrupo Categoria,
    IReadOnlyList<ParticipanteSincronizadoRequest> Participantes,
    IReadOnlyList<DespesaSincronizadaRequest> Despesas);

/// <summary>
/// Participante do grupo local. <see cref="Id"/> é o identificador estável já atribuído pelo app
/// (Room) — precisa ser preservado tal como veio, porque <see cref="DespesaSincronizadaRequest.PagadorId"/>
/// e <see cref="ParticipacaoSincronizadaRequest.ParticipanteId"/> referenciam esse mesmo valor
/// dentro do payload.
/// </summary>
public sealed record ParticipanteSincronizadoRequest(Guid Id, string Nome, bool EhConvidado);

/// <summary>
/// Despesa lançada localmente. <see cref="TipoDivisao"/> é único por despesa (não por
/// participação) — reflete a regra de domínio de que todas as participações de uma mesma despesa
/// são do mesmo subtipo concreto de <see cref="ParticipacaoDespesa"/> (ver o comentário desse
/// tipo); validar/impor essa consistência na borda de entrada é exatamente o papel deste DTO.
/// </summary>
public sealed record DespesaSincronizadaRequest(
    Guid Id,
    string Descricao,
    long ValorTotalCentavos,
    Guid PagadorId,
    DateOnly Data,
    TipoDivisaoRequest TipoDivisao,
    IReadOnlyList<ParticipacaoSincronizadaRequest> Participacoes);

/// <summary>Espelha os três subtipos concretos de <see cref="ParticipacaoDespesa"/> (RF17/18/19).</summary>
public enum TipoDivisaoRequest
{
    PorIgual,
    PorPeso,
    PorValorFixo,
}

/// <summary>
/// A parte de um participante numa despesa. <see cref="Peso"/> só é obrigatório quando
/// <see cref="DespesaSincronizadaRequest.TipoDivisao"/> é <see cref="TipoDivisaoRequest.PorPeso"/>;
/// <see cref="ValorCentavos"/> só quando é <see cref="TipoDivisaoRequest.PorValorFixo"/>. Para
/// <see cref="TipoDivisaoRequest.PorIgual"/> nenhum dos dois é usado.
/// </summary>
public sealed record ParticipacaoSincronizadaRequest(Guid ParticipanteId, long? Peso, long? ValorCentavos);
