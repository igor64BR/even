using Rateio.Domain;

namespace Rateio.Application.Grupos;

/// <summary>
/// Leitura mínima de um grupo existente, originalmente usada só por
/// <see cref="EntrarNoGrupoViaConviteUseCase"/> (T21.2) pra reconstruir um <see cref="Grupo"/> de
/// domínio suficiente pra chamar <see cref="Grupo.AdicionarParticipante"/> (T15) — que precisa da
/// lista atual de participantes pra revalidar a invariante de "sem participante duplicado".
///
/// Não é o agregado completo (sem despesas, sem <see cref="Grupo.Sincronizado"/> real): o único uso
/// que <see cref="EntrarNoGrupoViaConviteUseCase"/> faz do <see cref="Grupo"/> reconstruído a partir
/// daqui é chamar <c>AdicionarParticipante</c> e descartar o resto — persistir a entrada de fato usa
/// <see cref="IGrupoRepository.AdicionarParticipanteAsync"/> direto pelo <see cref="Guid"/> real do
/// grupo (mesmo padrão de <c>Rateio.Infrastructure.Despesas.DespesaRepository</c>: insere pela FK
/// sem recarregar o agregado inteiro).
///
/// T28.3 reaproveita este mesmo tipo pra metade de <see cref="GrupoCompleto"/> (nome/categoria/
/// participantes de <c>GET /groups/{id}</c>) em vez de introduzir uma segunda leitura com o mesmo
/// formato — quem chama já conhece o <see cref="Guid"/> real do grupo pela rota/parâmetro, então
/// não faz falta este tipo carregar o id.
/// </summary>
public sealed record GrupoParaEntrada(NomeGrupo Nome, CategoriaGrupo Categoria, IReadOnlyList<Participante> Participantes);
