namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Mapeamento de persistência de um participante de grupo. Esqueleto mínimo (T2.2) — não confundir
/// com <see cref="Rateio.Domain.ParticipanteId"/>, que é só o identificador wrapped usado pelo
/// motor de simplificação; esta entidade guarda o registro completo (nome, vínculo a um grupo).
/// <see cref="EhConvidado"/> foi adicionado em T18: espelha
/// <see cref="Rateio.Domain.Participante.EhConvidado"/> pra que a distinção convidado/autenticado
/// (RF06/RF07) sobreviva ao round-trip domínio→EF→domínio.
/// </summary>
public class ParticipanteEntity
{
    public Guid Id { get; set; }

    public Guid GrupoId { get; set; }

    public string Nome { get; set; } = string.Empty;

    public bool EhConvidado { get; set; }

    public GrupoEntity? Grupo { get; set; }
}
