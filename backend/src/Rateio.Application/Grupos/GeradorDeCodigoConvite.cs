using System.Security.Cryptography;

namespace Rateio.Application.Grupos;

/// <summary>
/// Gera o código curto de convite (T21.1: "8 caracteres alfanuméricos, não-adivinhável o
/// suficiente pra um MVP — não precisa ser criptograficamente forte como um token de sessão").
/// Ainda assim usa <see cref="RandomNumberGenerator"/> (CSPRNG) em vez de <see cref="Random"/>: o
/// custo é desprezível e evita qualquer dependência do estado/seed do PRNG padrão, sem exigir
/// justificar por que "um MVP pode usar o PRNG fraco aqui". O alfabeto exclui caracteres
/// visualmente ambíguos (0/O, 1/I/L) porque o código é pensado pra ser digitado à mão a partir de
/// um link compartilhado, não só colado — 32 símbolos ^ 8 posições ainda dá um espaço de mais de 1
/// trilhão de códigos, sobra de margem pra "não-adivinhável" num MVP.
/// </summary>
internal static class GeradorDeCodigoConvite
{
    private const string Alfabeto = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private const int Tamanho = 8;

    public static string Gerar() => RandomNumberGenerator.GetString(Alfabeto, Tamanho);
}
