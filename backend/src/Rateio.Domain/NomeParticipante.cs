namespace Rateio.Domain;

/// <summary>
/// Nome de um <see cref="Participante"/>, wrapped em vez de <see cref="string"/> cru (Object
/// Calisthenics: "wrap all primitives and strings"). Para um participante convidado (RF06 —
/// spec.md) o nome é o único dado que existe, então validar que ele nunca é vazio aqui protege a
/// única informação que identifica essa pessoa dentro do grupo.
/// </summary>
public readonly record struct NomeParticipante
{
    public string Valor { get; }

    private NomeParticipante(string valor)
    {
        Valor = valor;
    }

    public static NomeParticipante Criar(string valor)
    {
        if (string.IsNullOrWhiteSpace(valor))
        {
            throw new ArgumentException("Nome do participante não pode ser vazio.", nameof(valor));
        }

        return new NomeParticipante(valor.Trim());
    }

    public override string ToString() => Valor;
}
