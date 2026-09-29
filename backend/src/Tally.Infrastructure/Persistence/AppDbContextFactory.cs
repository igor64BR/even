using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;
using Microsoft.Extensions.Configuration;

namespace Tally.Infrastructure.Persistence;

/// <summary>
/// Factory used only by EF Core's design-time tools (<c>dotnet ef migrations add</c>,
/// <c>dotnet ef database update</c>). Reads the connection string from
/// <c>Tally.Api/appsettings.Development.json</c> (never hardcoded) without relying on runtime
/// DI — registering <see cref="AppDbContext"/> in the application container is T3's scope.
/// </summary>
public class AppDbContextFactory : IDesignTimeDbContextFactory<AppDbContext>
{
    private const string ConfigurationFileName = "appsettings.Development.json";

    public AppDbContext CreateDbContext(string[] args)
    {
        var connectionString = ReadConnectionString();

        var optionsBuilder = new DbContextOptionsBuilder<AppDbContext>();
        optionsBuilder.UseNpgsql(connectionString);

        return new AppDbContext(optionsBuilder.Options);
    }

    private static string ReadConnectionString()
    {
        var apiDirectory = ResolveApiProjectDirectory();

        var configuration = new ConfigurationBuilder()
            .SetBasePath(apiDirectory)
            .AddJsonFile(ConfigurationFileName, optional: false)
            .Build();

        return configuration.GetConnectionString("Default")
            ?? throw new InvalidOperationException(
                $"ConnectionStrings:Default not found in {Path.Combine(apiDirectory, ConfigurationFileName)}.");
    }

    private static string ResolveApiProjectDirectory()
    {
        string[] candidates =
        [
            Path.Combine(Directory.GetCurrentDirectory(), "src", "Tally.Api"),
            Path.Combine(Directory.GetCurrentDirectory(), "..", "Tally.Api"),
            Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "Tally.Api"),
        ];

        var found = candidates.FirstOrDefault(
            candidate => File.Exists(Path.Combine(candidate, ConfigurationFileName)));

        return found
            ?? throw new InvalidOperationException(
                $"Could not find {ConfigurationFileName} for Tally.Api from any candidate path.");
    }
}
