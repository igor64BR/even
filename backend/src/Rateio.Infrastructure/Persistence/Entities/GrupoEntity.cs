using Rateio.Domain;

namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Mapeamento de persistência de um grupo. Esqueleto mínimo (T2.2) até T18: ganhou aqui
/// <see cref="Categoria"/>, <see cref="Sincronizado"/> e <see cref="DonoUsuarioId"/> porque T18 é a
/// primeira vez que algo persiste um <see cref="Grupo"/> de domínio completo — antes disso nada no
/// projeto escrevia essas colunas. <see cref="Categoria"/> reaproveita o enum de domínio
/// diretamente (sem duplicar como string/segundo enum): é um valor fechado sem comportamento, então
/// não há o risco de divergência que levou T31 a não duplicar o tipo de divisão de despesa.
/// </summary>
public class GrupoEntity
{
    public Guid Id { get; set; }

    public string Nome { get; set; } = string.Empty;

    public CategoriaGrupo Categoria { get; set; }

    /// <summary>true assim que o grupo passa por <c>POST /groups/sync</c> (RF09/T18.2) — dali em
    /// diante o servidor é fonte da verdade para este grupo.</summary>
    public bool Sincronizado { get; set; }

    /// <summary>
    /// Dono/admin do grupo (RNF07: só o próprio usuário autenticado pode sincronizar em nome
    /// dele mesmo — nunca vem do corpo da requisição, sempre do claim <c>sub</c> do JWT).
    /// </summary>
    public Guid DonoUsuarioId { get; set; }

    public DateTimeOffset CriadoEm { get; set; }

    public List<ParticipanteEntity> Participantes { get; set; } = [];

    public List<DespesaEntity> Despesas { get; set; } = [];

    public UsuarioEntity? Dono { get; set; }
}
