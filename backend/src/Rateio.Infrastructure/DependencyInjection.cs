using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Rateio.Infrastructure.Persistence;

namespace Rateio.Infrastructure;

/// <summary>
/// Ponto de registro de DI da camada de Infrastructure. Chamado a partir do composition root
/// (<c>Rateio.Api/Program.cs</c>) — mantém o Program.cs enxuto em vez de espalhar
/// <c>services.AddX</c> de cada camada diretamente nele (T3).
/// </summary>
public static class DependencyInjection
{
    private const string NomeConnectionString = "Default";

    /// <summary>
    /// Registra o <see cref="AppDbContext"/> (Npgsql) e demais serviços de Infrastructure.
    /// A connection string vem de <paramref name="configuration"/> (appsettings/variável de
    /// ambiente) — nunca hardcoded.
    /// </summary>
    public static IServiceCollection AddInfrastructure(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString(NomeConnectionString)
            ?? throw new InvalidOperationException(
                $"ConnectionStrings:{NomeConnectionString} não encontrada na configuração.");

        services.AddDbContext<AppDbContext>(options => options.UseNpgsql(connectionString));

        // T4.1: health check de infraestrutura para RNF08 (`/health` reflete a saúde do DB).
        // Quando o Hub SignalR (E8) existir, seu health check entra aqui como um segundo
        // `.AddCheck(...)`/`.AddSignalRHub(...)` na mesma chain, tornando o endpoint agregado.
        services.AddHealthChecks()
            .AddNpgSql(connectionString, name: "postgres");

        return services;
    }
}
