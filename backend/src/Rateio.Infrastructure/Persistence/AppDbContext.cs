using Microsoft.EntityFrameworkCore;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence;

/// <summary>
/// Rateio's EF Core context. Lives in Rateio.Infrastructure — Rateio.Domain never references
/// EF Core (constitution/T2: Domain doesn't know EF Core exists).
/// </summary>
public class AppDbContext(DbContextOptions<AppDbContext> options) : DbContext(options)
{
    public DbSet<GroupEntity> Groups => Set<GroupEntity>();

    public DbSet<ParticipantEntity> Participants => Set<ParticipantEntity>();

    public DbSet<ExpenseEntity> Expenses => Set<ExpenseEntity>();

    public DbSet<ExpenseSplitEntity> ExpenseSplits => Set<ExpenseSplitEntity>();

    public DbSet<SettlementEntity> Settlements => Set<SettlementEntity>();

    public DbSet<UserEntity> Users => Set<UserEntity>();

    public DbSet<RefreshTokenEntity> RefreshTokens => Set<RefreshTokenEntity>();

    public DbSet<InviteCodeEntity> InviteCodes => Set<InviteCodeEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(AppDbContext).Assembly);
    }
}
