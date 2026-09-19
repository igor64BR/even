using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;
using Microsoft.Extensions.Configuration;

namespace Rateio.Infrastructure.Persistence;

/// <summary>
/// Fábrica usada só pelas ferramentas de design-time do EF Core (<c>dotnet ef migrations add</c>,
/// <c>dotnet ef database update</c>). Lê a connection string de
/// <c>Rateio.Api/appsettings.Development.json</c> (nunca hardcoded) sem depender de DI de runtime
/// — registrar <see cref="AppDbContext"/> no container da aplicação é escopo de T3.
/// </summary>
public class AppDbContextFactory : IDesignTimeDbContextFactory<AppDbContext>
{
    private const string NomeArquivoConfiguracao = "appsettings.Development.json";

    public AppDbContext CreateDbContext(string[] args)
    {
        var connectionString = LerConnectionString();

        var optionsBuilder = new DbContextOptionsBuilder<AppDbContext>();
        optionsBuilder.UseNpgsql(connectionString);

        return new AppDbContext(optionsBuilder.Options);
    }

    private static string LerConnectionString()
    {
        var diretorioApi = ResolverDiretorioProjetoApi();

        var configuration = new ConfigurationBuilder()
            .SetBasePath(diretorioApi)
            .AddJsonFile(NomeArquivoConfiguracao, optional: false)
            .Build();

        return configuration.GetConnectionString("Default")
            ?? throw new InvalidOperationException(
                $"ConnectionStrings:Default não encontrada em {Path.Combine(diretorioApi, NomeArquivoConfiguracao)}.");
    }

    private static string ResolverDiretorioProjetoApi()
    {
        string[] candidatos =
        [
            Path.Combine(Directory.GetCurrentDirectory(), "src", "Rateio.Api"),
            Path.Combine(Directory.GetCurrentDirectory(), "..", "Rateio.Api"),
            Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "Rateio.Api"),
        ];

        var encontrado = candidatos.FirstOrDefault(
            candidato => File.Exists(Path.Combine(candidato, NomeArquivoConfiguracao)));

        return encontrado
            ?? throw new InvalidOperationException(
                $"Não encontrei {NomeArquivoConfiguracao} de Rateio.Api a partir de nenhum caminho candidato.");
    }
}
