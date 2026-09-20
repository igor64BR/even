using Rateio.Application.Grupos;
using Rateio.Domain;

namespace Rateio.Api.Contracts;

/// <summary>
/// Resposta de sucesso de <c>GET /groups/{id}</c> (T28.3) — fecha a lacuna reportada por T22:
/// <c>POST /groups/join/{codigo}</c> (T21, <see cref="SincronizarGrupoResponse"/>-like) só devolve
/// <c>{grupoId}</c>, sem jeito de o app buscar o resto dos dados do grupo depois de entrar via link.
///
/// Mesmo formato já estabelecido pelo payload de sincronização (<c>SincronizarGrupoRequest</c>,
/// T18.1: participante com id/nome/convidado-ou-não, despesa com descrição/valor/pagador/data/
/// divisão) — deliberadamente não um formato de response paralelo, só o espelho de saída do mesmo
/// contrato que já entra pelo <c>POST /groups/sync</c>.
/// </summary>
public sealed record ObterGrupoResponse(
    string Nome,
    CategoriaGrupo Categoria,
    IReadOnlyList<ParticipanteResponse> Participantes,
    IReadOnlyList<DespesaDetalhadaResponse> Despesas);

/// <summary>Participante do grupo (RF06/RF07) — id/nome/convidado-ou-não, igual à borda de entrada.</summary>
public sealed record ParticipanteResponse(Guid Id, string Nome, bool EhConvidado);

/// <summary>
/// Despesa do grupo com tudo que o app precisa pra popular a tela: descrição, valor, pagador, data
/// de lançamento e a divisão completa entre participantes.
/// </summary>
public sealed record DespesaDetalhadaResponse(
    Guid Id,
    string Descricao,
    long ValorTotalCentavos,
    Guid PagadorId,
    DateOnly Data,
    TipoDivisaoRequest TipoDivisao,
    IReadOnlyList<ParticipacaoResponse> Participacoes);

/// <summary>
/// A parte de um participante numa despesa. <see cref="Peso"/>/<see cref="ValorCentavos"/> só vêm
/// preenchidos quando <see cref="DespesaDetalhadaResponse.TipoDivisao"/> é, respectivamente,
/// <see cref="TipoDivisaoRequest.PorPeso"/>/<see cref="TipoDivisaoRequest.PorValorFixo"/> — mesma
/// convenção de <c>ParticipacaoSincronizadaRequest</c>.
/// </summary>
public sealed record ParticipacaoResponse(Guid ParticipanteId, long? Peso, long? ValorCentavos);
