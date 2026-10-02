using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Even.Infrastructure.Persistence.Entities;

namespace Even.Infrastructure.Persistence.Configurations;

public class InviteCodeEntityConfiguration : IEntityTypeConfiguration<InviteCodeEntity>
{
    public void Configure(EntityTypeBuilder<InviteCodeEntity> builder)
    {
        builder.ToTable("invite_codes");

        builder.HasKey(code => code.Code);

        builder.Property(code => code.Code)
            .HasMaxLength(12)
            .IsRequired();

        builder.Property(code => code.GroupId)
            .IsRequired();

        builder.Property(code => code.CreatedAt)
            .IsRequired();

        builder.Property(code => code.ExpiresAt)
            .IsRequired();

        builder.HasIndex(code => code.GroupId);

        // Cascade: if the group is deleted, its invite code no longer makes sense —
        // unlike a group's owner (Restrict in GroupEntityConfiguration), which is a link that
        // shouldn't disappear just because a secondary operation deleted something.
        builder.HasOne(code => code.Group)
            .WithMany()
            .HasForeignKey(code => code.GroupId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}
