using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Even.Infrastructure.Persistence.Entities;

namespace Even.Infrastructure.Persistence.Configurations;

public class ExpenseSplitEntityConfiguration : IEntityTypeConfiguration<ExpenseSplitEntity>
{
    public void Configure(EntityTypeBuilder<ExpenseSplitEntity> builder)
    {
        builder.ToTable("expense_splits");

        builder.HasKey(split => split.Id);

        builder.Property(split => split.ParticipantId)
            .IsRequired();

        builder.Property(split => split.Type)
            .IsRequired();
    }
}
