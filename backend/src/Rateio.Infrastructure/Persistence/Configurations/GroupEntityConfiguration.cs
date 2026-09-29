using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence.Configurations;

public class GroupEntityConfiguration : IEntityTypeConfiguration<GroupEntity>
{
    public void Configure(EntityTypeBuilder<GroupEntity> builder)
    {
        builder.ToTable("groups");

        builder.HasKey(group => group.Id);

        builder.Property(group => group.Name)
            .HasMaxLength(120)
            .IsRequired();

        builder.Property(group => group.Category)
            .IsRequired();

        builder.Property(group => group.Synced)
            .IsRequired();

        builder.Property(group => group.OwnerUserId)
            .IsRequired();

        builder.Property(group => group.CreatedAt)
            .IsRequired();

        builder.HasMany(group => group.Participants)
            .WithOne(participant => participant.Group)
            .HasForeignKey(participant => participant.GroupId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.HasMany(group => group.Expenses)
            .WithOne(expense => expense.Group)
            .HasForeignKey(expense => expense.GroupId)
            .OnDelete(DeleteBehavior.Cascade);

        // Restrict (not Cascade): deleting a user should not silently delete the groups they
        // synced — that's an explicit decision for another operation, out of scope for T18.
        builder.HasOne(group => group.Owner)
            .WithMany()
            .HasForeignKey(group => group.OwnerUserId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}
