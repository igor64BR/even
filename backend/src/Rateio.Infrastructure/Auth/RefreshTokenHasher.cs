using System.Security.Cryptography;
using System.Text;

namespace Rateio.Infrastructure.Auth;

/// <summary>
/// SHA-256 of the refresh token, in hex. This exists so we never need to store (or compare) the
/// refresh token in plain text in the database — if the database leaks, the hashes alone don't
/// allow reconstructing valid sessions.
/// </summary>
internal static class RefreshTokenHasher
{
    public static string Hash(string refreshToken)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(refreshToken));

        return Convert.ToHexString(bytes);
    }
}
