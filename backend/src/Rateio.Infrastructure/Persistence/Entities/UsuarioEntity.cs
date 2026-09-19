namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Mapeamento de persistência de um usuário autenticado via Google (T11). Nunca guarda senha —
/// autenticação é inteiramente delegada ao Google (constitution.md, princípio 2); o único segredo
/// relacionado a identidade que este schema guarda é o hash do refresh token, em
/// <see cref="RefreshTokenEntity"/>.
/// </summary>
public class UsuarioEntity
{
    public Guid Id { get; set; }

    public string GoogleSubjectId { get; set; } = string.Empty;

    public string Nome { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;

    public DateTimeOffset CriadoEm { get; set; }

    public List<RefreshTokenEntity> RefreshTokens { get; set; } = [];
}
