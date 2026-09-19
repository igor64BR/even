using Microsoft.EntityFrameworkCore;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence;

/// <summary>
/// Contexto EF Core do Rateio. Vive em Rateio.Infrastructure — Rateio.Domain nunca referencia
/// EF Core (constitution/T2: Domain não sabe que EF Core existe).
/// </summary>
public class AppDbContext(DbContextOptions<AppDbContext> options) : DbContext(options)
{
    public DbSet<GrupoEntity> Grupos => Set<GrupoEntity>();

    public DbSet<ParticipanteEntity> Participantes => Set<ParticipanteEntity>();

    public DbSet<DespesaEntity> Despesas => Set<DespesaEntity>();

    public DbSet<ParticipacaoDespesaEntity> ParticipacoesDeDespesa => Set<ParticipacaoDespesaEntity>();

    public DbSet<UsuarioEntity> Usuarios => Set<UsuarioEntity>();

    public DbSet<RefreshTokenEntity> RefreshTokens => Set<RefreshTokenEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(AppDbContext).Assembly);
    }
}
