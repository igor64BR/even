using Rateio.Domain;

namespace Rateio.Application.Grupos;

/// <summary>
/// Leitura mínima de um grupo existente usada só por <see cref="EntrarNoGrupoViaConviteUseCase"/>
/// (T21.2) pra reconstruir um <see cref="Grupo"/> de domínio suficiente pra chamar
/// <see cref="Grupo.AdicionarParticipante"/> (T15) — que precisa da lista atual de participantes
/// pra revalidar a invariante de "sem participante duplicado".
///
/// Não é o agregado completo (sem despesas, sem <see cref="Grupo.Sincronizado"/> real): o único uso
/// que <see cref="EntrarNoGrupoViaConviteUseCase"/> faz do <see cref="Grupo"/> reconstruído a partir
/// daqui é chamar <c>AdicionarParticipante</c> e descartar o resto — persistir a entrada de fato usa
/// <see cref="IGrupoRepository.AdicionarParticipanteAsync"/> direto pelo <see cref="Guid"/> real do
/// grupo (mesmo padrão de <c>Rateio.Infrastructure.Despesas.DespesaRepository</c>: insere pela FK
/// sem recarregar o agregado inteiro).
/// </summary>
public sealed record GrupoParaEntrada(NomeGrupo Nome, CategoriaGrupo Categoria, IReadOnlyList<Participante> Participantes);
