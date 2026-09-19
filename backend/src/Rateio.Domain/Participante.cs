namespace Rateio.Domain;

/// <summary>
/// Participante de um <see cref="Grupo"/>. A distinção entre convidado (só nome, sem conta) e
/// autenticado (vinculado a uma conta Google) é central pro produto — ver constitution.md
/// princípio 2 e spec.md RF06/RF07 — então ela é expressa como dois métodos de fábrica nomeados
/// em vez de um construtor que recebe um <see cref="bool"/> solto (Object Calisthenics: um
/// parâmetro booleano no call site não diz o que está sendo criado; <c>Participante.Convidado(...)</c>
/// diz).
/// </summary>
public sealed class Participante : IEquatable<Participante>
{
    public ParticipanteId Id { get; }

    public NomeParticipante Nome { get; }

    /// <summary>
    /// <c>true</c> para um participante incluído só pelo nome (RF06), sem conta nem app próprio;
    /// <c>false</c> para um participante vinculado a uma conta Google (RF07).
    /// </summary>
    public bool EhConvidado { get; }

    private Participante(ParticipanteId id, NomeParticipante nome, bool ehConvidado)
    {
        Id = id;
        Nome = nome;
        EhConvidado = ehConvidado;
    }

    public static Participante Convidado(ParticipanteId id, NomeParticipante nome) =>
        new(id, nome, ehConvidado: true);

    public static Participante Autenticado(ParticipanteId id, NomeParticipante nome) =>
        new(id, nome, ehConvidado: false);

    public bool Equals(Participante? outro)
    {
        if (outro is null)
        {
            return false;
        }

        return Id.Equals(outro.Id);
    }

    public override bool Equals(object? obj) => Equals(obj as Participante);

    public override int GetHashCode() => Id.GetHashCode();
}
