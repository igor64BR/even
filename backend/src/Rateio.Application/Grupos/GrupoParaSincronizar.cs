using Rateio.Domain;

namespace Rateio.Application.Grupos;

/// <summary>
/// Saída do mapeamento DTO→Domínio (<see cref="ConstrutorDeGrupoSincronizado"/>): o agregado
/// <see cref="Grupo"/> já validado (invariantes de T15 aplicadas via <see cref="Grupo.Criar"/>),
/// junto com o id do usuário autenticado que será o dono (RNF07 — nunca vem do payload, só do
/// JWT) e as despesas do grupo com os campos que <see cref="Despesa"/> não carrega por ser só a
/// representação mínima usada pelo motor de simplificação (descrição/data de lançamento são
/// dados de persistência, não de cálculo).
/// </summary>
public sealed record GrupoParaSincronizar(
    Grupo Grupo,
    Guid DonoUsuarioId,
    IReadOnlyList<DespesaParaSincronizar> Despesas);

/// <summary>
/// Uma despesa pronta para persistir: o registro de domínio (usado pelo motor de simplificação)
/// mais os campos que só existem na borda de persistência.
/// </summary>
public sealed record DespesaParaSincronizar(Despesa Despesa, string Descricao, DateOnly Data);
