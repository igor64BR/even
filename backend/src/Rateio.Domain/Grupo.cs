namespace Rateio.Domain;

/// <summary>
/// Agregado raiz de um grupo de divisão de despesas (RF05). Invariante protegida: um grupo
/// precisa de pelo menos 1 participante para existir — o app permite criar só com "Você", mas
/// nunca com zero (ver T15-modelar-grupo-participante.md). A invariante é imposta na criação
/// (<see cref="Criar"/>) e revalidada em toda mutação que reduz a lista de participantes
/// (<see cref="RemoverParticipante"/>); não há setters públicos soltos, então não existe caminho
/// pra violar isso depois de criado.
/// </summary>
public sealed class Grupo
{
    private readonly List<Participante> _participantes;

    public Guid Id { get; }

    public NomeGrupo Nome { get; }

    public CategoriaGrupo Categoria { get; }

    /// <summary>
    /// <c>true</c> quando o grupo foi sincronizado com o backend (RF09); grupos locais nascem
    /// <c>false</c> e só mudam de estado via <see cref="MarcarComoSincronizado"/>.
    /// </summary>
    public bool Sincronizado { get; private set; }

    public IReadOnlyList<Participante> Participantes => _participantes;

    private Grupo(
        Guid id,
        NomeGrupo nome,
        CategoriaGrupo categoria,
        List<Participante> participantes,
        bool sincronizado)
    {
        Id = id;
        Nome = nome;
        Categoria = categoria;
        _participantes = participantes;
        Sincronizado = sincronizado;
    }

    /// <summary>
    /// Único jeito de criar um <see cref="Grupo"/> — garante a invariante de pelo menos 1
    /// participante em vez de deixar isso a cargo de quem chama um construtor público.
    /// </summary>
    public static Grupo Criar(
        NomeGrupo nome,
        CategoriaGrupo categoria,
        IReadOnlyCollection<Participante> participantes)
    {
        ArgumentNullException.ThrowIfNull(participantes);
        ExigirPeloMenosUmParticipante(participantes.Count);

        return new Grupo(Guid.NewGuid(), nome, categoria, new List<Participante>(participantes), sincronizado: false);
    }

    public void AdicionarParticipante(Participante participante)
    {
        ArgumentNullException.ThrowIfNull(participante);

        if (_participantes.Any(existente => existente.Id.Equals(participante.Id)))
        {
            throw new InvalidOperationException($"Participante {participante.Id} já está no grupo.");
        }

        _participantes.Add(participante);
    }

    /// <summary>
    /// Remove um participante pelo id (RF10). A checagem de saldo zerado exigida por RF10 depende
    /// de despesas/saldos, que este agregado não conhece (isso é responsabilidade de uma camada de
    /// aplicação que também enxerga <see cref="Despesa"/> — escopo de T18/T23); aqui só se protege
    /// a invariante estrutural do próprio <see cref="Grupo"/>: nunca ficar com zero participantes.
    /// </summary>
    public void RemoverParticipante(ParticipanteId participanteId)
    {
        ExigirPeloMenosUmParticipante(_participantes.Count - 1);

        var participantesRestantes = _participantes.Where(p => !p.Id.Equals(participanteId)).ToList();
        if (participantesRestantes.Count == _participantes.Count)
        {
            throw new InvalidOperationException($"Participante {participanteId} não está no grupo.");
        }

        _participantes.Clear();
        _participantes.AddRange(participantesRestantes);
    }

    public void MarcarComoSincronizado() => Sincronizado = true;

    private static void ExigirPeloMenosUmParticipante(int quantidade)
    {
        if (quantidade < 1)
        {
            throw new InvalidOperationException("Grupo precisa de pelo menos 1 participante.");
        }
    }
}
