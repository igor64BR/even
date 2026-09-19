namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Mapeamento de persistência de um participante de grupo. Esqueleto mínimo (T2.2) — não confundir
/// com <see cref="Rateio.Domain.ParticipanteId"/>, que é só o identificador wrapped usado pelo
/// motor de simplificação; esta entidade guarda o registro completo (nome, vínculo a um grupo).
/// </summary>
public class ParticipanteEntity
{
    public Guid Id { get; set; }

    public Guid GrupoId { get; set; }

    public string Nome { get; set; } = string.Empty;

    public GrupoEntity? Grupo { get; set; }
}
