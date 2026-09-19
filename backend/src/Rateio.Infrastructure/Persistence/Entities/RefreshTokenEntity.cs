namespace Rateio.Infrastructure.Persistence.Entities;

/// <summary>
/// Sessão de refresh emitida para um usuário (T11). Guarda só o hash do refresh token — nunca o
/// valor em texto puro — pra que um vazamento do banco não exponha tokens de sessão utilizáveis.
/// <see cref="RevogadoEm"/> existe desde já (mesmo sem nada que o preencha ainda) pra T14 (logout)
/// não precisar de outra migration só pra isso.
/// </summary>
public class RefreshTokenEntity
{
    public Guid Id { get; set; }

    public Guid UsuarioId { get; set; }

    public string TokenHash { get; set; } = string.Empty;

    public DateTimeOffset CriadoEm { get; set; }

    public DateTimeOffset ExpiraEm { get; set; }

    public DateTimeOffset? RevogadoEm { get; set; }

    public UsuarioEntity? Usuario { get; set; }
}
