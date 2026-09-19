namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Mapeamento de persistência de um grupo. Esqueleto mínimo (T2.2) — ainda não existe um tipo de
/// domínio "Grupo" rico em Rateio.Domain; virar isso um agregado de domínio é escopo de T15.
/// </summary>
public class GrupoEntity
{
    public Guid Id { get; set; }

    public string Nome { get; set; } = string.Empty;

    public DateTimeOffset CriadoEm { get; set; }

    public List<ParticipanteEntity> Participantes { get; set; } = [];

    public List<DespesaEntity> Despesas { get; set; } = [];
}
