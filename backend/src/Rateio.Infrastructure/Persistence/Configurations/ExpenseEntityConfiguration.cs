using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence.Configurations;

public class ExpenseEntityConfiguration : IEntityTypeConfiguration<ExpenseEntity>
{
    public void Configure(EntityTypeBuilder<ExpenseEntity> builder)
    {
        builder.ToTable("expenses");

        builder.HasKey(expense => expense.Id);

        builder.Property(expense => expense.Description)
            .HasMaxLength(200)
            .IsRequired();

        builder.Property(expense => expense.TotalAmountCents)
            .IsRequired();

        builder.Property(expense => expense.PayerId)
            .IsRequired();

        builder.Property(expense => expense.Date)
            .IsRequired();

        builder.Property(expense => expense.CreatedAt)
            .IsRequired();

        builder.HasMany(expense => expense.Splits)
            .WithOne(split => split.Expense)
            .HasForeignKey(split => split.ExpenseId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}
