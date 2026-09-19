namespace Rateio.Application.Grupos;

/// <summary>
/// O código de convite (<c>POST /groups/join/{codigo}</c>, T21.2) não existe, já foi substituído
/// por um código mais novo do mesmo grupo, ou expirou (ver <see cref="CodigoConvite"/>). As três
/// causas viram o mesmo 404 no controller — de fora, "código inválido" e "código que nunca
/// existiu" não devem ser distinguíveis (não vazar se um código specific já existiu).
/// </summary>
public sealed class CodigoConviteInvalidoException(string codigo) : Exception($"Código de convite '{codigo}' inválido ou expirado.")
{
    public string Codigo { get; } = codigo;
}
