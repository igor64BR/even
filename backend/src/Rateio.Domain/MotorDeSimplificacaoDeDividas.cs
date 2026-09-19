namespace Rateio.Domain;

/// <summary>
/// Implementação de referência de <see cref="IMotorDeSimplificacaoDeDividas"/>, tradução linha a
/// linha do pseudocódigo em algorithm-spec.md. Convenção de nomes adotada neste projeto:
/// membros públicos do contrato do motor (<c>ComputeBalances</c>/<c>ComputeSettlement</c>) usam
/// os nomes em inglês da spec para bater exatamente com ela; tipos e métodos privados de apoio
/// usam português, consistente com o domínio (despesa, quitação, saldo) e com o restante das
/// specs do projeto.
/// </summary>
public sealed class MotorDeSimplificacaoDeDividas : IMotorDeSimplificacaoDeDividas
{
    public IReadOnlyDictionary<ParticipanteId, Dinheiro> ComputeBalances(
        IReadOnlyList<Despesa> despesas,
        IReadOnlyList<Quitacao> quitacoes)
    {
        var saldos = new Dictionary<ParticipanteId, Dinheiro>();

        foreach (var despesa in despesas)
            AplicarDespesa(saldos, despesa);

        foreach (var quitacao in quitacoes)
            AplicarQuitacao(saldos, quitacao);

        return saldos;
    }

    public IReadOnlyList<Transacao> ComputeSettlement(IReadOnlyDictionary<ParticipanteId, Dinheiro> saldos)
    {
        var devedores = ParticipantesComSaldoNegativo(saldos);
        var credores = ParticipantesComSaldoPositivo(saldos);
        var transacoes = new List<Transacao>();

        while (devedores.Count > 0 && credores.Count > 0)
            transacoes.Add(QuitarMaiorDevedorComMaiorCredor(devedores, credores));

        return transacoes;
    }

    // --- computeBalances -----------------------------------------------------------------

    private static void AplicarDespesa(Dictionary<ParticipanteId, Dinheiro> saldos, Despesa despesa)
    {
        Creditar(saldos, despesa.PagadorId, despesa.ValorTotal);

        var partes = Dividir(despesa.ValorTotal, despesa.Participacoes);
        foreach (var (participanteId, parte) in partes)
            Debitar(saldos, participanteId, parte);
    }

    private static void AplicarQuitacao(Dictionary<ParticipanteId, Dinheiro> saldos, Quitacao quitacao)
    {
        Creditar(saldos, quitacao.PagadorId, quitacao.Valor);
        Debitar(saldos, quitacao.RecebedorId, quitacao.Valor);
    }

    private static void Creditar(Dictionary<ParticipanteId, Dinheiro> saldos, ParticipanteId id, Dinheiro valor) =>
        saldos[id] = SaldoAtual(saldos, id) + valor;

    private static void Debitar(Dictionary<ParticipanteId, Dinheiro> saldos, ParticipanteId id, Dinheiro valor) =>
        saldos[id] = SaldoAtual(saldos, id) - valor;

    private static Dinheiro SaldoAtual(Dictionary<ParticipanteId, Dinheiro> saldos, ParticipanteId id) =>
        saldos.TryGetValue(id, out var saldo) ? saldo : Dinheiro.Zero;

    // --- divisão da despesa (fechamento por maiores restos) ------------------------------

    private static IReadOnlyDictionary<ParticipanteId, Dinheiro> Dividir(
        Dinheiro total, IReadOnlyList<ParticipacaoDespesa> participacoes)
    {
        if (participacoes.Count == 0)
            throw new ArgumentException("Despesa sem participantes na divisão.", nameof(participacoes));

        return participacoes[0] switch
        {
            ParticipacaoDespesa.PorIgual => DividirIgualmente(total, participacoes),
            ParticipacaoDespesa.PorPeso => DividirPorPeso(total, participacoes),
            ParticipacaoDespesa.PorValorFixo => DividirPorValorFixo(participacoes),
            var participacao => throw new NotSupportedException(
                $"Tipo de participação não suportado: {participacao.GetType().Name}")
        };
    }

    private static IReadOnlyDictionary<ParticipanteId, Dinheiro> DividirIgualmente(
        Dinheiro total, IReadOnlyList<ParticipacaoDespesa> participacoes)
    {
        var ordenados = participacoes
            .Select(participacao => participacao.ParticipanteId)
            .OrderBy(id => id)
            .ToList();

        var quantidade = ordenados.Count;
        var baseCentavos = total.Centavos / quantidade;
        var resto = total.Centavos % quantidade;

        var partes = new Dictionary<ParticipanteId, Dinheiro>();
        for (var i = 0; i < quantidade; i++)
        {
            var centavoExtra = i < resto ? 1 : 0;
            partes[ordenados[i]] = Dinheiro.EmCentavos(baseCentavos + centavoExtra);
        }

        return partes;
    }

    private static IReadOnlyDictionary<ParticipanteId, Dinheiro> DividirPorPeso(
        Dinheiro total, IReadOnlyList<ParticipacaoDespesa> participacoes)
    {
        var pesos = participacoes.Cast<ParticipacaoDespesa.PorPeso>().ToList();
        var somaPesos = pesos.Sum(p => p.Peso);

        var parteBasePorId = pesos.ToDictionary(
            p => p.ParticipanteId,
            p => total.Centavos * p.Peso / somaPesos);
        var restoFracionarioPorId = pesos.ToDictionary(
            p => p.ParticipanteId,
            p => total.Centavos * p.Peso % somaPesos);

        var centavosNaoDistribuidos = total.Centavos - parteBasePorId.Values.Sum();
        var ordemDeDesempate = pesos
            .Select(p => p.ParticipanteId)
            .OrderByDescending(id => restoFracionarioPorId[id])
            .ThenBy(id => id)
            .ToList();

        var partes = new Dictionary<ParticipanteId, Dinheiro>();
        for (var i = 0; i < ordemDeDesempate.Count; i++)
        {
            var id = ordemDeDesempate[i];
            var centavoExtra = i < centavosNaoDistribuidos ? 1 : 0;
            partes[id] = Dinheiro.EmCentavos(parteBasePorId[id] + centavoExtra);
        }

        return partes;
    }

    private static IReadOnlyDictionary<ParticipanteId, Dinheiro> DividirPorValorFixo(
        IReadOnlyList<ParticipacaoDespesa> participacoes) =>
        participacoes
            .Cast<ParticipacaoDespesa.PorValorFixo>()
            .ToDictionary(p => p.ParticipanteId, p => p.Valor);

    // --- computeSettlement (algoritmo guloso) ---------------------------------------------

    private static Transacao QuitarMaiorDevedorComMaiorCredor(
        List<ParticipanteComSaldo> devedores, List<ParticipanteComSaldo> credores)
    {
        var devedor = RemoverMaior(devedores);
        var credor = RemoverMaior(credores);

        var valorQuitado = MenorValor(devedor.Restante, credor.Restante);

        DevolverSeAindaTemSaldo(devedores, devedor.ComRestante(devedor.Restante - valorQuitado));
        DevolverSeAindaTemSaldo(credores, credor.ComRestante(credor.Restante - valorQuitado));

        return new Transacao(devedor.Id, credor.Id, valorQuitado);
    }

    /// <summary>
    /// Acha e remove o maior saldo restante da lista: valor desc, empate por ParticipanteId asc.
    /// Reselecionar o maior a cada chamada (em vez de ordenar uma vez e percorrer com
    /// ponteiros) é intencional — ver algorithm-spec.md, seção computeSettlement.
    /// </summary>
    private static ParticipanteComSaldo RemoverMaior(List<ParticipanteComSaldo> participantes)
    {
        var maior = participantes
            .OrderByDescending(participante => participante.Restante)
            .ThenBy(participante => participante.Id)
            .First();

        participantes.Remove(maior);
        return maior;
    }

    private static void DevolverSeAindaTemSaldo(
        List<ParticipanteComSaldo> participantes, ParticipanteComSaldo participante)
    {
        if (!participante.Restante.EhPositivo) return;
        participantes.Add(participante);
    }

    private static Dinheiro MenorValor(Dinheiro a, Dinheiro b) => a.CompareTo(b) <= 0 ? a : b;

    private static List<ParticipanteComSaldo> ParticipantesComSaldoNegativo(
        IReadOnlyDictionary<ParticipanteId, Dinheiro> saldos) =>
        saldos
            .Where(par => par.Value.EhNegativo)
            .Select(par => new ParticipanteComSaldo(par.Key, -par.Value))
            .ToList();

    private static List<ParticipanteComSaldo> ParticipantesComSaldoPositivo(
        IReadOnlyDictionary<ParticipanteId, Dinheiro> saldos) =>
        saldos
            .Where(par => par.Value.EhPositivo)
            .Select(par => new ParticipanteComSaldo(par.Key, par.Value))
            .ToList();

    /// <summary>Participante com o valor (sempre positivo) que ainda falta quitar na fila de
    /// devedores ou de credores do algoritmo guloso.</summary>
    private sealed record ParticipanteComSaldo(ParticipanteId Id, Dinheiro Restante)
    {
        public ParticipanteComSaldo ComRestante(Dinheiro novoRestante) => this with { Restante = novoRestante };
    }
}
