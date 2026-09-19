using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence.Configurations;

public class DespesaEntityConfiguration : IEntityTypeConfiguration<DespesaEntity>
{
    public void Configure(EntityTypeBuilder<DespesaEntity> builder)
    {
        builder.ToTable("despesas");

        builder.HasKey(despesa => despesa.Id);

        builder.Property(despesa => despesa.Descricao)
            .HasMaxLength(200)
            .IsRequired();

        builder.Property(despesa => despesa.ValorTotalCentavos)
            .IsRequired();

        builder.Property(despesa => despesa.PagadorId)
            .IsRequired();

        builder.Property(despesa => despesa.CriadoEm)
            .IsRequired();
    }
}
