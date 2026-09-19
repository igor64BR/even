using Rateio.Application.Despesas;
using Rateio.Domain;

namespace Rateio.Application.Grupos;

/// <summary>
/// Saída do mapeamento DTO→Domínio (<see cref="ConstrutorDeGrupoSincronizado"/>): o agregado
/// <see cref="Grupo"/> já validado (invariantes de T15 aplicadas via <see cref="Grupo.Criar"/>),
/// junto com o id do usuário autenticado que será o dono (RNF07 — nunca vem do payload, só do
/// JWT) e as despesas do grupo (<see cref="DespesaParaPersistir"/> — mesmo tipo que T23 usa pra
/// despesa avulsa, ver comentário daquele tipo pra por que é compartilhado).
/// </summary>
public sealed record GrupoParaSincronizar(
    Grupo Grupo,
    Guid DonoUsuarioId,
    IReadOnlyList<DespesaParaPersistir> Despesas);
