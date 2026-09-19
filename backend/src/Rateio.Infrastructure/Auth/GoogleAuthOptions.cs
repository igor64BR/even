namespace Rateio.Infrastructure.Auth;

/// <summary>
/// Config de validação do ID token do Google. <see cref="ClientId"/> é a audience esperada — o
/// client id OAuth do projeto no Google Cloud Console. Em <c>appsettings.Development.json</c> hoje
/// é um placeholder; precisa ser trocado pelo client id real assim que o projeto Google Cloud
/// existir (configuração externa, fora do escopo de T11 — ver T11 para detalhes de como testar
/// manualmente depois disso).
/// </summary>
public sealed class GoogleAuthOptions
{
    public const string SecaoConfiguracao = "GoogleAuth";

    public required string ClientId { get; init; }
}
