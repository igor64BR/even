namespace Rateio.Domain;

/// <summary>
/// Nome de um <see cref="Grupo"/>, wrapped em vez de <see cref="string"/> cru (Object
/// Calisthenics: "wrap all primitives and strings") para que a regra "não pode ser vazio" viva
/// num único lugar em vez de ser reimplementada em cada borda que recebe um nome de grupo.
/// </summary>
public readonly record struct NomeGrupo
{
    public string Valor { get; }

    private NomeGrupo(string valor)
    {
        Valor = valor;
    }

    public static NomeGrupo Criar(string valor)
    {
        if (string.IsNullOrWhiteSpace(valor))
        {
            throw new ArgumentException("Nome do grupo não pode ser vazio.", nameof(valor));
        }

        return new NomeGrupo(valor.Trim());
    }

    public override string ToString() => Valor;
}
