namespace Rateio.Infrastructure.Notificacoes;

/// <summary>
/// Caminho do <c>RateioHub</c> (T38, <c>Rateio.Api.Hubs.RateioHub</c>) — constante compartilhada
/// entre <see cref="DependencyInjection.AddAutenticacaoJwt"/> (que precisa saber quais requisições
/// são handshake de Hub pra ler o JWT da query string <c>access_token</c> em vez do header
/// <c>Authorization</c>) e <c>Rateio.Api/Program.cs</c> (que mapeia o Hub nesta rota). Vive em
/// Infrastructure, não em Rateio.Api.Hubs, porque é Infrastructure quem configura o
/// <c>AddJwtBearer</c> e não pode referenciar Rateio.Api (a dependência vai na direção contrária) —
/// e não em Rateio.Application porque é um detalhe de transporte HTTP, não do domínio/casos de uso.
/// </summary>
public static class RotaDoHubDeNotificacoes
{
    public const string Caminho = "/hubs/rateio";
}
