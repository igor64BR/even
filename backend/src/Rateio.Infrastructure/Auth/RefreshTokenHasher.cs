using System.Security.Cryptography;
using System.Text;

namespace Rateio.Infrastructure.Auth;

/// <summary>
/// SHA-256 do refresh token, em hex. Existe pra que nunca precisemos guardar (nem comparar) o
/// refresh token em texto puro no banco — se o banco vazar, os hashes sozinhos não permitem
/// reconstruir sessões válidas.
/// </summary>
internal static class RefreshTokenHasher
{
    public static string Hash(string refreshToken)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(refreshToken));

        return Convert.ToHexString(bytes);
    }
}
