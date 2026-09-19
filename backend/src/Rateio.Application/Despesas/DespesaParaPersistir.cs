using Rateio.Domain;

namespace Rateio.Application.Despesas;

/// <summary>
/// Uma despesa pronta para persistir: o registro de domínio (usado pelo motor de simplificação)
/// mais os campos que só existem na borda de persistência (descrição, data de lançamento).
/// Extraído de <c>Rateio.Application.Grupos.DespesaParaSincronizar</c> (T18.1) para ser
/// compartilhado com T23 (<c>POST /groups/{id}/expenses</c>) — tanto o bulk de sincronização
/// (<c>GrupoParaSincronizar.Despesas</c>) quanto o endpoint de despesa avulsa
/// (<see cref="IDespesaRepository.AdicionarAsync"/>) persistem exatamente essa mesma forma.
/// </summary>
public sealed record DespesaParaPersistir(Despesa Despesa, string Descricao, DateOnly Data);
