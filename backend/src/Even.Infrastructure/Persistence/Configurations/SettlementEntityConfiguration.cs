using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Even.Infrastructure.Persistence.Entities;

namespace Even.Infrastructure.Persistence.Configurations;

public class SettlementEntityConfiguration : IEntityTypeConfiguration<SettlementEntity>
{
    public void Configure(EntityTypeBuilder<SettlementEntity> builder)
    {
        builder.ToTable("settlements");

        builder.HasKey(settlement => settlement.Id);

        builder.Property(settlement => settlement.PayerId)
            .IsRequired();

        builder.Property(settlement => settlement.PayeeId)
            .IsRequired();

        builder.Property(settlement => settlement.AmountCents)
            .IsRequired();

        builder.Property(settlement => settlement.CreatedAt)
            .IsRequired();

        // No navigation collection on GroupEntity (same pattern as GroupEntity.Owner): nothing
        // today needs to load "the group with its settlements" at once, only to filter
        // settlements by GroupId — SettlementRepository queries SettlementEntity directly.
        builder.HasOne(settlement => settlement.Group)
            .WithMany()
            .HasForeignKey(settlement => settlement.GroupId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}
