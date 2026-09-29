using System.Security.Cryptography;

namespace Tally.Application.Groups;

/// <summary>
/// Generates the short invite code (T21.1: "8 alphanumeric characters, hard-to-guess enough for an
/// MVP — doesn't need to be as cryptographically strong as a session token"). Still uses
/// <see cref="RandomNumberGenerator"/> (CSPRNG) instead of <see cref="Random"/>: the cost is
/// negligible and it avoids any dependency on the default PRNG's state/seed, without having to
/// justify why "an MVP can use the weak PRNG here". The alphabet excludes visually ambiguous
/// characters (0/O, 1/I/L) because the code is meant to be typed by hand from a shared link, not
/// just pasted — 32 symbols ^ 8 positions still gives a space of over 1 trillion codes, plenty of
/// margin for "hard-to-guess" in an MVP.
/// </summary>
internal static class InviteCodeGenerator
{
    private const string Alphabet = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private const int Length = 8;

    public static string Generate() => RandomNumberGenerator.GetString(Alphabet, Length);
}
