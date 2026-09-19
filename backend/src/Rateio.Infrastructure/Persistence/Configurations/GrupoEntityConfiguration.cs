using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence.Configurations;

public class GrupoEntityConfiguration : IEntityTypeConfiguration<GrupoEntity>
{
    public void Configure(EntityTypeBuilder<GrupoEntity> builder)
    {
        builder.ToTable("grupos");

        builder.HasKey(grupo => grupo.Id);

        builder.Property(grupo => grupo.Nome)
            .HasMaxLength(120)
            .IsRequired();

        builder.Property(grupo => grupo.CriadoEm)
            .IsRequired();

        builder.HasMany(grupo => grupo.Participantes)
            .WithOne(participante => participante.Grupo)
            .HasForeignKey(participante => participante.GrupoId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.HasMany(grupo => grupo.Despesas)
            .WithOne(despesa => despesa.Grupo)
            .HasForeignKey(despesa => despesa.GrupoId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}
