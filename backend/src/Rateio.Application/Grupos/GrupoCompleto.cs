using Rateio.Application.Despesas;

namespace Rateio.Application.Grupos;

/// <summary>
/// T28.3: saída de <see cref="ObterGrupoUseCase"/> — tudo que <c>GET /groups/{id}</c> precisa pra
/// popular a tela do app depois de entrar via link (lacuna reportada por T22:
/// <c>POST /groups/join/{codigo}</c> só devolve <c>{grupoId}</c>). Composição de dois tipos já
/// estabelecidos em vez de um formato paralelo: <see cref="Grupo"/> reaproveita a mesma leitura
/// mínima de nome/categoria/participantes que <see cref="EntrarNoGrupoViaConviteUseCase"/> (T21.2)
/// já usa (<see cref="GrupoParaEntrada"/>), e <see cref="Despesas"/> reaproveita
/// <see cref="DespesaParaPersistir"/> — o mesmo tipo que T18/T23 usam pra persistir uma despesa,
/// aqui devolvido pela via de leitura <see cref="IDespesaRepository.ObterDetalhadasPorGrupoAsync"/>.
/// </summary>
public sealed record GrupoCompleto(GrupoParaEntrada Grupo, IReadOnlyList<DespesaParaPersistir> Despesas);
