namespace Rateio.Domain;

/// <summary>
/// Valor monetário usado dentro do motor de simplificação: um inteiro de centavos (<see
/// cref="long"/>) wrapped, nunca cru. Ver "Dinheiro: representação e arredondamento" em
/// algorithm-spec.md — dentro do algoritmo não existe <c>float</c>/<c>double</c>/<c>decimal</c>;
/// essas representações só existem na borda de parsing/formatação, fora do escopo deste motor.
/// </summary>
public readonly record struct Dinheiro : IComparable<Dinheiro>
{
    public long Centavos { get; }

    private Dinheiro(long centavos)
    {
        Centavos = centavos;
    }

    public static Dinheiro Zero { get; } = new(0);

    public static Dinheiro EmCentavos(long centavos) => new(centavos);

    public bool EhPositivo => Centavos > 0;

    public bool EhNegativo => Centavos < 0;

    public bool EhZero => Centavos == 0;

    public static Dinheiro operator +(Dinheiro esquerda, Dinheiro direita) =>
        new(esquerda.Centavos + direita.Centavos);

    public static Dinheiro operator -(Dinheiro esquerda, Dinheiro direita) =>
        new(esquerda.Centavos - direita.Centavos);

    public static Dinheiro operator -(Dinheiro valor) => new(-valor.Centavos);

    public int CompareTo(Dinheiro outro) => Centavos.CompareTo(outro.Centavos);

    public override string ToString() => Centavos.ToString();
}
