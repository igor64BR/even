using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Rateio.Infrastructure.Persistence.Entities;

namespace Rateio.Infrastructure.Persistence.Configurations;

public class ParticipanteEntityConfiguration : IEntityTypeConfiguration<ParticipanteEntity>
{
    public void Configure(EntityTypeBuilder<ParticipanteEntity> builder)
    {
        builder.ToTable("participantes");

        builder.HasKey(participante => participante.Id);

        builder.Property(participante => participante.Nome)
            .HasMaxLength(120)
            .IsRequired();

        builder.Property(participante => participante.EhConvidado)
            .IsRequired();
    }
}
